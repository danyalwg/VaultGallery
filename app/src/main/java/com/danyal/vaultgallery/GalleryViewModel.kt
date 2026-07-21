package com.danyal.vaultgallery

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.data.MediaStoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PublicGalleryState(
    val loading: Boolean = true,
    val hasAccess: Boolean = false,
    val media: List<GalleryMedia> = emptyList(),
    val trash: List<GalleryMedia> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val error: String? = null,
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaStoreRepository(application)
    private val _state = MutableStateFlow(PublicGalleryState())
    val state: StateFlow<PublicGalleryState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val hasAccess = repository.hasAnyAccess()
            _state.value = _state.value.copy(loading = true, hasAccess = hasAccess, error = null)
            runCatching {
                if (hasAccess) repository.loadMedia() to repository.loadMedia(includeTrashed = true).filter { it.isTrashed }
                else emptyList<GalleryMedia>() to emptyList()
            }
                .onSuccess { (media, trash) ->
                    _state.value = _state.value.copy(
                        loading = false,
                        hasAccess = hasAccess,
                        media = media,
                        trash = trash,
                        selectedIds = _state.value.selectedIds.intersect(media.mapTo(HashSet()) { it.id }),
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(loading = false, hasAccess = hasAccess, error = error.message ?: "Unable to read media")
                }
        }
    }

    fun toggleSelection(id: Long) {
        val selected = _state.value.selectedIds.toMutableSet()
        if (!selected.add(id)) selected.remove(id)
        _state.value = _state.value.copy(selectedIds = selected)
    }

    fun setSelection(ids: Set<Long>) {
        val available = _state.value.media.mapTo(HashSet()) { it.id }
        _state.value = _state.value.copy(selectedIds = ids.intersect(available))
    }

    fun selectAll(items: List<GalleryMedia>) {
        _state.value = _state.value.copy(selectedIds = items.mapTo(LinkedHashSet()) { it.id })
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedIds = emptySet())
    }

    fun selectedUris(): ArrayList<Uri> = ArrayList(
        _state.value.media.filter { it.id in _state.value.selectedIds }.map { it.uri },
    )
}
