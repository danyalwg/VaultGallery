package com.danyal.vaultgallery

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.ByteArrayInputStream
import java.io.File
import java.io.RandomAccessFile
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.zip.InflaterInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal object MessagingBackupKeyStore {
    private const val STORE = "messaging-backup-keys"
    private const val KEYSTORE = "AndroidKeyStore"

    fun save(context: Context, source: MessagingSource, value: String) {
        val clean = value.filterNot(Char::isWhitespace).lowercase()
        require(clean.matches(Regex("[0-9a-f]{64}"))) { "The backup key must contain exactly 64 hexadecimal characters" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(source))
        val encrypted = cipher.doFinal(clean.toByteArray(Charsets.US_ASCII))
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
            .putString("${source.storageId}.iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("${source.storageId}.value", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun load(context: Context, source: MessagingSource): String? = runCatching {
        val preferences = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val iv = Base64.decode(preferences.getString("${source.storageId}.iv", null), Base64.NO_WRAP)
        val encrypted = Base64.decode(preferences.getString("${source.storageId}.value", null), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(source), GCMParameterSpec(128, iv))
        cipher.doFinal(encrypted).toString(Charsets.US_ASCII)
    }.getOrNull()

    fun forget(context: Context, source: MessagingSource) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
            .remove("${source.storageId}.iv")
            .remove("${source.storageId}.value")
            .apply()
    }

    private fun key(source: MessagingSource): java.security.Key {
        val alias = "vaultgallery.messaging.${source.storageId}"
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        store.getKey(alias, null)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }
}

internal object MessagingIndexCoordinator {
    const val WORK_TAG = "messaging-chat-index"

    fun enqueue(context: Context, source: MessagingSource) {
        val work = OneTimeWorkRequestBuilder<MessagingIndexWorker>()
            .setInputData(workDataOf(MessagingIndexWorker.INPUT_SOURCE to source.storageId))
            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "$WORK_TAG-${source.storageId}",
            ExistingWorkPolicy.REPLACE,
            work,
        )
    }
}

