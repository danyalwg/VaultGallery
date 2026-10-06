package com.danyal.vaultgallery

import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.data.GalleryAlbum
import com.danyal.vaultgallery.security.SecureItem
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal enum class MediaSort(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    NAME_AZ("Name A–Z"),
    NAME_ZA("Name Z–A"),
    LARGEST("Largest first"),
    SMALLEST("Smallest first"),
    LONGEST("Longest videos first"),
    SHORTEST("Shortest videos first"),
    MEDIA_TYPE("Media type"),
}

internal fun mediaSortFrom(value: String?): MediaSort = runCatching { MediaSort.valueOf(value.orEmpty()) }.getOrDefault(MediaSort.NEWEST)

/** Camera is a navigation anchor, not a sort mode: keep it first under every album sort. */
internal fun List<GalleryAlbum>.withCameraFirst(): List<GalleryAlbum> = sortedWith(
    compareBy<GalleryAlbum> { !it.name.equals("Camera", ignoreCase = true) }
)

internal fun List<Pair<String, List<SecureItem>>>.withSecureCameraFirst(): List<Pair<String, List<SecureItem>>> = sortedWith(
    compareBy<Pair<String, List<SecureItem>>> { !it.first.equals("Camera", ignoreCase = true) }
)

internal fun List<GalleryMedia>.sortedByPreference(sort: MediaSort): List<GalleryMedia> = when (sort) {
    MediaSort.NEWEST -> sortedWith(compareByDescending<GalleryMedia> { it.dateTakenMs }.thenByDescending { it.id })
    MediaSort.OLDEST -> sortedWith(compareBy<GalleryMedia> { it.dateTakenMs }.thenBy { it.id })
    MediaSort.NAME_AZ -> sortedWith(compareBy<GalleryMedia> { it.name.lowercase(Locale.getDefault()) }.thenByDescending { it.dateTakenMs })
    MediaSort.NAME_ZA -> sortedWith(compareByDescending<GalleryMedia> { it.name.lowercase(Locale.getDefault()) }.thenByDescending { it.dateTakenMs })
    MediaSort.LARGEST -> sortedWith(compareByDescending<GalleryMedia> { it.sizeBytes }.thenByDescending { it.dateTakenMs })
    MediaSort.SMALLEST -> sortedWith(compareBy<GalleryMedia> { it.sizeBytes }.thenByDescending { it.dateTakenMs })
    MediaSort.LONGEST -> sortedWith(compareByDescending<GalleryMedia> { it.durationMs }.thenByDescending { it.dateTakenMs })
    MediaSort.SHORTEST -> sortedWith(compareBy<GalleryMedia> { it.durationMs }.thenByDescending { it.dateTakenMs })
    MediaSort.MEDIA_TYPE -> sortedWith(compareBy<GalleryMedia> { it.kind.ordinal }.thenByDescending { it.dateTakenMs })
}

internal fun List<SecureItem>.sortedSecureByPreference(sort: MediaSort): List<SecureItem> = when (sort) {
    MediaSort.NEWEST -> sortedWith(compareByDescending<SecureItem> { it.dateTakenMs }.thenBy { it.id })
    MediaSort.OLDEST -> sortedWith(compareBy<SecureItem> { it.dateTakenMs }.thenBy { it.id })
    MediaSort.NAME_AZ -> sortedWith(compareBy<SecureItem> { it.name.lowercase(Locale.getDefault()) }.thenByDescending { it.dateTakenMs })
    MediaSort.NAME_ZA -> sortedWith(compareByDescending<SecureItem> { it.name.lowercase(Locale.getDefault()) }.thenByDescending { it.dateTakenMs })
    MediaSort.LARGEST -> sortedWith(compareByDescending<SecureItem> { it.sizeBytes }.thenByDescending { it.dateTakenMs })
    MediaSort.SMALLEST -> sortedWith(compareBy<SecureItem> { it.sizeBytes }.thenByDescending { it.dateTakenMs })
    MediaSort.LONGEST -> sortedWith(compareByDescending<SecureItem> { it.durationMs }.thenByDescending { it.dateTakenMs })
    MediaSort.SHORTEST -> sortedWith(compareBy<SecureItem> { it.durationMs }.thenByDescending { it.dateTakenMs })
    MediaSort.MEDIA_TYPE -> sortedWith(compareBy<SecureItem> { it.isVideo }.thenByDescending { it.dateTakenMs })
}

@Composable
internal fun MediaSortDialog(selected: MediaSort, onSelect: (MediaSort) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort media") },
        text = {
            Column {
                MediaSort.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(horizontal = 4.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(option.label, modifier = Modifier.weight(1f))
                        if (option == selected) Icon(Icons.Outlined.Check, "Selected")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
