@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.danyal.vaultgallery

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.danyal.vaultgallery.core.GalleryLogic
import com.danyal.vaultgallery.data.GalleryAlbum
import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.data.MediaKind
import com.danyal.vaultgallery.data.MediaStoreRepository
import com.danyal.vaultgallery.ui.VaultBackground
import com.danyal.vaultgallery.ui.VaultBlue
import com.danyal.vaultgallery.ui.VaultPrimary
import com.danyal.vaultgallery.ui.VaultRaised
import com.danyal.vaultgallery.ui.VaultSecondary
import com.danyal.vaultgallery.ui.VaultSecure
import com.danyal.vaultgallery.ui.VaultSurface
import com.danyal.vaultgallery.ui.VaultTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainGalleryActivity : ComponentActivity() {
    private val galleryViewModel by viewModels<GalleryViewModel>()
    private val mediaActionLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            galleryViewModel.clearSelection()
            galleryViewModel.refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent { VaultTheme { PublicGalleryApp(galleryViewModel, this) } }
    }

    fun deleteMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 30) {
            val pending = MediaStore.createTrashRequest(contentResolver, uris, true)
            mediaActionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
        } else {
            Toast.makeText(this, "Delete approval requires Android 11 or later", Toast.LENGTH_LONG).show()
        }
    }

    fun restoreMedia(uris: List<Uri>) {
        if (uris.isEmpty() || Build.VERSION.SDK_INT < 30) return
        val pending = MediaStore.createTrashRequest(contentResolver, uris, false)
        mediaActionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
    }

    fun deletePermanently(uris: List<Uri>) {
        if (uris.isEmpty() || Build.VERSION.SDK_INT < 30) return
        val pending = MediaStore.createDeleteRequest(contentResolver, uris)
        mediaActionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
    }

    fun setFavourite(uris: List<Uri>, favourite: Boolean) {
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 30) {
            val pending = MediaStore.createFavoriteRequest(contentResolver, uris, favourite)
            mediaActionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
        } else {
            Toast.makeText(this, "System favourites require Android 11 or later", Toast.LENGTH_LONG).show()
        }
    }
}

private enum class PublicTab { PICTURES, ALBUMS, STORIES, MENU }
private enum class AuxiliaryScreen { SEARCH, VIDEOS, RECENT, FAVOURITES, CLEAN_OUT, LOCATIONS, SHARED_ALBUMS }

