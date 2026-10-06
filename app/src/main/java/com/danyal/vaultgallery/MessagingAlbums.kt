package com.danyal.vaultgallery

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.danyal.vaultgallery.data.AlbumKind
import com.danyal.vaultgallery.data.GalleryAlbum
import com.danyal.vaultgallery.data.GalleryMedia
import java.io.File
import java.text.Collator
import java.util.Locale
import org.json.JSONObject

internal enum class MessagingSource(
    val storageId: String,
    val title: String,
    val albumKind: AlbumKind,
    val rootKind: AlbumKind,
    val pathMarker: String,
) {
    WHATSAPP(
        "whatsapp",
        "WhatsApp",
        AlbumKind.WHATSAPP_CHAT,
        AlbumKind.WHATSAPP_ROOT,
        "/android/media/com.whatsapp/whatsapp/",
    ),
    WHATSAPP_BUSINESS(
        "whatsapp-business",
        "WhatsApp Business",
        AlbumKind.WHATSAPP_BUSINESS_CHAT,
        AlbumKind.WHATSAPP_BUSINESS_ROOT,
        "/android/media/com.whatsapp.w4b/whatsapp business/",
    ),
}

internal enum class MessagingOrganizationMode(val preferenceValue: String) {
    CONVERSATIONS("conversations"),
    ORIGINAL_FOLDERS("original-folders"),
}

internal fun Context.messagingOrganizationMode(source: MessagingSource): MessagingOrganizationMode {
    val saved = getSharedPreferences("gallery-settings", Context.MODE_PRIVATE)
        .getString("messaging_organization_${source.storageId}", MessagingOrganizationMode.CONVERSATIONS.preferenceValue)
    return MessagingOrganizationMode.entries.firstOrNull { it.preferenceValue == saved }
        ?: MessagingOrganizationMode.CONVERSATIONS
}

internal fun Context.setMessagingOrganizationMode(source: MessagingSource, mode: MessagingOrganizationMode) {
    getSharedPreferences("gallery-settings", Context.MODE_PRIVATE).edit()
        .putString("messaging_organization_${source.storageId}", mode.preferenceValue)
        .apply()
}

internal fun GalleryMedia.messagingSource(): MessagingSource? {
    val path = relativePath.replace('\\', '/').lowercase(Locale.ROOT)
    val bucket = bucketName.trim().lowercase(Locale.ROOT)
    return when {
        path.contains("android/media/com.whatsapp.w4b/whatsapp business/") || bucket.startsWith("whatsapp business") -> MessagingSource.WHATSAPP_BUSINESS
        path.contains("android/media/com.whatsapp/whatsapp/") || bucket.startsWith("whatsapp") -> MessagingSource.WHATSAPP
        else -> null
    }
}

internal data class MessagingCatalog(
    val source: MessagingSource,
    val albums: List<GalleryAlbum>,
    val indexed: Boolean,
    val backupTimestampMs: Long,
    val unmatchedCount: Int,
) {
    val mediaIds: Set<Long> = albums.flatMapTo(LinkedHashSet()) { it.mediaIds.orEmpty() }

    fun rootAlbum(): GalleryAlbum? {
        val cover = albums.asSequence().map(GalleryAlbum::cover).maxByOrNull(GalleryMedia::dateTakenMs) ?: return null
        return GalleryAlbum(
            bucketId = when (source) {
                MessagingSource.WHATSAPP -> Long.MIN_VALUE + 100
                MessagingSource.WHATSAPP_BUSINESS -> Long.MIN_VALUE + 101
            },
            name = source.title,
            cover = cover,
            count = mediaIds.size,
            bucketIds = emptySet(),
            mediaIds = mediaIds,
            kind = source.rootKind,
            subtitle = if (indexed) "${albums.size - if (unmatchedCount > 0) 1 else 0} conversations" else "Set up chat organization",
        )
    }
}

/**
 * Builds read-only virtual conversation albums from a locally generated WhatsApp index.
 * Original WhatsApp files are never renamed, moved, copied, or edited.
 */
internal class MessagingAlbumRepository(private val context: Context) {
    private val indexDirectory = File(context.filesDir, "messaging-index")

    fun indexFile(source: MessagingSource): File = File(indexDirectory, "${source.storageId}.json")

    fun load(media: List<GalleryMedia>): List<MessagingCatalog> {
        val contactNames = loadContactNames()
        return MessagingSource.entries.map { source -> loadSource(source, media, contactNames) }
    }

