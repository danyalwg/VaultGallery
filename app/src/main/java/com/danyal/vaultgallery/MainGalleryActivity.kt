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
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.danyal.vaultgallery.ui.VaultOrange
import com.danyal.vaultgallery.ui.VaultPrimary
import com.danyal.vaultgallery.ui.VaultRaised
import com.danyal.vaultgallery.ui.VaultSecondary
import com.danyal.vaultgallery.ui.VaultSurface
import com.danyal.vaultgallery.ui.VaultTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainGalleryActivity : ComponentActivity() {
    private val galleryViewModel by viewModels<GalleryViewModel>()
    private val deleteLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
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
            val pending = MediaStore.createDeleteRequest(contentResolver, uris)
            deleteLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
        } else {
            Toast.makeText(this, "Delete approval requires Android 11 or later", Toast.LENGTH_LONG).show()
        }
    }
}

private enum class PublicTab { PICTURES, ALBUMS, STORIES, MENU }

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
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refresh() }

    LaunchedEffect(Unit) { viewModel.refresh() }
    BackHandler(viewer != null || settings || allAlbums || albumFilter != null || state.selectedIds.isNotEmpty()) {
        when {
            viewer != null -> viewer = null
            settings -> settings = false
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
            onDelete = { activity.deleteMedia(listOf(viewer!!.uri)); viewer = null },
        )
        return
    }
    if (settings) {
        GallerySettings(onBack = { settings = false })
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
    val selectedUris = state.media.filter { it.id in state.selectedIds }.map { it.uri }

    Scaffold(
        containerColor = VaultBackground,
        bottomBar = {
            Column(Modifier.background(VaultBackground).navigationBarsPadding()) {
                if (state.selectedIds.isNotEmpty()) {
                    SelectionBar(
                        count = state.selectedIds.size,
                        onShare = { shareUris(activity, selectedUris, "*/*") },
                        onSecure = { launchSecureImport(activity, ArrayList(selectedUris)) },
                        onDelete = { activity.deleteMedia(selectedUris) },
                        onCancel = viewModel::clearSelection,
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
                    onBack = { albumFilter = null },
                    onSearch = { searchOpen = !searchOpen },
                    onOpen = { if (state.selectedIds.isEmpty()) viewer = it else viewModel.toggleSelection(it.id) },
                    onSelect = viewModel::toggleSelection,
                )
                tab == PublicTab.PICTURES -> PicturesScreen(
                    media = filtered,
                    loading = state.loading,
                    selected = state.selectedIds,
                    query = query,
                    searchOpen = searchOpen,
                    onQuery = { query = it },
                    onSearch = { searchOpen = !searchOpen },
                    onOpen = { if (state.selectedIds.isEmpty()) viewer = it else viewModel.toggleSelection(it.id) },
                    onSelect = viewModel::toggleSelection,
                    onSelectAll = { viewModel.selectAll(filtered) },
                    onRefresh = viewModel::refresh,
                )
                tab == PublicTab.ALBUMS -> AlbumsScreen(
                    albums,
                    onViewAll = { allAlbums = true },
                    onOpen = { albumFilter = it.bucketId },
                    onSettings = { settings = true },
                )
                tab == PublicTab.STORIES -> StoriesScreen()
                else -> MenuScreen(
                    onVideos = { query = ""; kindFilter = MediaKind.VIDEO; favouriteOnly = false; tab = PublicTab.PICTURES },
                    onRecent = { kindFilter = null; favouriteOnly = false; tab = PublicTab.PICTURES },
                    onFavourites = { query = ""; kindFilter = null; favouriteOnly = true; tab = PublicTab.PICTURES },
                    onSettings = { settings = true },
                    onSecure = { activity.startActivity(Intent(activity, SecureGalleryActivity::class.java)) },
                )
            }
            if (BuildConfig.DEBUG) {
                Text(
                    "DEBUG",
                    color = VaultOrange,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 2.dp),
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
    extra: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(112.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (back != null) IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        if (title != null) Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
        extra?.invoke()
        if (onSearch != null) IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Search") }
    }
}

@Composable
private fun PicturesScreen(
    media: List<GalleryMedia>, loading: Boolean, selected: Set<Long>, query: String, searchOpen: Boolean,
    onQuery: (String) -> Unit, onSearch: () -> Unit, onOpen: (GalleryMedia) -> Unit,
    onSelect: (Long) -> Unit, onSelectAll: () -> Unit, onRefresh: () -> Unit,
) {
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
                extra = {
                    IconButton(onClick = onRefresh) { Icon(Icons.Outlined.GridView, "Refresh layout") }
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
        else TimelineGrid(media, selected, onOpen, onSelect)
    }
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
private fun TimelineGrid(media: List<GalleryMedia>, selected: Set<Long>, onOpen: (GalleryMedia) -> Unit, onSelect: (Long) -> Unit) {
    val format = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val grouped = remember(media) { media.groupBy { format.format(Date(it.dateTakenMs)) } }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalArrangement = Arrangement.spacedBy(1.5.dp),
        modifier = Modifier.fillMaxSize(),
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
            .combinedClickable(onClick = { onOpen(item) }, onLongClick = { onSelect(item.id) }),
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
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar(extra = { IconButton(onClick = onViewAll) { Icon(Icons.Outlined.Search, "View all albums") }; IconButton(onClick = onSettings) { Icon(Icons.Outlined.MoreVert, "Album settings") } })
        Card(
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
            Text("Essential albums", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onViewAll) { Text("View all") }
        }
        AlbumGrid(albums.take(12), onOpen)
    }
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
private fun MediaGridScreen(title: String, media: List<GalleryMedia>, selected: Set<Long>, onBack: () -> Unit, onSearch: () -> Unit, onOpen: (GalleryMedia) -> Unit, onSelect: (Long) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar(title, onBack, onSearch)
        if (media.isEmpty()) EmptyGallery() else LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
            verticalArrangement = Arrangement.spacedBy(1.5.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        ) { items(media, key = { it.id }) { MediaTile(it, it.id in selected, onOpen, onSelect) } }
    }
}

@Composable
private fun StoriesScreen() {
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 42.dp)) {
                Text("No stories", style = MaterialTheme.typography.headlineMedium, color = VaultSecondary)
                Spacer(Modifier.height(14.dp))
                Text("Your locally created story collections will appear here.", textAlign = TextAlign.Center, color = VaultSecondary)
            }
        }
    }
}