@Composable
private fun PublicGalleryApp(viewModel: GalleryViewModel, activity: MainGalleryActivity) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(PublicTab.PICTURES) }
    var viewer by remember { mutableStateOf<GalleryMedia?>(null) }
    var settings by remember { mutableStateOf(false) }
    var allAlbums by remember { mutableStateOf(false) }
    var albumFilter by remember { mutableStateOf<Long?>(null) }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var kindFilter by remember { mutableStateOf<MediaKind?>(null) }
    var favouriteOnly by remember { mutableStateOf(false) }
    var trashOpen by remember { mutableStateOf(false) }
    var auxiliaryScreen by remember { mutableStateOf<AuxiliaryScreen?>(null) }
    var storyItems by remember { mutableStateOf<List<GalleryMedia>?>(null) }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refresh() }

    LaunchedEffect(Unit) { viewModel.refresh() }
    BackHandler(viewer != null || storyItems != null || settings || trashOpen || auxiliaryScreen != null || allAlbums || albumFilter != null || state.selectedIds.isNotEmpty()) {
        when {
            viewer != null -> viewer = null
            storyItems != null -> storyItems = null
            settings -> settings = false
            trashOpen -> trashOpen = false
            auxiliaryScreen != null -> auxiliaryScreen = null
            allAlbums -> allAlbums = false
            albumFilter != null -> albumFilter = null
            state.selectedIds.isNotEmpty() -> viewModel.clearSelection()
        }
    }

    if (viewer != null) {
        PublicViewer(
            media = viewer!!,
            onBack = { viewer = null },
            onShare = { shareUris(activity, listOf(viewer!!.uri), viewer!!.mimeType) },
            onSecure = { launchSecureImport(activity, arrayListOf(viewer!!.uri)) },
            onFavourite = { activity.setFavourite(listOf(viewer!!.uri), !viewer!!.isFavourite); viewer = null },
            onDelete = { activity.deleteMedia(listOf(viewer!!.uri)); viewer = null },
        )
        return
    }
    if (storyItems != null) {
        StoryViewer(storyItems!!, onBack = { storyItems = null })
        return
    }
    if (settings) {
        GallerySettings(onBack = { settings = false })
        return
    }
    if (trashOpen) {
        PublicTrashScreen(
            state.trash,
            onBack = { trashOpen = false },
            onRestore = { activity.restoreMedia(listOf(it.uri)) },
            onDelete = { activity.deletePermanently(listOf(it.uri)) },
            onEmpty = { activity.deletePermanently(state.trash.map { it.uri }) },
        )
        return
    }

    if (auxiliaryScreen != null) {
        when (auxiliaryScreen!!) {
            AuxiliaryScreen.SEARCH -> GallerySearchScreen(state.media, onBack = { auxiliaryScreen = null }, onOpen = { viewer = it })
            AuxiliaryScreen.VIDEOS -> SamsungCollectionScreen("Videos", state.media.filter { it.kind == MediaKind.VIDEO }, onBack = { auxiliaryScreen = null }, onOpen = { viewer = it })
            AuxiliaryScreen.RECENT -> SamsungCollectionScreen("Recent", state.media, onBack = { auxiliaryScreen = null }, onOpen = { viewer = it })
            AuxiliaryScreen.FAVOURITES -> SamsungCollectionScreen("Favourites", state.media.filter { it.isFavourite }, onBack = { auxiliaryScreen = null }, onOpen = { viewer = it })
            AuxiliaryScreen.CLEAN_OUT -> CleanOutScreen(state.media, onBack = { auxiliaryScreen = null })
            AuxiliaryScreen.LOCATIONS -> LocationsScreen(state.media, onBack = { auxiliaryScreen = null }, onOpen = { viewer = it })
            AuxiliaryScreen.SHARED_ALBUMS -> SharedAlbumsScreen(onBack = { auxiliaryScreen = null })
        }
        return
    }

    val filtered = remember(state.media, query, albumFilter, kindFilter, favouriteOnly) {
        state.media.filter { media ->
            (albumFilter == null || media.bucketId == albumFilter) &&
                (kindFilter == null || media.kind == kindFilter) &&
                (!favouriteOnly || media.isFavourite) &&
                (query.isBlank() || media.name.contains(query, true) || media.bucketName.contains(query, true))
        }
    }
    val albums = remember(state.media) { MediaStoreRepository(activity).albums(state.media) }
    val selectedMedia = state.media.filter { it.id in state.selectedIds }
    val selectedUris = selectedMedia.map { it.uri }
    val allSelectedFavourite = selectedMedia.isNotEmpty() && selectedMedia.all { it.isFavourite }

    Scaffold(
        containerColor = VaultBackground,
        bottomBar = {
            Column(Modifier.background(VaultBackground).navigationBarsPadding()) {
                if (state.selectedIds.isNotEmpty()) {
                    SelectionBar(
                        onShare = { shareUris(activity, selectedUris, "*/*") },
                        onSecure = { launchSecureImport(activity, ArrayList(selectedUris)) },
                        favouriteLabel = if (allSelectedFavourite) "Unfavourite" else "Favourite",
                        onFavourite = { activity.setFavourite(selectedUris, !allSelectedFavourite) },
                        onDelete = { activity.deleteMedia(selectedUris) },
                    )
                } else {
                    PublicBottomNavigation(tab) { next ->
                        tab = next; allAlbums = false; albumFilter = null; kindFilter = null; favouriteOnly = false
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(VaultBackground)) {
            when {
                !state.hasAccess -> PermissionGate(
                    loading = state.loading,
                    onGrant = { permissionLauncher.launch(requiredMediaPermissions()) },
                )
                state.error != null -> ErrorState(state.error!!, viewModel::refresh)
                allAlbums -> AllAlbumsScreen(albums, onBack = { allAlbums = false }, onOpen = { albumFilter = it.bucketId; allAlbums = false })
                albumFilter != null -> MediaGridScreen(
                    title = albums.firstOrNull { it.bucketId == albumFilter }?.name ?: "Album",
                    media = filtered,
                    selected = state.selectedIds,
                    query = query,
                    searchOpen = searchOpen,
                    onBack = { albumFilter = null },
                    onSearch = { searchOpen = !searchOpen },
                    onQuery = { query = it },
                    onOpen = {
                        if (state.selectedIds.isNotEmpty()) viewModel.toggleSelection(it.id)
                        else if (it.kind == MediaKind.VIDEO && activity.getSharedPreferences("gallery-settings", 0).getBoolean("external_player", false)) {
                            activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(it.uri, it.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                        } else viewer = it
                    },
                    onSelect = viewModel::toggleSelection,
                    onSelectionSet = viewModel::setSelection,
                )
                tab == PublicTab.PICTURES -> PicturesScreen(
                    media = filtered,
                    loading = state.loading,
                    selected = state.selectedIds,
                    query = query,
                    searchOpen = searchOpen,
                    onQuery = { query = it },
                    onSearch = { auxiliaryScreen = AuxiliaryScreen.SEARCH },
                    onOpen = {
                        if (state.selectedIds.isNotEmpty()) viewModel.toggleSelection(it.id)
                        else if (it.kind == MediaKind.VIDEO && activity.getSharedPreferences("gallery-settings", 0).getBoolean("external_player", false)) {
                            activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(it.uri, it.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                        } else viewer = it
                    },
                    onSelect = viewModel::toggleSelection,
                    onSelectionSet = viewModel::setSelection,
                    onSelectAll = { viewModel.selectAll(filtered) },
                    onRefresh = viewModel::refresh,
                )
                tab == PublicTab.ALBUMS -> AlbumsScreen(
                    albums,
                    onViewAll = { allAlbums = true },
                    onOpen = { albumFilter = it.bucketId },
                    onSettings = { settings = true },
                )
                tab == PublicTab.STORIES -> StoriesScreen(state.media) { storyItems = it }
                else -> MenuScreen(
                    onVideos = { auxiliaryScreen = AuxiliaryScreen.VIDEOS },
                    onRecent = { auxiliaryScreen = AuxiliaryScreen.RECENT },
                    onFavourites = { auxiliaryScreen = AuxiliaryScreen.FAVOURITES },
                    onCleanOut = { auxiliaryScreen = AuxiliaryScreen.CLEAN_OUT },
                    onLocations = { auxiliaryScreen = AuxiliaryScreen.LOCATIONS },
                    onSharedAlbums = { auxiliaryScreen = AuxiliaryScreen.SHARED_ALBUMS },
                    onSettings = { settings = true },
                    onTrash = { trashOpen = true },
                    onSecure = { activity.startActivity(Intent(activity, SecureGalleryActivity::class.java)) },
                )
            }
        }
    }
}

private fun requiredMediaPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

@Composable
private fun PermissionGate(loading: Boolean, onGrant: () -> Unit) {
    Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
        if (loading) CircularProgressIndicator() else Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(Icons.Outlined.PermMedia, null, Modifier.size(58.dp), tint = VaultBlue)
            Text("Choose the photos and videos Gallery can show", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text("You can grant full or selected access. Gallery stays usable with only the items you choose.", color = VaultSecondary, textAlign = TextAlign.Center)
            Button(onClick = onGrant) { Text("Continue") }
        }
    }
}

@Composable
private fun ErrorState(message: String, retry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Gallery could not load", style = MaterialTheme.typography.headlineMedium)
            Text(message, color = VaultSecondary)
            Button(onClick = retry) { Text("Retry") }
        }
    }
}

@Composable
private fun GalleryToolbar(
    title: String? = null,
    back: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    beforeSearch: @Composable (() -> Unit)? = null,
    extra: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(112.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (back != null) IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        if (title != null) Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
        beforeSearch?.invoke()
        if (onSearch != null) IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Search") }
        extra?.invoke()
    }
}

@Composable
private fun PicturesScreen(
    media: List<GalleryMedia>, loading: Boolean, selected: Set<Long>, query: String, searchOpen: Boolean,
    onQuery: (String) -> Unit, onSearch: () -> Unit, onOpen: (GalleryMedia) -> Unit,
    onSelect: (Long) -> Unit, onSelectionSet: (Set<Long>) -> Unit, onSelectAll: () -> Unit, onRefresh: () -> Unit,
) {
    var moreMenu by remember { mutableStateOf(false) }
    var createMenu by remember { mutableStateOf(false) }
    var slideshow by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().height(92.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSelectAll) { Text("All") }
                Text("${selected.size} selected", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { selected.toList().forEach { onSelect(it) } }) { Text("Cancel") }
            }
        } else {
            GalleryToolbar(
                onSearch = onSearch,
                beforeSearch = { IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Collections, "Change layout") } },
                extra = {
                    Box {
                        IconButton(onClick = { moreMenu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                            DropdownMenuItem(text = { Text("Select") }, onClick = { moreMenu = false; media.firstOrNull()?.let { onSelect(it.id) } })
                            DropdownMenuItem(text = { Text("Create") }, onClick = { moreMenu = false; createMenu = true })
                            DropdownMenuItem(text = { Text("Start slideshow") }, onClick = { moreMenu = false; slideshow = true })
                            DropdownMenuItem(text = { Text("View duplicates") }, onClick = { moreMenu = false })
                        }
                    }
                },
            )
        }
        AnimatedVisibility(searchOpen) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                label = { Text("Search names and albums") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (media.isEmpty()) EmptyGallery()
        else TimelineGrid(media, selected, onOpen, onSelect, onSelectionSet)
    }
    if (createMenu) AlertDialog(
        onDismissRequest = { createMenu = false },
        title = { Text("Create") },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MenuAction(Icons.Outlined.AutoAwesome, "GIF", { createMenu = false })
                MenuAction(Icons.Outlined.GridView, "Collage", { createMenu = false })
                MenuAction(Icons.Outlined.Movie, "Movie", { createMenu = false })
            }
        },
        confirmButton = { TextButton(onClick = { createMenu = false }) { Text("Cancel") } },
    )
    if (slideshow && media.isNotEmpty()) StoryViewer(media, onBack = { slideshow = false })
}

