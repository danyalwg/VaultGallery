package com.danyal.vaultgallery.viewer

data class ViewerSession(
    val collectionId: String,
    val orderedMediaIds: List<String>,
    val currentIndex: Int,
    val soundEnabled: Boolean = false,
    val controlsVisible: Boolean = true,
    val detailsFraction: Float = 0f,
) {
    init { require(currentIndex in orderedMediaIds.indices || orderedMediaIds.isEmpty()) }
    val currentId: String? get() = orderedMediaIds.getOrNull(currentIndex)
    fun moveTo(index: Int) = copy(currentIndex = index.coerceIn(0, (orderedMediaIds.size - 1).coerceAtLeast(0)))
}
