package com.danyal.vaultgallery.editor

import android.content.Context
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Parameter-only, crash-safe project persistence; source pixels are never copied into the project. */
class EditProjectStore(context: Context) {
    private val root = File(context.noBackupFilesDir, "edit-projects").apply { mkdirs() }
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "operation"
        prettyPrint = false
    }

    suspend fun savePhoto(project: PhotoEditProject) = writeAtomic(photoFile(project.source), json.encodeToString(PhotoEditProject.serializer(), project))
    suspend fun loadPhoto(source: String): PhotoEditProject? = read(photoFile(source)) { json.decodeFromString(PhotoEditProject.serializer(), it) }
    suspend fun deletePhoto(source: String) = withContext(Dispatchers.IO) { photoFile(source).delete() }

    suspend fun saveVideo(project: VideoEditProject) = writeAtomic(videoFile(project.tracks.firstOrNull()?.clips?.firstOrNull()?.uri ?: project.id), json.encodeToString(VideoEditProject.serializer(), project))
    suspend fun loadVideo(source: String): VideoEditProject? = read(videoFile(source)) { json.decodeFromString(VideoEditProject.serializer(), it) }
    suspend fun deleteVideo(source: String) = withContext(Dispatchers.IO) { videoFile(source).delete() }

    suspend fun photoProjects(): List<PhotoEditProject> = readDirectory("photo-") { json.decodeFromString(PhotoEditProject.serializer(), it) }
    suspend fun videoProjects(): List<VideoEditProject> = readDirectory("video-") { json.decodeFromString(VideoEditProject.serializer(), it) }

    private suspend fun writeAtomic(destination: File, contents: String) = withContext(Dispatchers.IO) {
        val temporary = File(destination.parentFile, ".${destination.name}.tmp")
        temporary.writeText(contents)
        if (destination.exists()) destination.delete()
        require(temporary.renameTo(destination)) { "Could not save edit project" }
    }

    private suspend fun <T> read(file: File, decode: (String) -> T): T? = withContext(Dispatchers.IO) {
        if (!file.isFile) return@withContext null
        runCatching { decode(file.readText()) }.getOrNull()
    }

    private suspend fun <T> readDirectory(prefix: String, decode: (String) -> T): List<T> = withContext(Dispatchers.IO) {
        root.listFiles().orEmpty().filter { it.isFile && it.name.startsWith(prefix) }
            .sortedByDescending(File::lastModified)
            .mapNotNull { runCatching { decode(it.readText()) }.getOrNull() }
    }

    private fun photoFile(source: String) = File(root, "photo-${stableKey(source)}.json")
    private fun videoFile(source: String) = File(root, "video-${stableKey(source)}.json")
    private fun stableKey(source: String): String = MessageDigest.getInstance("SHA-256")
        .digest(source.toByteArray()).joinToString("") { "%02x".format(it) }
}