@Composable
private fun EmptyGallery() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(54.dp), tint = VaultSecondary)
            Text("No pictures or videos", style = MaterialTheme.typography.titleLarge)
            Text("New media will appear here automatically.", color = VaultSecondary)
        }
    }
}

@Composable
private fun TimelineGrid(
    media: List<GalleryMedia>,
    selected: Set<Long>,
    onOpen: (GalleryMedia) -> Unit,
    onSelect: (Long) -> Unit,
    onSelectionSet: (Set<Long>) -> Unit,
) {
    val context = LocalContext.current
    val gridColumns = context.getSharedPreferences("gallery-settings", 0).getInt("grid_columns", 4).coerceIn(3, 5)
    val format = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val grouped = remember(media) { media.groupBy { format.format(Date(it.dateTakenMs)) } }
    val orderedIds = remember(media) { media.map { it.id } }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val latestSelected by rememberUpdatedState(selected)
    val latestSelectionSet by rememberUpdatedState(onSelectionSet)
    var dragAnchor by remember { mutableStateOf<Long?>(null) }
    var dragBase by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var dragSelect by remember { mutableStateOf(true) }

    fun mediaAt(x: Float, y: Float): Long? = gridState.layoutInfo.visibleItemsInfo
        .lastOrNull { info ->
            info.key is Long && x >= info.offset.x && x <= info.offset.x + info.size.width &&
                y >= info.offset.y && y <= info.offset.y + info.size.height
        }?.key as? Long

    fun updateSlideSelection(current: Long) {
        val anchor = dragAnchor ?: return
        latestSelectionSet(GalleryLogic.slideSelection(orderedIds, dragBase, anchor, current, dragSelect))
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(gridColumns),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalArrangement = Arrangement.spacedBy(1.5.dp),
        modifier = Modifier.fillMaxSize().pointerInput(media) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                mediaAt(longPress.position.x, longPress.position.y)?.let { id ->
                    dragAnchor = id
                    dragBase = latestSelected
                    dragSelect = id !in latestSelected
                    updateSlideSelection(id)
                }
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    change.consume()
                    val viewportHeight = gridState.layoutInfo.viewportSize.height.toFloat()
                    when {
                        change.position.y < 88f -> scope.launch { gridState.scrollBy(-30f) }
                        change.position.y > viewportHeight - 88f -> scope.launch { gridState.scrollBy(30f) }
                    }
                    mediaAt(change.position.x, change.position.y)?.let(::updateSlideSelection)
                }
                dragAnchor = null
            }
        },
    ) {
        grouped.forEach { (date, items) ->
            item(span = { GridItemSpan(maxLineSpan) }, key = "date-$date") {
                Text(date, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 2.dp, vertical = 15.dp))
            }
            items(items, key = { it.id }) { item -> MediaTile(item, item.id in selected, onOpen, onSelect) }
        }
    }
}

