package com.danyal.vaultgallery.transfer

import java.util.UUID

enum class TransferAction { COPY, MOVE }
enum class ConflictPolicy { MERGE, RENAME, REPLACE, SKIP, ASK }
enum class TransferState { QUEUED, RUNNING, PAUSED, CANCELLING, COMPLETED, FAILED }
data class TransferEntry(val source: String, val destination: String, val expectedBytes: Long, val expectedHash: String? = null)
data class TransferManifest(
    val id: String = UUID.randomUUID().toString(),
    val action: TransferAction,
    val conflictPolicy: ConflictPolicy,
    val entries: List<TransferEntry>,
    val state: TransferState = TransferState.QUEUED,
    val completedEntries: Set<Int> = emptySet(),
)
data class TransferProgress(val completedItems: Int, val totalItems: Int, val completedBytes: Long, val totalBytes: Long, val bytesPerSecond: Long, val remainingMillis: Long?)