internal class MessagingIndexWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    companion object {
        const val INPUT_SOURCE = "source"
        private const val CHANNEL = "messaging-index"
        private const val NOTIFICATION_ID = 4307
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val source = MessagingSource.entries.firstOrNull { it.storageId == inputData.getString(INPUT_SOURCE) }
            ?: return@withContext Result.failure(workDataOf("error" to "Unknown messaging source"))
        val rootKey = MessagingBackupKeyStore.load(applicationContext, source)
            ?: return@withContext fail(source, "Enter the ${source.title} backup key first")
        val backup = findBackup(source) ?: return@withContext fail(source, "No Crypt15 backup was found for ${source.title}")
        setForeground(notification(source, "Decrypting the latest backup…", true))
        val workDirectory = File(applicationContext.cacheDir, "messaging-index-${source.storageId}").apply { mkdirs() }
        val compressed = File(workDirectory, "decrypted.bin")
        val database = File(workDirectory, "msgstore.db")
        try {
            decryptCrypt15(backup, rootKey, compressed)
            setForeground(notification(source, "Reading conversations…", true))
            inflateOrCopyDatabase(compressed, database)
            val output = MessagingAlbumRepository(applicationContext).indexFile(source)
            output.parentFile?.mkdirs()
            val temporary = File(output.parentFile, "${output.name}.new")
            buildIndex(database, backup.lastModified(), temporary)
            if (output.exists() && !output.delete()) error("Could not replace the previous index")
            if (!temporary.renameTo(output)) error("Could not publish the new index")
            applicationContext.getSharedPreferences("messaging-index-status", Context.MODE_PRIVATE).edit()
                .putString("${source.storageId}.status", "ready")
                .putLong("${source.storageId}.updated", System.currentTimeMillis())
                .remove("${source.storageId}.error")
                .apply()
            setForeground(notification(source, "Conversation albums are ready", false))
            Result.success()
        } catch (error: Throwable) {
            fail(source, error.message ?: "Could not index the backup")
        } finally {
            compressed.delete()
            database.delete()
            workDirectory.delete()
        }
    }

    private fun fail(source: MessagingSource, message: String): Result {
        applicationContext.getSharedPreferences("messaging-index-status", Context.MODE_PRIVATE).edit()
            .putString("${source.storageId}.status", "error")
            .putString("${source.storageId}.error", message)
            .apply()
        return Result.failure(workDataOf("error" to message))
    }

    private fun findBackup(source: MessagingSource): File? {
        val shared = Environment.getExternalStorageDirectory()
        val directory = when (source) {
            MessagingSource.WHATSAPP -> File(shared, "Android/media/com.whatsapp/WhatsApp/Databases")
            MessagingSource.WHATSAPP_BUSINESS -> File(shared, "Android/media/com.whatsapp.w4b/WhatsApp Business/Databases")
        }
        return directory.listFiles { file -> file.isFile && file.name.startsWith("msgstore") && file.name.endsWith(".crypt15") }
            ?.maxByOrNull(File::lastModified)
    }

    private fun decryptCrypt15(input: File, rootKeyHex: String, output: File) {
        val rootKey = rootKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val aesKey = deriveBackupKey(rootKey)
        RandomAccessFile(input, "r").use { encrypted ->
            val protobufSize = encrypted.readUnsignedByte()
            val possibleFlag = encrypted.readUnsignedByte()
            val hasFeatureFlag = possibleFlag == 1
            if (!hasFeatureFlag) encrypted.seek(1L)
            val protobuf = ByteArray(protobufSize)
            encrypted.readFully(protobuf)
            val headerSize = 1L + (if (hasFeatureFlag) 1L else 0L) + protobufSize
            val iv = parseCrypt15Iv(protobuf)
            val ciphertextLength = input.length() - headerSize - 32L
            require(ciphertextLength > 0L) { "The Crypt15 backup is truncated" }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, iv))
            encrypted.seek(headerSize)
            output.outputStream().buffered().use { sink ->
                val buffer = ByteArray(1024 * 1024)
                var remaining = ciphertextLength
                while (remaining > 0L) {
                    val read = encrypted.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    check(read > 0) { "Unexpected end of backup" }
                    cipher.update(buffer, 0, read)?.let(sink::write)
                    remaining -= read
                }
                val tag = ByteArray(16)
                encrypted.readFully(tag)
                cipher.doFinal(tag)?.let(sink::write)
            }
        }
    }

    private fun deriveBackupKey(rootKey: ByteArray): ByteArray {
        val first = Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(ByteArray(32), "HmacSHA256"))
            doFinal(rootKey)
        }
        return Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(first, "HmacSHA256"))
            doFinal("backup encryption".toByteArray(Charsets.US_ASCII) + byteArrayOf(1))
        }
    }

    private fun parseCrypt15Iv(protobuf: ByteArray): ByteArray {
        val outer = ProtoReader(protobuf)
        while (!outer.exhausted()) {
            val tag = outer.varint().toInt()
            val field = tag ushr 3
            val wire = tag and 7
            if (field == 3 && wire == 2) {
                val nested = ProtoReader(outer.bytes(outer.varint().toInt()))
                while (!nested.exhausted()) {
                    val nestedTag = nested.varint().toInt()
                    if ((nestedTag ushr 3) == 1 && (nestedTag and 7) == 2) {
                        return nested.bytes(nested.varint().toInt()).also { require(it.size == 16) { "Invalid Crypt15 IV" } }
                    }
                    nested.skip(nestedTag and 7)
                }
            } else outer.skip(wire)
        }
        error("Crypt15 IV was not found in the backup header")
    }

    private fun inflateOrCopyDatabase(input: File, output: File) {
        val header = input.inputStream().use { stream -> ByteArray(16).also { stream.read(it) } }
        if (header.contentEquals("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))) {
            input.inputStream().use { source -> output.outputStream().use(source::copyTo) }
        } else {
            input.inputStream().buffered().use { source ->
                InflaterInputStream(source).use { inflated -> output.outputStream().buffered().use(inflated::copyTo) }
            }
        }
        val result = output.inputStream().use { stream -> ByteArray(16).also { stream.read(it) } }
        require(result.contentEquals("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))) { "Backup decryption did not produce a WhatsApp database" }
    }

    private fun buildIndex(database: File, backupTimestamp: Long, output: File) {
        val db = SQLiteDatabase.openDatabase(database.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            val lidToPhone = HashMap<Long, String>()
            runCatching {
                db.rawQuery(
                    "SELECT jm.lid_row_id, pn.user FROM jid_map jm JOIN jid pn ON pn._id=jm.jid_row_id",
                    null,
                ).use { cursor -> while (cursor.moveToNext()) lidToPhone[cursor.getLong(0)] = cursor.getString(1).orEmpty() }
            }
            data class ChatBuilder(val id: String, val type: String, val name: String, val digits: String, val media: JSONArray = JSONArray())
            val chats = LinkedHashMap<Long, ChatBuilder>()
            db.rawQuery(
                """SELECT mm.file_path,m.timestamp,c._id,c.subject,c.jid_row_id,j.user,j.server,j.raw_string
                   FROM message_media mm
                   JOIN message m ON m._id=mm.message_row_id
                   JOIN chat c ON c._id=m.chat_row_id
                   JOIN jid j ON j._id=c.jid_row_id
                   WHERE mm.file_path IS NOT NULL AND length(mm.file_path)>0
                   ORDER BY m.timestamp ASC""".trimIndent(),
                null,
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val path = cursor.getString(0).orEmpty().replace('\\', '/').trimStart('/').lowercase()
                    val timestamp = cursor.getLong(1)
                    val chatRowId = cursor.getLong(2)
                    val subject = cursor.getString(3).orEmpty()
                    val jidRowId = cursor.getLong(4)
                    val user = cursor.getString(5).orEmpty()
                    val server = cursor.getString(6).orEmpty()
                    val rawJid = cursor.getString(7).orEmpty()
                    val type = when (server) {
                        "g.us" -> "group"
                        "newsletter" -> "channel"
                        else -> "direct"
                    }
                    val chat = chats.getOrPut(chatRowId) {
                        ChatBuilder(
                            id = rawJid,
                            type = type,
                            name = if (type == "direct") "" else subject,
                            digits = if (server == "lid") lidToPhone[jidRowId].orEmpty() else if (type == "direct") user else "",
                        )
                    }
                    chat.media.put(JSONObject().put("path", path).put("timestampMs", timestamp))
                }
            }
            val chatArray = JSONArray()
            chats.values.forEach { chat ->
                chatArray.put(
                    JSONObject()
                        .put("id", chat.id)
                        .put("type", chat.type)
                        .put("displayName", chat.name)
                        .put("contactDigits", chat.digits)
                        .put("media", chat.media),
                )
            }
            output.writeText(
                JSONObject()
                    .put("version", 1)
                    .put("backupTimestampMs", backupTimestamp)
                    .put("generatedAtMs", System.currentTimeMillis())
                    .put("chats", chatArray)
                    .toString(),
                Charsets.UTF_8,
            )
        } finally {
            db.close()
        }
    }

    private fun notification(source: MessagingSource, text: String, ongoing: Boolean): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Messaging album indexing", NotificationManager.IMPORTANCE_LOW))
        val intent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainGalleryActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(source.title)
            .setContentText(text)
            .setContentIntent(intent)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setProgress(if (ongoing) 100 else 0, 0, ongoing)
            .build()
        val id = NOTIFICATION_ID + source.ordinal
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else ForegroundInfo(id, notification)
    }
}

private class ProtoReader(private val data: ByteArray) {
    private var offset = 0
    fun exhausted(): Boolean = offset >= data.size
    fun varint(): Long {
        var result = 0L
        var shift = 0
        while (offset < data.size && shift < 64) {
            val value = data[offset++].toInt() and 0xff
            result = result or ((value and 0x7f).toLong() shl shift)
            if (value and 0x80 == 0) return result
            shift += 7
        }
        error("Invalid protobuf varint")
    }
    fun bytes(length: Int): ByteArray {
        require(length >= 0 && offset + length <= data.size) { "Invalid protobuf length" }
        return data.copyOfRange(offset, offset + length).also { offset += length }
    }
    fun skip(wire: Int) {
        when (wire) {
            0 -> varint()
            1 -> bytes(8)
            2 -> bytes(varint().toInt())
            5 -> bytes(4)
            else -> error("Unsupported protobuf wire type $wire")
        }
    }
}