@Composable
private fun MediaTile(item: GalleryMedia, selected: Boolean, onOpen: (GalleryMedia) -> Unit, onSelect: (Long) -> Unit) {
    Box(
        Modifier.aspectRatio(1f).clip(RoundedCornerShape(1.dp)).background(VaultSurface)
            .semantics { onLongClick("Select") { onSelect(item.id); true } }
            .clickable(onClick = { onOpen(item) }),
    ) {
        AsyncImage(item.uri, item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (item.kind == MediaKind.VIDEO) {
            Row(
                Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(5.dp)).background(Color(0x99000000)).padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.PlayArrow, null, Modifier.size(14.dp), tint = Color.White)
                Text(GalleryLogic.durationLabel(item.durationMs), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (selected) {
            Box(Modifier.fillMaxSize().background(Color(0x55000000)))
            Box(Modifier.align(Alignment.TopStart).padding(7.dp).size(25.dp).clip(CircleShape).background(VaultPrimary), contentAlignment = Alignment.Center) {
                Text("✓", color = Color.Black)
            }
        }
    }
}

@Composable
private fun AlbumsScreen(albums: List<GalleryAlbum>, onViewAll: () -> Unit, onOpen: (GalleryAlbum) -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val showEssential = context.getSharedPreferences("gallery-settings", 0).getBoolean("essential", true)
    var menu by remember { mutableStateOf(false) }
    var create by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar(extra = {
            IconButton(onClick = { create = true }) { Icon(Icons.Outlined.Add, "Create") }
            IconButton(onClick = onViewAll) { Icon(Icons.Outlined.Search, "Search") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Select") }, onClick = { menu = false })
                    DropdownMenuItem(text = { Text("Select essential albums") }, onClick = { menu = false; onSettings() })
                    DropdownMenuItem(text = { Text("Hide albums") }, onClick = { menu = false })
                }
            }
        })
        if (showEssential) Card(
            colors = CardDefaults.cardColors(containerColor = VaultSurface),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose the albums you want close at hand.", style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.align(Alignment.End)) {
                    TextButton(onClick = onViewAll) { Text("View all") }
                    TextButton(onClick = onSettings) { Text("Settings") }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (showEssential) "Essential albums" else "All albums", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onViewAll) { Text("View all") }
        }
        AlbumGrid(if (showEssential) albums.take(12) else albums, onOpen)
    }
    if (create) AlertDialog(
        onDismissRequest = { create = false },
        title = { Text("Choose what to create") },
        text = { Text("Album\nCreate a new album and add pictures and videos manually.\n\nAuto-updating album\nAutomatically include pictures of people you select.\n\nGroup\nCreate a group of related albums.\n\nShared album\nCreate an album you can share.\n\nShared family album\nShare with your family through Android's share providers.") },
        confirmButton = { TextButton(onClick = { create = false }) { Text("Done") } },
    )
}

@Composable
private fun AllAlbumsScreen(albums: List<GalleryAlbum>, onBack: () -> Unit, onOpen: (GalleryAlbum) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("All albums", onBack)
        AlbumGrid(albums, onOpen)
    }
}

@Composable
private fun AlbumGrid(albums: List<GalleryAlbum>, onOpen: (GalleryAlbum) -> Unit) {
    if (albums.isEmpty()) { EmptyGallery(); return }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(116.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(albums, key = { it.bucketId }) { album ->
            Column(Modifier.combinedClickable(onClick = { onOpen(album) }, onLongClick = { onOpen(album) })) {
                AsyncImage(album.cover.uri, album.name, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
                Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 7.dp))
                Text(album.count.toString(), color = VaultSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MediaGridScreen(
    title: String,
    media: List<GalleryMedia>,
    selected: Set<Long>,
    query: String,
    searchOpen: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onQuery: (String) -> Unit,
    onOpen: (GalleryMedia) -> Unit,
    onSelect: (Long) -> Unit,
    onSelectionSet: (Set<Long>) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar(title, onBack, onSearch)
        AnimatedVisibility(searchOpen) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                label = { Text("Search this album") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (media.isEmpty()) EmptyGallery() else SlideSelectableMediaGrid(media, selected, onOpen, onSelect, onSelectionSet)
    }
}

@Composable
private fun SlideSelectableMediaGrid(
    media: List<GalleryMedia>,
    selected: Set<Long>,
    onOpen: (GalleryMedia) -> Unit,
    onSelect: (Long) -> Unit,
    onSelectionSet: (Set<Long>) -> Unit,
) {
    val context = LocalContext.current
    val gridColumns = context.getSharedPreferences("gallery-settings", 0).getInt("grid_columns", 4).coerceIn(3, 5)
    val gridState = rememberLazyGridState()
    val orderedIds = remember(media) { media.map { it.id } }
    val scope = rememberCoroutineScope()
    val latestSelected by rememberUpdatedState(selected)
    val latestSelectionSet by rememberUpdatedState(onSelectionSet)
    var dragAnchor by remember { mutableStateOf<Long?>(null) }
    var dragBase by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var dragSelect by remember { mutableStateOf(true) }

    fun mediaAt(x: Float, y: Float): Long? = gridState.layoutInfo.visibleItemsInfo
        .lastOrNull { info ->
            info.key is Long && x >= info.offset.x && x <= info.offset.x + info.size.width &&
                y >= info.offset.y && y <= info.offset.y + info.size.height
        }?.key as? Long

    fun updateSlideSelection(current: Long) {
        val anchor = dragAnchor ?: return
        latestSelectionSet(GalleryLogic.slideSelection(orderedIds, dragBase, anchor, current, dragSelect))
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(gridColumns),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalArrangement = Arrangement.spacedBy(1.5.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp).pointerInput(media) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                mediaAt(longPress.position.x, longPress.position.y)?.let { id ->
                    dragAnchor = id
                    dragBase = latestSelected
                    dragSelect = id !in latestSelected
                    updateSlideSelection(id)
                }
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    change.consume()
                    val viewportHeight = gridState.layoutInfo.viewportSize.height.toFloat()
                    when {
                        change.position.y < 88f -> scope.launch { gridState.scrollBy(-30f) }
                        change.position.y > viewportHeight - 88f -> scope.launch { gridState.scrollBy(30f) }
                    }
                    mediaAt(change.position.x, change.position.y)?.let(::updateSlideSelection)
                }
                dragAnchor = null
            }
        },
    ) { items(media, key = { it.id }) { MediaTile(it, it.id in selected, onOpen, onSelect) } }
}

@Composable
private fun StoriesScreen(media: List<GalleryMedia>, onOpen: (List<GalleryMedia>) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val format = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val stories = remember(media) {
        media.filter { it.kind == MediaKind.IMAGE }
            .groupBy { format.format(Date(it.dateTakenMs)) }
            .entries.map { it.key to it.value }.filter { it.second.size >= 2 }
    }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar(
            onSearch = {},
            extra = {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Create story") }, onClick = { menu = false })
                        DropdownMenuItem(text = { Text("Hide content") }, onClick = { menu = false })
                    }
                }
            },
        )
        if (stories.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 42.dp)) {
                Text("No stories", style = MaterialTheme.typography.headlineMedium, color = VaultSecondary)
                Spacer(Modifier.height(14.dp))
                Text("Experience your adventures again in curated collections automatically made from your pictures and videos.", textAlign = TextAlign.Center, color = VaultSecondary)
            }
        } else LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(stories, key = { it.first }) { (title, items) ->
                Column(Modifier.combinedClickable(onClick = { onOpen(items) }, onLongClick = { onOpen(items) })) {
                    AsyncImage(items.first().uri, title, Modifier.fillMaxWidth().aspectRatio(0.82f).clip(RoundedCornerShape(22.dp)), contentScale = ContentScale.Crop)
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text("${items.size} photos", color = VaultSecondary)
                }
            }
        }
    }
}