@Composable
private fun MenuScreen(onVideos: () -> Unit, onRecent: () -> Unit, onFavourites: () -> Unit, onSettings: () -> Unit, onSecure: () -> Unit) {
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
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MenuAction(Icons.Outlined.Settings, "Settings", onSettings)
                    MenuAction(Icons.Outlined.Lock, "Secure Gallery", onSecure)
                }
            }
        }
    }
}

@Composable
private fun MenuAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.width(110.dp).combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onClick).padding(vertical = 18.dp),
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
private fun SelectionBar(count: Int, onShare: () -> Unit, onSecure: () -> Unit, onDelete: () -> Unit, onCancel: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(82.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
        BottomAction(Icons.Outlined.Share, "Share", onShare)
        BottomAction(Icons.Outlined.Lock, "Secure", onSecure)
        BottomAction(Icons.Outlined.Delete, "Delete", onDelete)
        BottomAction(Icons.Outlined.MoreVert, "Cancel ($count)", onCancel)
    }
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
    var fullScroll by remember { mutableStateOf(preferences.getBoolean("full_scroll", false)) }
    var autoplay by remember { mutableStateOf(preferences.getBoolean("autoplay", true)) }
    var externalPlayer by remember { mutableStateOf(preferences.getBoolean("external_player", false)) }
    var stories by remember { mutableStateOf(preferences.getBoolean("stories", true)) }
    var notifications by remember { mutableStateOf(preferences.getBoolean("notifications", true)) }
    var essential by remember { mutableStateOf(preferences.getBoolean("essential", true)) }
    var merge by remember { mutableStateOf(preferences.getBoolean("merge", false)) }
    var dialog by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("Gallery settings", onBack)
        androidx.compose.foundation.rememberScrollState().let { scroll ->
            Column(Modifier.fillMaxSize().padding(horizontal = 10.dp).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsHeader("Viewing")
                SettingsGroup {
                    SettingSwitch("Full screen scrolling", fullScroll) { fullScroll = it; preferences.edit().putBoolean("full_scroll", it).apply() }
                    SettingSwitch("Auto play motion photos", autoplay) { autoplay = it; preferences.edit().putBoolean("autoplay", it).apply() }
                    SettingSwitch("Open in video player", externalPlayer) { externalPlayer = it; preferences.edit().putBoolean("external_player", it).apply() }
                }
                SettingsHeader("Stories")
                SettingsGroup {
                    SettingSwitch("Auto create stories", stories) { stories = it; preferences.edit().putBoolean("stories", it).apply() }
                    SettingSwitch("Notifications", notifications) { notifications = it; preferences.edit().putBoolean("notifications", it).apply() }
                }
                SettingsHeader("Albums")
                SettingsGroup {
                    SettingSwitch("Select essential albums", essential) { essential = it; preferences.edit().putBoolean("essential", it).apply() }
                    SettingSwitch("Merge albums", merge) { merge = it; preferences.edit().putBoolean("merge", it).apply() }
                }
                SettingsHeader("Privacy")
                SettingsGroup {
                    SettingLink("Privacy Policy") { dialog = "Vault Gallery processes media locally and does not upload it by default. Secure Gallery exports plaintext only after your explicit action." }
                    SettingLink("Permissions") {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
                    }
                }
                SettingsGroup { SettingLink("About Gallery") { dialog = "Vault Gallery 1.0\nLocal media browser and application-level encrypted gallery." } }
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
private fun SettingLink(title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(74.dp).combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onClick).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
    HorizontalDivider(color = Color(0xFF38383D), modifier = Modifier.padding(horizontal = 18.dp))
}

@Composable
private fun PublicViewer(media: GalleryMedia, onBack: () -> Unit, onShare: () -> Unit, onSecure: () -> Unit, onDelete: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (media.kind == MediaKind.VIDEO) PublicVideo(media.uri) else ZoomableImage(media.uri, media.name)
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
            Text(media.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().background(Color(0xCC111113)).padding(10.dp), horizontalArrangement = Arrangement.SpaceAround) {
            BottomAction(Icons.Outlined.Share, "Share", onShare)
            BottomAction(Icons.Outlined.Lock, "Secure", onSecure)
            BottomAction(Icons.Outlined.Delete, "Delete", onDelete)
        }
    }
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
