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
import android.util.Size
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
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
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
import androidx.compose.material.icons.outlined.Pause
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        applyFullScreenPreference()
        setContent { VaultTheme { PublicGalleryApp(galleryViewModel, this, intent.getStringExtra("collection")) } }
    }

    override fun onResume() {
        super.onResume()
        applyFullScreenPreference()
        galleryViewModel.refresh()
    }

    fun applyFullScreenPreference(enabled: Boolean = getSharedPreferences("gallery-settings", 0).getBoolean("full_screen_scroll", false)) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (enabled) controller.hide(WindowInsetsCompat.Type.systemBars()) else controller.show(WindowInsetsCompat.Type.systemBars())
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
private fun PublicGalleryApp(viewModel: GalleryViewModel, activity: MainGalleryActivity, initialCollection: String?) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(PublicTab.PICTURES) }
    var viewer by remember { mutableStateOf<GalleryMedia?>(null) }
    var settings by remember { mutableStateOf(false) }
    var allAlbums by remember { mutableStateOf(false) }
    var albumFilter by remember { mutableStateOf<Set<Long>?>(null) }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var kindFilter by remember { mutableStateOf<MediaKind?>(null) }
    var favouriteOnly by remember { mutableStateOf(false) }
    var trashOpen by remember { mutableStateOf(false) }
    var auxiliaryScreen by remember(initialCollection) {
        mutableStateOf(
            when (initialCollection) {
                "Videos" -> AuxiliaryScreen.VIDEOS
                "Recent" -> AuxiliaryScreen.RECENT
                "Favourites" -> AuxiliaryScreen.FAVOURITES
                else -> null
            },
        )
    }
    var storyItems by remember { mutableStateOf<List<GalleryMedia>?>(null) }
    var selectedAlbumIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val galleryPreferences = remember { activity.getSharedPreferences("gallery-settings", 0) }
    var essentialAlbumIds by remember {
        mutableStateOf<Set<Long>>(galleryPreferences.getStringSet("essential_album_ids", emptySet()).orEmpty().mapNotNullTo(LinkedHashSet()) { it.toLongOrNull() })
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refresh() }

    LaunchedEffect(Unit) { viewModel.refresh() }
    BackHandler(viewer != null || storyItems != null || settings || trashOpen || auxiliaryScreen != null || allAlbums || albumFilter != null || state.selectedIds.isNotEmpty() || selectedAlbumIds.isNotEmpty()) {
        when {
            viewer != null -> viewer = null
            storyItems != null -> storyItems = null
            settings -> settings = false
            trashOpen -> trashOpen = false
            auxiliaryScreen != null -> auxiliaryScreen = null
            state.selectedIds.isNotEmpty() -> viewModel.clearSelection()
            selectedAlbumIds.isNotEmpty() -> selectedAlbumIds = emptySet()
            allAlbums -> allAlbums = false
            albumFilter != null -> albumFilter = null
        }
    }

    if (viewer != null) {
        PublicViewer(
            media = viewer!!,
            onBack = { viewer = null },
            onShare = { shareUris(activity, listOf(viewer!!.uri), viewer!!.mimeType) },
            onSecure = { move -> launchSecureImport(activity, arrayListOf(viewer!!.uri), move) },
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
            AuxiliaryScreen.CLEAN_OUT -> CleanOutScreen(state.media, onBack = { auxiliaryScreen = null }, onOpen = { viewer = it })
            AuxiliaryScreen.LOCATIONS -> LocationsScreen(state.media, onBack = { auxiliaryScreen = null }, onOpen = { viewer = it })
            AuxiliaryScreen.SHARED_ALBUMS -> SharedAlbumsScreen(onBack = { auxiliaryScreen = null })
        }
        return
    }

    val filtered = remember(state.media, query, albumFilter, kindFilter, favouriteOnly) {
        state.media.filter { media ->
            (albumFilter == null || media.bucketId in albumFilter.orEmpty()) &&
                (kindFilter == null || media.kind == kindFilter) &&
                (!favouriteOnly || media.isFavourite) &&
                (query.isBlank() || media.name.contains(query, true) || media.bucketName.contains(query, true))
        }
    }
    val mergeAlbums = galleryPreferences.getBoolean("merge_albums", true)
    val albums = remember(state.media, mergeAlbums) { MediaStoreRepository(activity).albums(state.media, mergeAlbums) }
    val selectedMedia = state.media.filter { it.id in state.selectedIds }
    val selectedUris = selectedMedia.map { it.uri }
    val allSelectedFavourite = selectedMedia.isNotEmpty() && selectedMedia.all { it.isFavourite }

    Scaffold(
        containerColor = VaultBackground,
        bottomBar = {
            Column(Modifier.background(VaultBackground).navigationBarsPadding()) {
                if (state.selectedIds.isNotEmpty()) {
                    SelectionBar(
                        selectedMedia = selectedMedia,
                        onShare = { shareUris(activity, selectedUris, "*/*") },
                        onSecure = { move -> launchSecureImport(activity, ArrayList(selectedUris), move) },
                        favouriteLabel = if (allSelectedFavourite) "Unfavourite" else "Favourite",
                        onFavourite = { activity.setFavourite(selectedUris, !allSelectedFavourite) },
                        onDelete = { activity.deleteMedia(selectedUris) },
                    )
                } else if (selectedAlbumIds.isNotEmpty()) {
                    val selectedAlbums = albums.filter { it.bucketId in selectedAlbumIds }
                    val selectedBuckets = selectedAlbums.flatMapTo(LinkedHashSet()) { it.bucketIds }
                    val albumMedia = state.media.filter { it.bucketId in selectedBuckets }
                    AlbumSelectionBar(
                        selectedAlbums = selectedAlbums,
                        selectedMedia = albumMedia,
                        onShare = { shareUris(activity, albumMedia.map { it.uri }, "*/*") },
                        onSecure = { move -> launchSecureImport(activity, ArrayList(albumMedia.map { it.uri }), move) },
                        essentialAlbumIds = essentialAlbumIds,
                        onEssentialChanged = { ids ->
                            essentialAlbumIds = ids
                            galleryPreferences.edit().putStringSet("essential_album_ids", ids.mapTo(LinkedHashSet()) { it.toString() }).apply()
                        },
                        onDelete = { activity.deleteMedia(albumMedia.map { it.uri }) },
                        onDone = { selectedAlbumIds = emptySet() },
                    )
                } else {
                    PublicBottomNavigation(tab) { next ->
                        tab = next; allAlbums = false; albumFilter = null; kindFilter = null; favouriteOnly = false; selectedAlbumIds = emptySet()
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
                allAlbums -> AllAlbumsScreen(
                    albums = albums,
                    selected = selectedAlbumIds,
                    onBack = { if (selectedAlbumIds.isNotEmpty()) selectedAlbumIds = emptySet() else allAlbums = false },
                    onOpen = { albumFilter = it.bucketIds; allAlbums = false; selectedAlbumIds = emptySet() },
                    onToggleSelection = { id -> selectedAlbumIds = GalleryLogic.toggleSelection(selectedAlbumIds, id) },
                )
                albumFilter != null -> MediaGridScreen(
                    title = albums.firstOrNull { it.bucketIds == albumFilter }?.name ?: "Album",
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
                            if (!openExternalVideo(activity, it)) viewer = it
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
                            if (!openExternalVideo(activity, it)) viewer = it
                        } else viewer = it
                    },
                    onSelect = viewModel::toggleSelection,
                    onSelectionSet = viewModel::setSelection,
                    onSelectAll = { viewModel.selectAll(filtered) },
                    onDuplicates = { auxiliaryScreen = AuxiliaryScreen.CLEAN_OUT },
                )
                tab == PublicTab.ALBUMS -> AlbumsScreen(
                    albums,
                    onViewAll = { allAlbums = true },
                    onOpen = { albumFilter = it.bucketIds; selectedAlbumIds = emptySet() },
                    onSettings = { settings = true },
                    onRefresh = viewModel::refresh,
                    essentialAlbumIds = essentialAlbumIds,
                    selected = selectedAlbumIds,
                    onToggleSelection = { id -> selectedAlbumIds = GalleryLogic.toggleSelection(selectedAlbumIds, id) },
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
                    onSecure = { launchSecureGallery(activity) },
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
    onSelect: (Long) -> Unit, onSelectionSet: (Set<Long>) -> Unit, onSelectAll: () -> Unit,
    onDuplicates: () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    var gridColumns by remember { mutableIntStateOf(preferences.getInt("grid_columns", 4).coerceIn(3, 5)) }
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
                beforeSearch = { IconButton(onClick = {
                    gridColumns = if (gridColumns >= 5) 3 else gridColumns + 1
                    preferences.edit().putInt("grid_columns", gridColumns).apply()
                }) { Icon(Icons.Outlined.Collections, "Change layout, currently $gridColumns columns") } },
                extra = {
                    Box {
                        IconButton(onClick = { moreMenu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                            DropdownMenuItem(text = { Text("Select") }, onClick = { moreMenu = false; media.firstOrNull()?.let { onSelect(it.id) } })
                            DropdownMenuItem(text = { Text("Create") }, onClick = { moreMenu = false; createMenu = true })
                            DropdownMenuItem(text = { Text("Start slideshow") }, onClick = { moreMenu = false; slideshow = true })
                            DropdownMenuItem(text = { Text("View duplicates") }, onClick = { moreMenu = false; onDuplicates() })
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
        else TimelineGrid(media, selected, gridColumns, onOpen, onSelect, onSelectionSet)
    }
    if (createMenu) AlertDialog(
        onDismissRequest = { createMenu = false },
        title = { Text("Create") },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MenuAction(Icons.Outlined.AutoAwesome, "GIF", { createMenu = false; openCreativeChooser(context, media.take(30), "GIF") })
                MenuAction(Icons.Outlined.GridView, "Collage", { createMenu = false; openCreativeChooser(context, media.take(30), "Collage") })
                MenuAction(Icons.Outlined.Movie, "Movie", { createMenu = false; openCreativeChooser(context, media.take(30), "Movie") })
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
    gridColumns: Int,
    onOpen: (GalleryMedia) -> Unit,
    onSelect: (Long) -> Unit,
    onSelectionSet: (Set<Long>) -> Unit,
) {
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
private fun MediaTile(item: GalleryMedia, selected: Boolean, onOpen: (GalleryMedia) -> Unit, onSelect: ((Long) -> Unit)?) {
    val context = LocalContext.current
    val thumbnail = remember(item.uri, item.kind) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .apply { if (item.kind == MediaKind.VIDEO) videoFrameMillis(1_000) }
            .memoryCacheKey("gallery-thumb-${item.id}-${item.dateTakenMs}")
            .diskCacheKey("gallery-thumb-${item.id}-${item.dateTakenMs}")
            .build()
    }
    Box(
        Modifier.aspectRatio(1f).clip(RoundedCornerShape(1.dp)).background(VaultSurface)
            .then(if (onSelect != null) Modifier.semantics { onLongClick("Select") { onSelect(item.id); true } } else Modifier)
            .combinedClickable(onClick = { onOpen(item) }, onLongClick = { onSelect?.invoke(item.id) }),
    ) {
        GalleryThumbnail(item, thumbnail)
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
private fun GalleryThumbnail(item: GalleryMedia, fallback: ImageRequest) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, item.uri, item.dateTakenMs) {
        if (item.kind == MediaKind.VIDEO && Build.VERSION.SDK_INT >= 29) {
            value = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.loadThumbnail(item.uri, Size(640, 640), null) }.getOrNull()
            }
        }
    }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    else AsyncImage(fallback, item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}

@Composable
private fun AlbumsScreen(
    albums: List<GalleryAlbum>,
    onViewAll: () -> Unit,
    onOpen: (GalleryAlbum) -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    essentialAlbumIds: Set<Long>,
    selected: Set<Long>,
    onToggleSelection: (Long) -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    val showEssential = preferences.getBoolean("essential", true)
    var showEducation by remember { mutableStateOf(!preferences.getBoolean("albums_education_dismissed", false)) }
    var hiddenAlbumIds by remember { mutableStateOf(preferences.getStringSet("hidden_album_ids", emptySet()).orEmpty()) }
    val visibleAlbums = remember(albums, hiddenAlbumIds) { albums.filterNot { it.bucketId.toString() in hiddenAlbumIds } }
    var menu by remember { mutableStateOf(false) }
    var create by remember { mutableStateOf(false) }
    var hideAlbums by remember { mutableStateOf(false) }
    var pendingCreateUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val destinationPicker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        val sources = pendingCreateUris
        pendingCreateUris = emptyList()
        if (treeUri != null && sources.isNotEmpty()) scope.launch {
            val copied = withContext(Dispatchers.IO) { copyUrisToTree(context, sources, treeUri) }
            Toast.makeText(context, if (copied == sources.size) "Album created with $copied items" else "Copied $copied of ${sources.size} items", Toast.LENGTH_LONG).show()
            onRefresh()
        }
    }
    val createMediaPicker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            pendingCreateUris = uris
            destinationPicker.launch(null)
        }
    }
    val essentialAlbums = if (essentialAlbumIds.isEmpty()) visibleAlbums.take(12) else visibleAlbums.filter { it.bucketId in essentialAlbumIds }
    val displayedAlbums = if (showEssential) essentialAlbums else visibleAlbums
    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) SelectionHeader(selected.size, displayedAlbums.size, {
            displayedAlbums.filterNot { it.bucketId in selected }.forEach { onToggleSelection(it.bucketId) }
        }) {
            selected.toList().forEach(onToggleSelection)
        } else GalleryToolbar(extra = {
            IconButton(onClick = { create = true }) { Icon(Icons.Outlined.Add, "Create") }
            IconButton(onClick = onViewAll) { Icon(Icons.Outlined.Search, "Search") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Select") }, onClick = { menu = false; visibleAlbums.firstOrNull()?.let { onToggleSelection(it.bucketId) } })
                    DropdownMenuItem(text = { Text("Select essential albums") }, onClick = { menu = false; onSettings() })
                    DropdownMenuItem(text = { Text("Hide albums") }, onClick = { menu = false; hideAlbums = true })
                }
            }
        })
        if (showEducation) Card(
            colors = CardDefaults.cardColors(containerColor = VaultSurface),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("To show all your albums on the Albums tab, turn off Select essential albums in Settings.", style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.align(Alignment.End)) {
                    TextButton(onClick = { preferences.edit().putBoolean("albums_education_dismissed", true).apply(); showEducation = false }) { Text("Not now") }
                    TextButton(onClick = { preferences.edit().putBoolean("albums_education_dismissed", true).apply(); showEducation = false; onSettings() }) { Text("Settings") }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (showEssential) "Essential albums" else "All albums", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onViewAll) { Text("View all") }
        }
        AlbumGrid(displayedAlbums, selected, onOpen, onToggleSelection)
    }
    if (create) AlertDialog(
        onDismissRequest = { create = false },
        title = { Text("Create album") },
        text = { Text("Choose pictures and videos, then use Files to create or select their destination folder. Android will index that folder as an album.") },
        dismissButton = { TextButton(onClick = { create = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = {
            create = false
            createMediaPicker.launch(arrayOf("image/*", "video/*"))
        }) { Text("Choose media") } },
    )
    if (hideAlbums) AlertDialog(
        onDismissRequest = { hideAlbums = false },
        title = { Text("Hide albums") },
        text = {
            Column(Modifier.height(420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                albums.forEach { album ->
                    val hidden = album.bucketId.toString() in hiddenAlbumIds
                    Row(Modifier.fillMaxWidth().clickable {
                        hiddenAlbumIds = hiddenAlbumIds.toMutableSet().apply { if (hidden) remove(album.bucketId.toString()) else add(album.bucketId.toString()) }
                        preferences.edit().putStringSet("hidden_album_ids", hiddenAlbumIds).apply()
                    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(album.name, modifier = Modifier.weight(1f), maxLines = 1)
                        Switch(checked = hidden, onCheckedChange = null)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { hideAlbums = false }) { Text("Done") } },
    )
}

@Composable
private fun AllAlbumsScreen(
    albums: List<GalleryAlbum>, selected: Set<Long>, onBack: () -> Unit, onOpen: (GalleryAlbum) -> Unit,
    onToggleSelection: (Long) -> Unit,
) {
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(albums, query) { albums.filter { query.isBlank() || it.name.contains(query, true) } }
    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) SelectionHeader(selected.size, filtered.size, {
            filtered.filterNot { it.bucketId in selected }.forEach { onToggleSelection(it.bucketId) }
        }) { selected.toList().forEach(onToggleSelection) }
        else GalleryToolbar("All albums", onBack, onSearch = { searchOpen = !searchOpen })
        AnimatedVisibility(searchOpen) {
            OutlinedTextField(query, { query = it }, label = { Text("Search albums") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
        }
        AlbumGrid(filtered, selected, onOpen, onToggleSelection)
    }
}

@Composable
private fun AlbumGrid(albums: List<GalleryAlbum>, selected: Set<Long>, onOpen: (GalleryAlbum) -> Unit, onToggleSelection: (Long) -> Unit) {
    if (albums.isEmpty()) { EmptyGallery(); return }
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(116.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(albums, key = { it.bucketId }) { album ->
            val coverRequest = remember(album.cover.uri, album.cover.dateTakenMs) {
                ImageRequest.Builder(context).data(album.cover.uri)
                    .apply { if (album.cover.kind == MediaKind.VIDEO) videoFrameMillis(1_000) }
                    .memoryCacheKey("album-cover-${album.cover.id}-${album.cover.dateTakenMs}")
                    .build()
            }
            Column(Modifier.combinedClickable(
                onClick = { if (selected.isNotEmpty()) onToggleSelection(album.bucketId) else onOpen(album) },
                onLongClick = { onToggleSelection(album.bucketId) },
            )) {
                Box {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp))) { GalleryThumbnail(album.cover, coverRequest) }
                    if (album.bucketId in selected) {
                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Color(0x55000000)))
                        Box(Modifier.align(Alignment.TopStart).padding(9.dp).size(28.dp).clip(CircleShape).background(VaultBlue), contentAlignment = Alignment.Center) { Text("✓", color = Color.White) }
                    }
                }
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
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    var menu by remember { mutableStateOf(false) }
    var hideScreenshots by remember { mutableStateOf(preferences.getBoolean("stories_hide_screenshots", false)) }
    var autoStories by remember { mutableStateOf(preferences.getBoolean("auto_stories", true)) }
    val format = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val stories = remember(media, hideScreenshots, autoStories) {
        if (!autoStories) emptyList() else media.filter { it.kind == MediaKind.IMAGE && (!hideScreenshots || !it.bucketName.contains("screenshot", true)) }
            .groupBy { format.format(Date(it.dateTakenMs)) }
            .entries.map { it.key to it.value }.filter { it.second.size >= 2 }
    }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar(
            extra = {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Create story") }, onClick = {
                            menu = false
                            val recent = media.filter { it.kind == MediaKind.IMAGE }.take(30)
                            if (recent.isNotEmpty()) onOpen(recent) else Toast.makeText(context, "Add photos before creating a story", Toast.LENGTH_SHORT).show()
                        })
                        DropdownMenuItem(text = { Text(if (hideScreenshots) "Show screenshots in stories" else "Hide screenshots from stories") }, onClick = {
                            menu = false; hideScreenshots = !hideScreenshots; preferences.edit().putBoolean("stories_hide_screenshots", hideScreenshots).apply()
                        })
                    }
                }
            },
        )
        if (stories.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 42.dp)) {
                Text("No stories", style = MaterialTheme.typography.headlineMedium, color = VaultSecondary)
                Spacer(Modifier.height(14.dp))
                Text(if (autoStories) "Experience your adventures again in curated collections automatically made from your pictures and videos." else "Automatic stories are off. You can turn them back on here at any time.", textAlign = TextAlign.Center, color = VaultSecondary)
                if (!autoStories) Button(onClick = {
                    autoStories = true
                    preferences.edit().putBoolean("auto_stories", true).apply()
                }, modifier = Modifier.padding(top = 18.dp)) { Text("Turn on automatic stories") }
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
    var index by remember(items) { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    LaunchedEffect(index, items, paused) {
        if (!paused) {
            delay(3_500)
            if (index < items.lastIndex) index++ else onBack()
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(items[index].uri, items[index].name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
            Text("${index + 1} / ${items.size}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }
        Row(Modifier.fillMaxSize().padding(top = 96.dp, bottom = 48.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().combinedClickable(onClick = { if (index > 0) index-- }, onLongClick = { paused = !paused }))
            Box(Modifier.weight(1f).fillMaxHeight().combinedClickable(onClick = { if (index < items.lastIndex) index++ else onBack() }, onLongClick = { paused = !paused }))
        }
        if (paused) Icon(Icons.Outlined.Pause, "Story paused", Modifier.align(Alignment.Center).size(58.dp).clip(CircleShape).background(Color(0x99000000)).padding(12.dp))
    }
}

@Composable
private fun SamsungCollectionScreen(title: String, media: List<GalleryMedia>, onBack: () -> Unit, onOpen: (GalleryMedia) -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var create by remember { mutableStateOf(false) }
    var slideshow by remember { mutableStateOf(false) }
    val selectedMedia = remember(media, selected) { media.filter { it.id in selected } }
    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) SelectionHeader(selected.size, media.size, { selected = media.mapTo(LinkedHashSet()) { it.id } }, { selected = emptySet() })
        else GalleryToolbar(
            title = title,
            back = onBack,
            extra = {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Select") }, onClick = { menu = false; media.firstOrNull()?.let { selected = setOf(it.id) } })
                        DropdownMenuItem(text = { Text("Create") }, onClick = { menu = false; create = true })
                        DropdownMenuItem(text = { Text("Add to Home screen") }, onClick = { menu = false; pinCollectionShortcut(context, title) })
                        DropdownMenuItem(text = { Text("Start slideshow") }, onClick = { menu = false; if (media.isNotEmpty()) slideshow = true else Toast.makeText(context, "This collection is empty", Toast.LENGTH_SHORT).show() })
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
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            ) { items(media, key = { it.id }) { item -> MediaTile(item, item.id in selected, { if (selected.isNotEmpty()) selected = GalleryLogic.toggleSelection(selected, it.id) else onOpen(it) }, { selected = GalleryLogic.toggleSelection(selected, it) }) } }
            if (selected.isNotEmpty()) Row(Modifier.fillMaxWidth().navigationBarsPadding().height(82.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                BottomAction(Icons.Outlined.Share, "Share") { (context as? Activity)?.let { shareUris(it, selectedMedia.map { mediaItem -> mediaItem.uri }, "*/*") } }
                BottomAction(Icons.Outlined.AutoAwesome, "Create") { create = true }
                BottomAction(Icons.Outlined.Lock, "Secure") { (context as? Activity)?.let { launchSecureImport(it, ArrayList(selectedMedia.map { mediaItem -> mediaItem.uri }), false) } }
                BottomAction(Icons.Outlined.Delete, "Delete") { (context as? MainGalleryActivity)?.deleteMedia(selectedMedia.map { it.uri }) }
            }
        }
    }
    if (slideshow) StoryViewer(media, onBack = { slideshow = false })
    if (create) AlertDialog(
        onDismissRequest = { create = false },
        title = { Text("Create") },
        text = { Text("Choose GIF, collage, or movie in a compatible creator. Android will show the installed apps that can receive the selected media.") },
        dismissButton = { TextButton(onClick = { create = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = {
            create = false
            val source = if (selectedMedia.isNotEmpty()) selectedMedia else media.take(20)
            (context as? Activity)?.let { shareUris(it, source.map { item -> item.uri }, "*/*") }
        }) { Text("Choose creator") } },
    )
}

@Composable
private fun GallerySearchScreen(media: List<GalleryMedia>, onBack: () -> Unit, onOpen: (GalleryMedia) -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-search-settings", 0) }
    val tagPreferences = remember { context.getSharedPreferences("gallery-tags", 0) }
    var query by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    val results = remember(media, query) {
        if (query.isBlank()) emptyList() else media.filter {
            it.name.contains(query, true) || it.bucketName.contains(query, true) || it.mimeType.contains(query, true) ||
                tagPreferences.getString(it.id.toString(), "").orEmpty().contains(query, true)
        }
    }
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
                items(results, key = { it.id }) { MediaTile(it, false, onOpen, null) }
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
private fun CleanOutScreen(media: List<GalleryMedia>, onBack: () -> Unit, onOpen: (GalleryMedia) -> Unit) {
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
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize().padding(8.dp)) { items(duplicates, key = { it.id }) { MediaTile(it, false, onOpen, null) } }
        }
    }
}

@Composable
private fun LocationsScreen(media: List<GalleryMedia>, onBack: () -> Unit, onOpen: (GalleryMedia) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("Locations", onBack)
        Text("On this device", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(18.dp))
        Text("Android limits location metadata for some media. Items with accessible location details are grouped here locally.", color = VaultSecondary, modifier = Modifier.padding(horizontal = 18.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize().padding(8.dp)) { items(media.take(40), key = { it.id }) { MediaTile(it, false, onOpen, null) } }
    }
}

@Composable
private fun SharedAlbumsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) (context as? Activity)?.let { shareUris(it, uris, "*/*") }
    }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("Shared albums", onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(38.dp)) {
                Icon(Icons.Outlined.FolderShared, null, Modifier.size(62.dp), tint = VaultSecondary)
                Text("Stay connected", style = MaterialTheme.typography.headlineMedium)
                Text("Create albums everyone can add to. On Pixel, sharing uses Android's system share sheet so you stay in control of the provider and recipients.", color = VaultSecondary, textAlign = TextAlign.Center)
                Button(onClick = { picker.launch(arrayOf("image/*", "video/*")) }) { Text("Choose media to share") }
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
private fun SelectionHeader(selectedCount: Int, totalCount: Int, onSelectAll: () -> Unit, onCancel: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().height(92.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onSelectAll) { Text(if (selectedCount == totalCount) "All ✓" else "All") }
        Text("$selectedCount selected", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).padding(start = 8.dp))
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun AlbumSelectionBar(
    selectedAlbums: List<GalleryAlbum>,
    selectedMedia: List<GalleryMedia>,
    onShare: () -> Unit,
    onSecure: (Boolean) -> Unit,
    essentialAlbumIds: Set<Long>,
    onEssentialChanged: (Set<Long>) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var more by remember { mutableStateOf(false) }
    var moveAfterCopy by remember { mutableStateOf(false) }
    val folderPicker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri != null) scope.launch {
            val copied = withContext(Dispatchers.IO) { copyMediaToTree(context, selectedMedia, treeUri) }
            Toast.makeText(context, if (copied == selectedMedia.size) "Copied $copied items" else "Copied $copied of ${selectedMedia.size} items", Toast.LENGTH_LONG).show()
            if (moveAfterCopy && copied == selectedMedia.size) {
                onDone()
                onDelete()
            }
            moveAfterCopy = false
        } else moveAfterCopy = false
    }
    val selectedIds = selectedAlbums.mapTo(LinkedHashSet()) { it.bucketId }
    val allEssential = selectedIds.isNotEmpty() && selectedIds.all { it in essentialAlbumIds }
    Row(Modifier.fillMaxWidth().height(82.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
        BottomAction(Icons.Outlined.ContentCopy, "Copy") { moveAfterCopy = false; folderPicker.launch(null) }
        BottomAction(Icons.Outlined.Collections, "Move") { moveAfterCopy = true; folderPicker.launch(null) }
        BottomAction(Icons.Outlined.Share, "Share", onShare)
        Box {
            BottomAction(Icons.Outlined.MoreVert, "More") { more = true }
            DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                DropdownMenuItem(text = { Text(if (allEssential) "Remove from Essential albums" else "Add to Essential albums") }, onClick = {
                    more = false
                    onEssentialChanged(essentialAlbumIds.toMutableSet().apply { if (allEssential) removeAll(selectedIds) else addAll(selectedIds) })
                    Toast.makeText(context, if (allEssential) "Removed from Essential albums" else "Added to Essential albums", Toast.LENGTH_SHORT).show()
                })
                DropdownMenuItem(text = { Text("Copy selected albums to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { more = false; onSecure(false) })
                DropdownMenuItem(text = { Text("Move selected albums to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { more = false; onSecure(true) })
                DropdownMenuItem(text = { Text("Finish selection") }, onClick = { more = false; onDone() })
            }
        }
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
    selectedMedia: List<GalleryMedia>,
    onShare: () -> Unit,
    onSecure: (Boolean) -> Unit,
    favouriteLabel: String,
    onFavourite: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var createMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var tagDialog by remember { mutableStateOf(false) }
    var tag by remember { mutableStateOf("") }
    var moveAfterCopy by remember { mutableStateOf(false) }
    val folderPicker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri != null) scope.launch {
            val copied = withContext(Dispatchers.IO) { copyMediaToTree(context, selectedMedia, treeUri) }
            Toast.makeText(context, if (copied == selectedMedia.size) "Copied $copied items" else "Copied $copied of ${selectedMedia.size} items", Toast.LENGTH_LONG).show()
            if (moveAfterCopy && copied == selectedMedia.size) onDelete()
            moveAfterCopy = false
        } else moveAfterCopy = false
    }
    Row(Modifier.fillMaxWidth().height(82.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
        BottomAction(Icons.Outlined.Add, "Create", { createMenu = true })
        BottomAction(Icons.Outlined.Share, "Share", onShare)
        BottomAction(Icons.Outlined.Delete, "Delete", onDelete)
        Box {
            BottomAction(Icons.Outlined.MoreVert, "More", { moreMenu = true })
            DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                DropdownMenuItem(text = { Text("Copy to clipboard") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { moreMenu = false; copyUrisToClipboard(context, selectedMedia) })
                DropdownMenuItem(text = { Text("Copy to folder") }, onClick = { moreMenu = false; moveAfterCopy = false; folderPicker.launch(null) })
                DropdownMenuItem(text = { Text("Move to folder") }, onClick = { moreMenu = false; moveAfterCopy = true; folderPicker.launch(null) })
                DropdownMenuItem(text = { Text("Add to shared album") }, onClick = { moreMenu = false; (context as? Activity)?.let { shareUris(it, selectedMedia.map { item -> item.uri }, "*/*") } })
                DropdownMenuItem(text = { Text(if (favouriteLabel == "Unfavourite") "Remove from favourites" else "Add to favourites") }, leadingIcon = { Icon(Icons.Outlined.Favorite, null) }, onClick = { moreMenu = false; onFavourite() })
                DropdownMenuItem(text = { Text("Add tag") }, onClick = { moreMenu = false; tagDialog = true })
                if (selectedMedia.size == 1) DropdownMenuItem(text = { Text("Open in editor") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { moreMenu = false; launchFirstInEditor(context, selectedMedia) })
                if (selectedMedia.size == 1 && selectedMedia.first().kind == MediaKind.IMAGE) DropdownMenuItem(text = { Text("Set as wallpaper") }, onClick = {
                    moreMenu = false
                    setAsWallpaper(context, selectedMedia.first())
                })
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Copy to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { moreMenu = false; onSecure(false) })
                DropdownMenuItem(text = { Text("Move to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { moreMenu = false; onSecure(true) })
            }
        }
    }
    if (createMenu) AlertDialog(
        onDismissRequest = { createMenu = false },
        title = { Text("Create from selected items") },
        text = { Column {
            listOf("GIF", "Collage", "Movie").forEach { type ->
                TextButton(onClick = { createMenu = false; openCreativeChooser(context, selectedMedia, type) }, modifier = Modifier.fillMaxWidth()) { Text(type) }
            }
        } },
        confirmButton = { TextButton(onClick = { createMenu = false }) { Text("Cancel") } },
    )
    if (tagDialog) AlertDialog(
        onDismissRequest = { tagDialog = false },
        title = { Text("Add tag") },
        text = { OutlinedTextField(tag, { tag = it.take(40) }, label = { Text("Tag") }, singleLine = true) },
        dismissButton = { TextButton(onClick = { tagDialog = false }) { Text("Cancel") } },
        confirmButton = { TextButton(enabled = tag.isNotBlank(), onClick = {
            val prefs = context.getSharedPreferences("gallery-tags", 0)
            selectedMedia.forEach { item -> prefs.edit().putString(item.id.toString(), tag.trim()).apply() }
            Toast.makeText(context, "Tagged ${selectedMedia.size} items", Toast.LENGTH_SHORT).show()
            tag = ""; tagDialog = false
        }) { Text("Add") } },
    )
}

@Composable
private fun BottomAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) = BottomAction(icon, label, VaultPrimary, onClick)

@Composable
private fun BottomAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(Modifier.combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = tint); Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}

@Composable
private fun GallerySettings(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    var fullScreen by remember { mutableStateOf(preferences.getBoolean("full_screen_scroll", false)) }
    var externalPlayer by remember { mutableStateOf(preferences.getBoolean("external_player", false)) }
    var essential by remember { mutableStateOf(preferences.getBoolean("essential", true)) }
    var stories by remember { mutableStateOf(preferences.getBoolean("auto_stories", true)) }
    var mergeAlbums by remember { mutableStateOf(preferences.getBoolean("merge_albums", true)) }
    var secureLauncher by remember { mutableStateOf(isSecureLauncherVisible(context)) }
    var dialog by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("Gallery settings", onBack)
        androidx.compose.foundation.rememberScrollState().let { scroll ->
            Column(Modifier.fillMaxSize().padding(horizontal = 10.dp).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsHeader("Viewing")
                SettingsGroup {
                    SettingSwitch("Full screen scrolling", "Hide Android's system bars. Swipe from an edge to show them temporarily.", fullScreen) {
                        fullScreen = it
                        preferences.edit().putBoolean("full_screen_scroll", it).apply()
                        (context as? MainGalleryActivity)?.applyFullScreenPreference(it)
                    }
                    SettingSwitch("Open videos in external player", "When enabled, tapping a video opens your chosen Android video app instead of the built-in player.", externalPlayer) { externalPlayer = it; preferences.edit().putBoolean("external_player", it).apply() }
                }
                SettingsHeader("Stories")
                SettingsGroup {
                    SettingSwitch("Auto create stories", stories) { stories = it; preferences.edit().putBoolean("auto_stories", it).apply() }
                }
                SettingsHeader("Albums")
                SettingsGroup {
                    SettingSwitch("Select essential albums", "Show only the albums you select on the Albums tab instead of showing them all.", essential) { essential = it; preferences.edit().putBoolean("essential", it).apply() }
                    SettingSwitch("Merge albums", "Albums with the same name will be shown as a single album.", mergeAlbums) { mergeAlbums = it; preferences.edit().putBoolean("merge_albums", it).apply() }
                }
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
                    SettingLink("Secure Gallery settings") { (context as? Activity)?.let(::launchSecureGallery) }
                }
                SettingsGroup { SettingLink("About Gallery") { dialog = "Vault Gallery ${BuildConfig.VERSION_NAME}\nA local-first Pixel gallery with slide selection, system trash, favourites, search, stories, native video playback, and encrypted biometric Secure Gallery." } }
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
    onSecure: (Boolean) -> Unit,
    onFavourite: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var deletePrompt by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var landscape by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { (context as? Activity)?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED } }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (media.kind == MediaKind.VIDEO) PublicVideo(media.uri) else ZoomableImage(media.uri, media.name)
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_CAST_SETTINGS)) }
                    .onFailure { Toast.makeText(context, "Wireless display settings are unavailable", Toast.LENGTH_SHORT).show() }
            }) { Icon(Icons.Outlined.Slideshow, "Smart View", tint = Color.White) }
            IconButton(onClick = {
                landscape = !landscape
                (context as? Activity)?.requestedOrientation = if (landscape) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }) { Icon(Icons.Outlined.Restore, "Rotate", tint = Color.White) }
            Box {
                IconButton(onClick = { moreMenu = true }) { Icon(Icons.Outlined.MoreVert, "More options", tint = Color.White) }
                DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                    DropdownMenuItem(text = { Text("Details") }, leadingIcon = { Icon(Icons.Outlined.Info, null) }, onClick = { moreMenu = false; details = true })
                    DropdownMenuItem(text = { Text("Copy to clipboard") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = {
                        moreMenu = false
                        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard.setPrimaryClip(android.content.ClipData.newUri(context.contentResolver, media.name, media.uri))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    })
                    if (media.kind == MediaKind.VIDEO) DropdownMenuItem(text = { Text("Open in Video player") }, onClick = {
                        moreMenu = false; openExternalVideo(context, media)
                    })
                    DropdownMenuItem(text = { Text("Set as wallpaper") }, onClick = {
                        moreMenu = false; setAsWallpaper(context, media)
                    })
                    DropdownMenuItem(text = { Text("Copy to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { moreMenu = false; onSecure(false) })
                    DropdownMenuItem(text = { Text("Move to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { moreMenu = false; onSecure(true) })
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().background(Color(0xCC111113)).padding(10.dp), horizontalArrangement = Arrangement.SpaceAround) {
            BottomAction(Icons.Outlined.Favorite, if (media.isFavourite) "Unfavourite" else "Favourite", Color.White, onFavourite)
            BottomAction(Icons.Outlined.Edit, "Edit", Color.White, {
                runCatching { context.startActivity(Intent(Intent.ACTION_EDIT).setDataAndType(media.uri, media.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                    .onFailure { Toast.makeText(context, "No compatible editor is installed", Toast.LENGTH_SHORT).show() }
            })
            BottomAction(Icons.Outlined.Share, "Share", Color.White, onShare)
            BottomAction(Icons.Outlined.Delete, "Delete", Color.White, { deletePrompt = true })
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
            ) { items(media, key = { it.id }) { item -> MediaTile(item, false, { selected = it }, null) } }
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
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(true) }
    var muted by remember { mutableStateOf(false) }
    LaunchedEffect(player) {
        while (true) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it > 0 } ?: 0L
            playing = player.isPlaying
            delay(250)
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player; useController = false } },
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            Modifier.align(Alignment.BottomCenter).padding(start = 18.dp, end = 18.dp, bottom = 112.dp)
                .clip(RoundedCornerShape(24.dp)).background(Color(0xB5222226)).padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Slider(
                value = currentPosition.coerceIn(0L, duration.coerceAtLeast(1L)).toFloat(),
                onValueChange = { value -> currentPosition = value.toLong(); player.seekTo(currentPosition) },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth().height(26.dp),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (player.isPlaying) player.pause() else {
                        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                        player.play()
                    }
                    playing = player.isPlaying
                }) {
                    Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (playing) "Pause" else "Play", tint = Color.White)
                }
                Text("${GalleryLogic.durationLabel(currentPosition)} / ${GalleryLogic.durationLabel(duration)}", color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { muted = !muted; player.volume = if (muted) 0f else 1f }) {
                    Icon(if (muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp, if (muted) "Unmute" else "Mute", tint = Color.White)
                }
            }
        }
    }
}

private fun shareUris(activity: Activity, uris: List<Uri>, mime: String) {
    if (uris.isEmpty()) return
    val intent = if (uris.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
    else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    intent.type = mime.ifBlank { "*/*" }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    activity.startActivity(Intent.createChooser(intent, "Share media"))
}

private fun openExternalVideo(context: android.content.Context, media: GalleryMedia): Boolean = runCatching {
    context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(media.uri, media.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    true
}.getOrElse {
    Toast.makeText(context, "No external video player is installed; using Gallery instead", Toast.LENGTH_SHORT).show()
    false
}

private fun setAsWallpaper(context: android.content.Context, media: GalleryMedia) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_ATTACH_DATA).setDataAndType(media.uri, media.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra("mimeType", media.mimeType))
    }.onFailure { Toast.makeText(context, "No compatible wallpaper app is installed", Toast.LENGTH_SHORT).show() }
}

private fun copyUrisToClipboard(context: android.content.Context, media: List<GalleryMedia>) {
    if (media.isEmpty()) return
    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
    val clip = android.content.ClipData.newUri(context.contentResolver, media.first().name, media.first().uri)
    media.drop(1).forEach { clip.addItem(android.content.ClipData.Item(it.uri)) }
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied ${media.size} item${if (media.size == 1) "" else "s"} to clipboard", Toast.LENGTH_SHORT).show()
}

private fun copyMediaToTree(context: android.content.Context, media: List<GalleryMedia>, treeUri: Uri): Int {
    val directory = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri) ?: return 0
    var copied = 0
    media.forEach { item ->
        runCatching {
            val target = directory.createFile(item.mimeType.ifBlank { "application/octet-stream" }, item.name) ?: error("Could not create file")
            context.contentResolver.openInputStream(item.uri).use { input ->
                requireNotNull(input)
                context.contentResolver.openOutputStream(target.uri, "w").use { output -> requireNotNull(output); input.copyTo(output) }
            }
        }.onSuccess { copied++ }
    }
    return copied
}

private fun copyUrisToTree(context: android.content.Context, uris: List<Uri>, treeUri: Uri): Int {
    val directory = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri) ?: return 0
    var copied = 0
    uris.forEachIndexed { index, uri ->
        runCatching {
            var name = "Media ${index + 1}"
            var mime = context.contentResolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) name = cursor.getString(0).orEmpty().ifBlank { name }
            }
            val target = directory.createFile(mime, name) ?: error("Could not create file")
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input)
                context.contentResolver.openOutputStream(target.uri, "w").use { output -> requireNotNull(output); input.copyTo(output) }
            }
        }.onSuccess { copied++ }
    }
    return copied
}