@Composable
private fun StoryViewer(items: List<GalleryMedia>, onBack: () -> Unit) {
    var index by remember(items) { mutableStateOf(0) }
    LaunchedEffect(index, items) {
        delay(3_500)
        if (index < items.lastIndex) index++ else onBack()
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(items[index].uri, items[index].name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
            Text("${index + 1} / ${items.size}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }
        Row(Modifier.fillMaxSize().padding(top = 96.dp, bottom = 48.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().combinedClickable(onClick = { if (index > 0) index-- }, onLongClick = {}))
            Box(Modifier.weight(1f).fillMaxHeight().combinedClickable(onClick = { if (index < items.lastIndex) index++ else onBack() }, onLongClick = {}))
        }
    }
}

@Composable
private fun SamsungCollectionScreen(title: String, media: List<GalleryMedia>, onBack: () -> Unit, onOpen: (GalleryMedia) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar(
            title = title,
            back = onBack,
            extra = {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Select") }, onClick = { menu = false })
                        DropdownMenuItem(text = { Text("Create") }, onClick = { menu = false })
                        DropdownMenuItem(text = { Text("Add to Home screen") }, onClick = { menu = false })
                        DropdownMenuItem(text = { Text("Start slideshow") }, onClick = { menu = false })
                    }
                }
            },
        )
        if (media.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Favorite, null, Modifier.size(52.dp), tint = VaultSecondary)
                    Text(if (title == "Favourites") "No favourites" else "No pictures or videos", style = MaterialTheme.typography.titleLarge)
                    Text(if (title == "Favourites") "Tap the heart on your favourite shots so you can find them here fast." else "New media will appear here automatically.", color = VaultSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 36.dp))
                }
            }
        } else {
            Text("${media.count { it.kind == MediaKind.IMAGE }} images  ${media.count { it.kind == MediaKind.VIDEO }} videos", color = VaultSecondary, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            ) { items(media, key = { it.id }) { MediaTile(it, false, onOpen, {}) } }
        }
    }
}

@Composable
private fun GallerySearchScreen(media: List<GalleryMedia>, onBack: () -> Unit, onOpen: (GalleryMedia) -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-search-settings", 0) }
    var query by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    val results = remember(media, query) { if (query.isBlank()) emptyList() else media.filter { it.name.contains(query, true) || it.bucketName.contains(query, true) || it.mimeType.contains(query, true) } }
    if (settingsOpen) {
        SearchSettingsScreen(preferences, onBack = { settingsOpen = false })
        return
    }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar(
            back = onBack,
            extra = {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Search settings") }, onClick = { menu = false; settingsOpen = true })
                    }
                }
            },
        )
        if (query.isBlank()) {
            Column(Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SearchCategory("Locations", listOf("View all", "On this device"))
                SearchCategory("Activity", listOf("AI-edited", "Edited"))
                SearchCategory("Shot types", listOf("Video ${media.count { it.kind == MediaKind.VIDEO }}", "Scan", "Selfie", "Portrait"))
                SearchCategory("Documents", listOf("Receipts", "Screenshots", "Text"))
            }
        } else if (results.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No results for “$query”", color = VaultSecondary) }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                items(results, key = { it.id }) { MediaTile(it, false, onOpen, {}) }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(14.dp),
        )
    }
}

@Composable
private fun SearchCategory(title: String, values: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        values.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { value -> Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), modifier = Modifier.weight(1f)) { Text(value, Modifier.padding(18.dp)) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SearchSettingsScreen(preferences: android.content.SharedPreferences, onBack: () -> Unit) {
    var recent by remember { mutableStateOf(preferences.getBoolean("recent", true)) }
    var suggestions by remember { mutableStateOf(preferences.getBoolean("suggestions", true)) }
    val categories = listOf("Search shortcuts", "People", "Locations", "Documents", "Activity", "Shot types", "My tags")
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("Search settings", onBack)
        SettingsGroup {
            SettingSwitch("Show recent searches", recent) { recent = it; preferences.edit().putBoolean("recent", it).apply() }
            SettingSwitch("Show suggestions", suggestions) { suggestions = it; preferences.edit().putBoolean("suggestions", it).apply() }
        }
        Text("Search categories", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(18.dp))
        Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            categories.forEach { category ->
                var checked by remember(category) { mutableStateOf(preferences.getBoolean("category_$category", category != "My tags")) }
                SettingSwitch(category, checked) { checked = it; preferences.edit().putBoolean("category_$category", it).apply() }
            }
        }
    }
}