    private fun loadSource(
        source: MessagingSource,
        allMedia: List<GalleryMedia>,
        contactNames: Map<String, String>,
    ): MessagingCatalog {
        val sourceMedia = allMedia.filter { it.messagingSource() == source }
        val mediaByPath = sourceMedia.associateBy { normalizedMessagingPath(it.relativePath, it.name) }
        val mediaByUniqueName = sourceMedia.groupBy { it.name.lowercase(Locale.ROOT) }
            .mapNotNull { (name, items) -> items.singleOrNull()?.let { name to it } }
            .toMap()
        val index = indexFile(source)
        if (!index.isFile) {
            val unassigned = unassignedAlbum(source, sourceMedia)
            return MessagingCatalog(source, listOfNotNull(unassigned), false, 0L, sourceMedia.size)
        }

        val root = runCatching { JSONObject(index.readText(Charsets.UTF_8)) }.getOrNull()
            ?: return MessagingCatalog(source, listOfNotNull(unassignedAlbum(source, sourceMedia)), false, 0L, sourceMedia.size)
        val chats = root.optJSONArray("chats")
        val assigned = LinkedHashSet<Long>()
        val albums = buildList {
            if (chats != null) for (chatIndex in 0 until chats.length()) {
                val chat = chats.optJSONObject(chatIndex) ?: continue
                val entries = chat.optJSONArray("media") ?: continue
                val latestById = LinkedHashMap<Long, GalleryMedia>()
                for (entryIndex in 0 until entries.length()) {
                    val entry = entries.optJSONObject(entryIndex) ?: continue
                    val path = entry.optString("path").replace('\\', '/').trimStart('/').lowercase(Locale.ROOT)
                    val original = mediaByPath[path] ?: mediaByUniqueName[path.substringAfterLast('/')] ?: continue
                    val timestamp = entry.optLong("timestampMs", original.dateTakenMs).takeIf { it > 0L } ?: original.dateTakenMs
                    val item = original.copy(dateTakenMs = timestamp)
                    val previous = latestById[item.id]
                    if (previous == null || item.dateTakenMs > previous.dateTakenMs) latestById[item.id] = item
                }
                val items = latestById.values.sortedWith(compareByDescending<GalleryMedia> { it.dateTakenMs }.thenByDescending { it.id })
                if (items.isEmpty()) continue
                assigned += items.map(GalleryMedia::id)
                val chatType = chat.optString("type", "direct")
                val digits = chat.optString("contactDigits").filter(Char::isDigit)
                val suppliedName = chat.optString("displayName").trim()
                val name = when {
                    chatType != "direct" && suppliedName.isNotBlank() -> suppliedName
                    digits.isNotBlank() -> resolveContactName(digits, contactNames)
                    suppliedName.isNotBlank() -> suppliedName
                    else -> "Unknown chat"
                }
                val stableId = negativeStableId("${source.storageId}:${chat.optString("id")}")
                val savedCover = context.getSharedPreferences("gallery-settings", Context.MODE_PRIVATE)
                    .getString("album_cover_$stableId", null)
                val cover = savedCover?.let { uri -> items.firstOrNull { it.uri.toString() == uri } } ?: items.first()
                add(
                    GalleryAlbum(
                        bucketId = stableId,
                        name = name,
                        cover = cover,
                        count = items.size,
                        bucketIds = emptySet(),
                        mediaIds = items.mapTo(LinkedHashSet()) { it.id },
                        orderedMedia = items,
                        kind = source.albumKind,
                        subtitle = when (chatType) {
                            "group" -> "Group"
                            "channel" -> "Channel"
                            else -> "Contact"
                        },
                    ),
                )
            }
        }.toMutableList()
        val unassignedItems = sourceMedia.filterNot { it.id in assigned }
        unassignedAlbum(source, unassignedItems)?.let(albums::add)
        val collator = Collator.getInstance(Locale.getDefault())
        albums.sortWith(compareBy<GalleryAlbum> { it.name.startsWith("Unassigned") }.thenComparator { a, b -> collator.compare(a.name, b.name) })
        return MessagingCatalog(
            source = source,
            albums = albums,
            indexed = true,
            backupTimestampMs = root.optLong("backupTimestampMs"),
            unmatchedCount = unassignedItems.size,
        )
    }

    private fun unassignedAlbum(source: MessagingSource, items: List<GalleryMedia>): GalleryAlbum? {
        val sorted = items.distinctBy(GalleryMedia::id).sortedWith(compareByDescending<GalleryMedia> { it.dateTakenMs }.thenByDescending { it.id })
        val cover = sorted.firstOrNull() ?: return null
        return GalleryAlbum(
            bucketId = negativeStableId("${source.storageId}:unassigned"),
            name = "Unassigned / waiting for backup",
            cover = cover,
            count = sorted.size,
            bucketIds = emptySet(),
            mediaIds = sorted.mapTo(LinkedHashSet()) { it.id },
            orderedMedia = sorted,
            kind = source.albumKind,
            subtitle = "Not present in the latest backup",
        )
    }

    private fun loadContactNames(): Map<String, String> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return emptyMap()
        val names = LinkedHashMap<String, String>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val normalizedColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
            val numberColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn).orEmpty().trim()
                val raw = if (normalizedColumn >= 0) cursor.getString(normalizedColumn) else null
                val digits = (raw ?: cursor.getString(numberColumn)).orEmpty().filter(Char::isDigit)
                if (name.isNotBlank() && digits.isNotBlank()) {
                    names.putIfAbsent(digits, name)
                    if (digits.length >= 10) names.putIfAbsent(digits.takeLast(10), name)
                }
            }
        }
        return names
    }

    private fun resolveContactName(digits: String, names: Map<String, String>): String {
        return names[digits]
            ?: names[digits.takeLast(10)]
            ?: "Contact ending ${digits.takeLast(4).padStart(4, '•')}"
    }

    private fun normalizedMessagingPath(relativePath: String, name: String): String {
        val full = "${relativePath.trimEnd('/', '\\')}/$name".replace('\\', '/').lowercase(Locale.ROOT)
        val marker = "/media/"
        val index = full.lastIndexOf(marker)
        return if (index >= 0) full.substring(index + 1) else full.trimStart('/')
    }

    private fun negativeStableId(value: String): Long {
        var hash = -0x340d631b7bdddcdbL
        value.forEach { char -> hash = (hash xor char.code.toLong()) * 0x100000001b3L }
        return if (hash == Long.MIN_VALUE) Long.MIN_VALUE + 500 else -kotlin.math.abs(hash)
    }
}

internal fun List<GalleryAlbum>.withNavigationAlbumsFirst(): List<GalleryAlbum> = sortedWith(
    compareBy<GalleryAlbum> {
        when {
            it.name.equals("Camera", true) -> 0
            it.kind == AlbumKind.WHATSAPP_ROOT -> 1
            it.kind == AlbumKind.WHATSAPP_BUSINESS_ROOT -> 2
            else -> 3
        }
    },
)