private fun launchFirstInEditor(context: android.content.Context, media: List<GalleryMedia>) {
    val item = media.firstOrNull() ?: return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_EDIT).setDataAndType(item.uri, item.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }.onFailure { Toast.makeText(context, "No compatible metadata editor is installed", Toast.LENGTH_SHORT).show() }
}

private fun openCreativeChooser(context: android.content.Context, media: List<GalleryMedia>, type: String) {
    if (media.isEmpty()) return
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        this.type = "*/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(media.map { it.uri }))
        putExtra(Intent.EXTRA_TITLE, type)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Create $type with")) }
        .onFailure { Toast.makeText(context, "No compatible creator is installed", Toast.LENGTH_SHORT).show() }
}

private fun pinCollectionShortcut(context: android.content.Context, title: String) {
    val manager = context.getSystemService(android.content.pm.ShortcutManager::class.java)
    if (manager?.isRequestPinShortcutSupported != true) {
        Toast.makeText(context, "Your launcher does not support pinned shortcuts", Toast.LENGTH_SHORT).show()
        return
    }
    val shortcut = android.content.pm.ShortcutInfo.Builder(context, "collection-${title.lowercase().replace(' ', '-')}")
        .setShortLabel(title)
        .setLongLabel("Open $title in Gallery")
        .setIcon(android.graphics.drawable.Icon.createWithResource(context, com.danyal.vaultgallery.R.drawable.ic_gallery))
        .setIntent(Intent(context, MainGalleryActivity::class.java).setAction(Intent.ACTION_VIEW).putExtra("collection", title))
        .build()
    manager.requestPinShortcut(shortcut, null)
}

private fun launchSecureImport(activity: Activity, uris: ArrayList<Uri>, moveAfterImport: Boolean) {
    activity.startActivity(Intent(activity, SecureGalleryActivity::class.java)
        .putParcelableArrayListExtra("import_uris", uris)
        .putExtra("move_after_import", moveAfterImport)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
}

private fun launchSecureGallery(activity: Activity) {
    activity.startActivity(Intent(activity, SecureGalleryActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
}