@Composable
private fun CleanOutScreen(media: List<GalleryMedia>, onBack: () -> Unit) {
    val duplicates = remember(media) { media.groupBy { Triple(it.sizeBytes, it.width, it.height) }.values.filter { it.size > 1 }.flatten() }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("Clean out", onBack)
        if (duplicates.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.CleaningServices, null, Modifier.size(56.dp), tint = VaultSecondary)
                    Text("No clean out needed", style = MaterialTheme.typography.titleLarge)
                    Text("Duplicate pictures, old documents, and more will appear here so you can get rid of them easily.", color = VaultSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp))
                }
            }
        } else {
            Text("Possible duplicates", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(18.dp))
            Text("Review carefully before deleting. Matching file size and dimensions can still be different photos.", color = VaultSecondary, modifier = Modifier.padding(horizontal = 18.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize().padding(8.dp)) { items(duplicates, key = { it.id }) { MediaTile(it, false, {}, {}) } }
        }
    }
}

@Composable
private fun LocationsScreen(media: List<GalleryMedia>, onBack: () -> Unit, onOpen: (GalleryMedia) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("Locations", onBack)
        Text("On this device", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(18.dp))
        Text("Android limits location metadata for some media. Items with accessible location details are grouped here locally.", color = VaultSecondary, modifier = Modifier.padding(horizontal = 18.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize().padding(8.dp)) { items(media.take(40), key = { it.id }) { MediaTile(it, false, onOpen, {}) } }
    }
}

@Composable
private fun SharedAlbumsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("Shared albums", onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(38.dp)) {
                Icon(Icons.Outlined.FolderShared, null, Modifier.size(62.dp), tint = VaultSecondary)
                Text("Stay connected", style = MaterialTheme.typography.headlineMedium)
                Text("Create albums everyone can add to. On Pixel, sharing uses Android's system share sheet so you stay in control of the provider and recipients.", color = VaultSecondary, textAlign = TextAlign.Center)
                Button(onClick = {}) { Text("Get started") }
            }
        }
    }
}

