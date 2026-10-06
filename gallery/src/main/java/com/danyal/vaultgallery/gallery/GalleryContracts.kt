package com.danyal.vaultgallery.gallery

enum class GalleryDestination { PICTURES, ALBUMS, STORIES, MENU }
enum class GallerySort { DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, SIZE_DESC, SIZE_ASC }

data class CollectionContext(
    val id: String,
    val title: String,
    val orderedMediaIds: List<String>,
    val sort: GallerySort,
    val gridColumns: Int,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
)

data class GallerySurfaceState(
    val destination: GalleryDestination = GalleryDestination.PICTURES,
    val collection: CollectionContext? = null,
    val selectedIds: Set<String> = emptySet(),
)

interface GalleryFeatureSet {
    val supportsSearch: Boolean
    val supportsEditing: Boolean
    val supportsCreation: Boolean
    val supportsSharing: Boolean
    val supportsMap: Boolean
}
