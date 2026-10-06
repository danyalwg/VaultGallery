package com.danyal.vaultgallery.editor

import android.content.Context
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class PhotoPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val adjustment: PhotoOperation.Adjustment,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = createdAtMs,
)

/** User-owned, editable tone presets. Parameters only: no photo pixels or source paths. */
class PhotoPresetStore(context: Context) {
    private val destination = File(context.noBackupFilesDir, "photo-presets.json")
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    suspend fun list(): List<PhotoPreset> = withContext(Dispatchers.IO) {
        if (!destination.isFile) return@withContext emptyList()
        runCatching { json.decodeFromString(ListSerializer(PhotoPreset.serializer()), destination.readText()) }
            .getOrDefault(emptyList())
            .sortedBy { it.name.lowercase() }
    }

    suspend fun save(preset: PhotoPreset) = mutate { current ->
        (current.filterNot { it.id == preset.id } + preset.copy(
            name = preset.name.trim().take(50),
            updatedAtMs = System.currentTimeMillis(),
        )).sortedBy { it.name.lowercase() }
    }

    suspend fun delete(id: String) = mutate { it.filterNot { preset -> preset.id == id } }

    private suspend fun mutate(transform: (List<PhotoPreset>) -> List<PhotoPreset>) = withContext(Dispatchers.IO) {
        val current = if (destination.isFile) runCatching {
            json.decodeFromString(ListSerializer(PhotoPreset.serializer()), destination.readText())
        }.getOrDefault(emptyList()) else emptyList()
        val next = transform(current)
        val temporary = File(destination.parentFile, ".${destination.name}.tmp")
        temporary.writeText(json.encodeToString(ListSerializer(PhotoPreset.serializer()), next))
        if (destination.exists()) destination.delete()
        require(temporary.renameTo(destination)) { "Could not save photo presets" }
    }
}