@Composable
private fun MenuScreen(
    onVideos: () -> Unit,
    onRecent: () -> Unit,
    onFavourites: () -> Unit,
    onCleanOut: () -> Unit,
    onLocations: () -> Unit,
    onSharedAlbums: () -> Unit,
    onSettings: () -> Unit,
    onTrash: () -> Unit,
    onSecure: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar()
        Spacer(Modifier.weight(1f))
        Card(
            colors = CardDefaults.cardColors(containerColor = VaultRaised), shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(10.dp),
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MenuAction(Icons.Outlined.VideoLibrary, "Videos", onVideos)
                    MenuAction(Icons.Outlined.Favorite, "Favourites", onFavourites)
                    MenuAction(Icons.Outlined.Today, "Recent", onRecent)
                    MenuAction(Icons.Outlined.CleaningServices, "Clean out", onCleanOut)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MenuAction(Icons.Outlined.LocationOn, "Locations", onLocations)
                    MenuAction(Icons.Outlined.FolderShared, "Shared\nalbums", onSharedAlbums)
                    MenuAction(Icons.Outlined.Delete, "Recycle bin", onTrash)
                    MenuAction(Icons.Outlined.Settings, "Settings", onSettings)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4A4A4F)),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth().combinedClickable(role = Role.Button, onClick = onSecure, onLongClick = onSecure),
                ) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, "Secure Gallery", Modifier.size(30.dp), tint = VaultSecure)
                        Text("Open Secure Gallery", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp).weight(1f))
                        Text("›", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.width(82.dp).combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onClick).padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { Icon(icon, label, Modifier.size(30.dp)); Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun PublicBottomNavigation(selected: PublicTab, onSelected: (PublicTab) -> Unit) {
    Row(Modifier.fillMaxWidth().height(82.dp).background(VaultBackground), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
        NavItem(Icons.Outlined.Image, "Pictures", selected == PublicTab.PICTURES) { onSelected(PublicTab.PICTURES) }
        NavItem(Icons.Outlined.Collections, "Albums", selected == PublicTab.ALBUMS) { onSelected(PublicTab.ALBUMS) }
        NavItem(Icons.Outlined.Movie, "Stories", selected == PublicTab.STORIES) { onSelected(PublicTab.STORIES) }
        NavItem(Icons.Outlined.Menu, "Menu", selected == PublicTab.MENU) { onSelected(PublicTab.MENU) }
    }
}

@Composable
private fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) VaultPrimary else VaultSecondary
    Column(
        Modifier.width(86.dp).combinedClickable(role = Role.Tab, onClick = onClick, onLongClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) { Icon(icon, label, Modifier.size(27.dp), tint = color); Text(label, color = color, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun SelectionBar(
    onShare: () -> Unit,
    onSecure: () -> Unit,
    favouriteLabel: String,
    onFavourite: () -> Unit,
    onDelete: () -> Unit,
) {
    var createMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(82.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
        BottomAction(Icons.Outlined.Add, "Create", { createMenu = true })
        BottomAction(Icons.Outlined.Share, "Share", onShare)
        BottomAction(Icons.Outlined.Delete, "Delete", onDelete)
        Box {
            BottomAction(Icons.Outlined.MoreVert, "More", { moreMenu = true })
            DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                DropdownMenuItem(text = { Text("Copy to clipboard") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { moreMenu = false })
                DropdownMenuItem(text = { Text("Copy to album") }, onClick = { moreMenu = false })
                DropdownMenuItem(text = { Text("Move to album") }, onClick = { moreMenu = false })
                DropdownMenuItem(text = { Text("Add to shared album") }, onClick = { moreMenu = false })
                DropdownMenuItem(text = { Text(if (favouriteLabel == "Unfavourite") "Remove from favourites" else "Add to favourites") }, leadingIcon = { Icon(Icons.Outlined.Favorite, null) }, onClick = { moreMenu = false; onFavourite() })
                DropdownMenuItem(text = { Text("Add tag") }, onClick = { moreMenu = false })
                DropdownMenuItem(text = { Text("Edit date and time") }, onClick = { moreMenu = false })
                DropdownMenuItem(text = { Text("Edit location") }, onClick = { moreMenu = false })
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Move to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { moreMenu = false; onSecure() })
            }
        }
    }
    if (createMenu) AlertDialog(
        onDismissRequest = { createMenu = false },
        title = { Text("Create from selected items") },
        text = { Text("GIF\nCollage\nMovie") },
        confirmButton = { TextButton(onClick = { createMenu = false }) { Text("Done") } },
    )
}

@Composable
private fun BottomAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label); Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun GallerySettings(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    var oneDrive by remember { mutableStateOf(preferences.getBoolean("onedrive", false)) }
    var fullScreen by remember { mutableStateOf(preferences.getBoolean("full_screen_scroll", false)) }
    var motionPhotos by remember { mutableStateOf(preferences.getBoolean("motion_photos", true)) }
    var externalPlayer by remember { mutableStateOf(preferences.getBoolean("external_player", false)) }
    var essential by remember { mutableStateOf(preferences.getBoolean("essential", true)) }
    var stories by remember { mutableStateOf(preferences.getBoolean("auto_stories", true)) }
    var notifications by remember { mutableStateOf(preferences.getBoolean("story_notifications", true)) }
    var mergeAlbums by remember { mutableStateOf(preferences.getBoolean("merge_albums", true)) }
    var sharedNotifications by remember { mutableStateOf(preferences.getBoolean("shared_notifications", true)) }
    var placeNames by remember { mutableStateOf(preferences.getBoolean("place_names", false)) }
    var secureLauncher by remember { mutableStateOf(isSecureLauncherVisible(context)) }
    var dialog by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("Gallery settings", onBack)
        androidx.compose.foundation.rememberScrollState().let { scroll ->
            Column(Modifier.fillMaxSize().padding(horizontal = 10.dp).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsGroup {
                    SettingSwitch("Sync with OneDrive", "Cloud syncing is optional and handled through Android's document providers.", oneDrive) { oneDrive = it; preferences.edit().putBoolean("onedrive", it).apply() }
                }
                SettingsHeader("Viewing")
                SettingsGroup {
                    SettingSwitch("Full screen scrolling", fullScreen) { fullScreen = it; preferences.edit().putBoolean("full_screen_scroll", it).apply() }
                    SettingSwitch("Auto play motion photos", motionPhotos) { motionPhotos = it; preferences.edit().putBoolean("motion_photos", it).apply() }
                    SettingSwitch("Open in video player", externalPlayer) { externalPlayer = it; preferences.edit().putBoolean("external_player", it).apply() }
                }
                SettingsHeader("Editing")
                SettingsGroup {
                    SettingLink("Photo assist") { dialog = "Photo assist\n\nGenerative edit\nSketch to image\nPortrait studio\n\nAI editing is off by default. If enabled in a future provider integration, images will only be sent after explicit confirmation." }
                    SettingLink("Photo Editor settings") { dialog = "Photo Editor settings\n\nPermissions\nAbout Photo Editor" }
                }
                SettingsHeader("Stories")
                SettingsGroup {
                    SettingSwitch("Auto create stories", stories) { stories = it; preferences.edit().putBoolean("auto_stories", it).apply() }
                    SettingSwitch("Notifications", notifications) { notifications = it; preferences.edit().putBoolean("story_notifications", it).apply() }
                }
                SettingsHeader("Albums")
                SettingsGroup {
                    SettingSwitch("Select essential albums", "Show only the albums you select on the Albums tab instead of showing them all.", essential) { essential = it; preferences.edit().putBoolean("essential", it).apply() }
                    SettingSwitch("Merge albums", "Albums with the same name will be shown as a single album.", mergeAlbums) { mergeAlbums = it; preferences.edit().putBoolean("merge_albums", it).apply() }
                    SettingSwitch("Shared album notifications", sharedNotifications) { sharedNotifications = it; preferences.edit().putBoolean("shared_notifications", it).apply() }
                }
                SettingsGroup { SettingSwitch("Show place names", placeNames) { placeNames = it; preferences.edit().putBoolean("place_names", it).apply() } }
                SettingsHeader("Privacy")
                SettingsGroup {
                    SettingLink("Privacy Policy") { dialog = "Vault Gallery processes media locally and does not upload it by default. Secure Gallery exports plaintext only after your explicit action." }
                    SettingLink("Permissions") {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
                    }
                }
                SettingsHeader("Secure Gallery")
                SettingsGroup {
                    SettingSwitch("Show Secure Gallery icon", "Keep a separate launcher entry, similar to Samsung Secure Folder.", secureLauncher) {
                        secureLauncher = it; setSecureLauncherVisible(context, it)
                    }
                    SettingLink("Secure Gallery settings") { context.startActivity(Intent(context, SecureGalleryActivity::class.java)) }
                }
                SettingsGroup { SettingLink("About Gallery") { dialog = "Vault Gallery 2.0\nA local-first Pixel gallery with Samsung-style navigation, slide selection, system trash, favourites, search, stories, and encrypted biometric Secure Gallery." } }
                Spacer(Modifier.height(22.dp))
            }
        }
    }
    dialog?.let { message -> AlertDialog(onDismissRequest = { dialog = null }, confirmButton = { TextButton(onClick = { dialog = null }) { Text("OK") } }, text = { Text(message) }) }
}

@Composable private fun SettingsHeader(text: String) { Text(text, color = VaultSecondary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 18.dp, top = 12.dp)) }

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) { Column { content() } }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(74.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f)); Switch(checked, onChecked)
    }
    HorizontalDivider(color = Color(0xFF38383D), modifier = Modifier.padding(horizontal = 18.dp))
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, color = VaultSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
        Switch(checked, onChecked)
    }
    HorizontalDivider(color = Color(0xFF38383D), modifier = Modifier.padding(horizontal = 18.dp))
}

@Composable
private fun SettingLink(title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(74.dp).combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onClick).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
    HorizontalDivider(color = Color(0xFF38383D), modifier = Modifier.padding(horizontal = 18.dp))
}

