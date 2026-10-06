package com.danyal.vaultgallery.securitycontract

import java.io.InputStream

enum class StorageProtection { PUBLIC, LOCKED_ONLY, ENCRYPTED }

data class StorageCapabilities(
    val searchable: Boolean = true,
    val externallyShareable: Boolean = true,
    val editable: Boolean = true,
    val supportsTrash: Boolean = true,
    val protection: StorageProtection,
)

/** Storage boundary used by the shared gallery surface; public and secure stores implement it. */
interface GalleryStorageRepository<M : Any> {
    val capabilities: StorageCapabilities
    suspend fun list(collectionId: String? = null): List<M>
    suspend fun open(item: M): InputStream
    suspend fun copy(items: List<M>, destination: String): List<M>
    suspend fun move(items: List<M>, destination: String): List<M>
    suspend fun trash(items: List<M>)
    suspend fun restore(items: List<M>)
}