@Composable
private fun PublicViewer(
    media: GalleryMedia,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onSecure: () -> Unit,
    onFavourite: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var deletePrompt by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var assist by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (media.kind == MediaKind.VIDEO) PublicVideo(media.uri) else ZoomableImage(media.uri, media.name)
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Outlined.Slideshow, "Smart View") }
            IconButton(onClick = {}) { Icon(Icons.Outlined.Restore, "Rotate") }
            Box {
                IconButton(onClick = { moreMenu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                    DropdownMenuItem(text = { Text("Details") }, leadingIcon = { Icon(Icons.Outlined.Info, null) }, onClick = { moreMenu = false; details = true })
                    DropdownMenuItem(text = { Text("Copy to clipboard") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = {
                        moreMenu = false
                        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard.setPrimaryClip(android.content.ClipData.newUri(context.contentResolver, media.name, media.uri))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    })
                    if (media.kind == MediaKind.VIDEO) DropdownMenuItem(text = { Text("Open in Video player") }, onClick = {
                        moreMenu = false; context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(media.uri, media.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    })
                    DropdownMenuItem(text = { Text("Set as wallpaper") }, onClick = {
                        moreMenu = false; context.startActivity(Intent(Intent.ACTION_ATTACH_DATA).setDataAndType(media.uri, media.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra("mimeType", media.mimeType))
                    })
                    DropdownMenuItem(text = { Text("Move to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { moreMenu = false; onSecure() })
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().background(Color(0xCC111113)).padding(10.dp), horizontalArrangement = Arrangement.SpaceAround) {
            BottomAction(Icons.Outlined.Favorite, if (media.isFavourite) "Unfavourite" else "Favourite", onFavourite)
            BottomAction(Icons.Outlined.Edit, "Edit", {
                runCatching { context.startActivity(Intent(Intent.ACTION_EDIT).setDataAndType(media.uri, media.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                    .onFailure { Toast.makeText(context, "No compatible editor is installed", Toast.LENGTH_SHORT).show() }
            })
            BottomAction(Icons.Outlined.AutoAwesome, "Photo assist", { assist = true })
            BottomAction(Icons.Outlined.Share, "Share", onShare)
            BottomAction(Icons.Outlined.Delete, "Delete", { deletePrompt = true })
        }
    }
    if (deletePrompt) AlertDialog(
        onDismissRequest = { deletePrompt = false },
        title = { Text("Move to recycle bin?") },
        text = { Text("Android keeps the item in the system recycle bin so you can restore it later.") },
        dismissButton = { TextButton(onClick = { deletePrompt = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { deletePrompt = false; onDelete() }) { Text("Move to bin") } },
    )
    if (details) AlertDialog(
        onDismissRequest = { details = false },
        title = { Text(media.name) },
        text = { Text("${SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(media.dateTakenMs))}\n${media.width} × ${media.height}\n${GalleryLogic.fileSizeLabel(media.sizeBytes)}\n${media.mimeType}\nAlbum: ${media.bucketName}") },
        confirmButton = { TextButton(onClick = { details = false }) { Text("Done") } },
    )
    if (assist) AlertDialog(
        onDismissRequest = { assist = false },
        title = { Text("Photo assist") },
        text = { Text("Generative edit\nSketch to image\nPortrait studio\n\nThese Galaxy AI services are Samsung-exclusive. Vault Gallery keeps this entry explicit and does not upload your image without a configured AI provider and confirmation.") },
        confirmButton = { TextButton(onClick = { assist = false }) { Text("Done") } },
    )
}

@Composable
private fun PublicTrashScreen(
    media: List<GalleryMedia>,
    onBack: () -> Unit,
    onRestore: (GalleryMedia) -> Unit,
    onDelete: (GalleryMedia) -> Unit,
    onEmpty: () -> Unit,
) {
    var selected by remember { mutableStateOf<GalleryMedia?>(null) }
    var emptyPrompt by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar(
            title = "Recycle bin",
            back = onBack,
            extra = { if (media.isNotEmpty()) TextButton(onClick = { emptyPrompt = true }) { Text("Empty") } },
        )
        Text("Items remain here according to Android's system recycle-bin policy.", color = VaultSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (media.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Recycle bin is empty", color = VaultSecondary) }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) { items(media, key = { it.id }) { item -> MediaTile(item, false, { selected = it }, {}) } }
        }
    }
    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(item.name) },
            text = { Text("Restore this item or delete it permanently?") },
            dismissButton = { TextButton(onClick = { selected = null; onDelete(item) }) { Text("Delete permanently", color = MaterialTheme.colorScheme.error) } },
            confirmButton = { TextButton(onClick = { selected = null; onRestore(item) }) { Text("Restore") } },
        )
    }
    if (emptyPrompt) AlertDialog(
        onDismissRequest = { emptyPrompt = false },
        title = { Text("Empty recycle bin?") },
        text = { Text("All ${media.size} items will be permanently deleted. This cannot be undone.") },
        dismissButton = { TextButton(onClick = { emptyPrompt = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { emptyPrompt = false; onEmpty() }) { Text("Delete all", color = MaterialTheme.colorScheme.error) } },
    )
}

@Composable
private fun ZoomableImage(uri: Uri, description: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    AsyncImage(
        uri, description,
        Modifier.fillMaxSize().pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(1f, 5f)
                if (scale > 1f) { x += pan.x; y += pan.y } else { x = 0f; y = 0f }
            }
        }.graphicsLayer(scaleX = scale, scaleY = scale, translationX = x, translationY = y),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun PublicVideo(uri: Uri) {
    val context = LocalContext.current
    val player = remember(uri) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(uri)); prepare(); playWhenReady = true } }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxSize())
}

private fun shareUris(activity: Activity, uris: List<Uri>, mime: String) {
    if (uris.isEmpty()) return
    val intent = if (uris.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
    else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    intent.type = mime.ifBlank { "*/*" }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    activity.startActivity(Intent.createChooser(intent, "Share media"))
}

private fun launchSecureImport(activity: Activity, uris: ArrayList<Uri>) {
    activity.startActivity(Intent(activity, SecureGalleryActivity::class.java).putParcelableArrayListExtra("import_uris", uris))
}
