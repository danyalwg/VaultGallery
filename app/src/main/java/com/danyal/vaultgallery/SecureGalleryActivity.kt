@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.danyal.vaultgallery

import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import com.danyal.vaultgallery.security.SecureStoragePolicy
import com.danyal.vaultgallery.security.SecureUnlockPolicy
import com.danyal.vaultgallery.security.ApprovedPlayerStore
import com.danyal.vaultgallery.security.SecureMediaProvider
import com.danyal.vaultgallery.security.SecureAccessHistoryStore
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections as FilledCollections
import androidx.compose.material.icons.filled.Image as FilledImage
import androidx.compose.material.icons.filled.Menu as FilledMenu
import androidx.compose.material.icons.filled.Movie as FilledMovie
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.danyal.vaultgallery.security.SecureItem
import com.danyal.vaultgallery.security.SecureVault
import com.danyal.vaultgallery.core.GalleryLogic
import com.danyal.vaultgallery.data.GalleryAlbum
import com.danyal.vaultgallery.data.MediaStoreRepository
import com.danyal.vaultgallery.ui.VaultBackground
import com.danyal.vaultgallery.ui.VaultPrimary
import com.danyal.vaultgallery.ui.VaultRaised
import com.danyal.vaultgallery.ui.VaultSecondary
import com.danyal.vaultgallery.ui.VaultSecure
import com.danyal.vaultgallery.ui.VaultSurface
import com.danyal.vaultgallery.ui.VaultTheme
import com.danyal.vaultgallery.ui.LocalVaultMotion
import com.github.panpf.zoomimage.ZoomImage
import com.github.panpf.zoomimage.compose.rememberZoomState
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged

class SecureGalleryActivity : FragmentActivity() {
    private val secureViewModel by viewModels<SecureGalleryViewModel>()
    private var biometricInProgress = false
    private var biometricEnrollment = false
    private var persistentStorageGate = false
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) secureViewModel.onScreenOff()
        }
    }
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val persistentStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (SecureVault.hasPersistentStorageAccess(this)) recreate()
    }
    private val documentScannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val scan = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)
        val pages = scan?.pages?.map { it.imageUri }.orEmpty()
        scan?.pdf?.let { pdf ->
            persistScannedPdf(this, pdf.uri, pdf.pageCount) { saved ->
                saved.onFailure { Toast.makeText(this, it.message ?: "Scanned PDF could not be saved", Toast.LENGTH_LONG).show() }
            }
        }
        persistScannedPages(this, pages) { copied ->
            Toast.makeText(this, "$copied scanned page${if (copied == 1) "" else "s"} saved to Gallery", Toast.LENGTH_SHORT).show()
        }
    }

    private val biometricPrompt by lazy {
        BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    biometricInProgress = false
                    val cipher = result.cryptoObject?.cipher
                        ?: return secureViewModel.reportError("Android did not return the authenticated vault cipher")
                    if (biometricEnrollment) secureViewModel.completeBiometricEnrollment(cipher)
                    else secureViewModel.completeBiometricUnlock(cipher)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    biometricInProgress = false
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_CANCELED) {
                        secureViewModel.reportError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    secureViewModel.reportError("Fingerprint or face was not recognized")
                }
            },
        )
    }

    fun toggleViewerOrientation() {
        requestedOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    fun restoreViewerOrientation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    fun launchDocumentScanner() {
        val options = com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(30)
            .setResultFormats(
                com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
            )
            .setScannerMode(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        com.google.mlkit.vision.documentscanner.GmsDocumentScanning.getClient(options)
            .getStartScanIntent(this)
            .addOnSuccessListener { sender ->
                documentScannerLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(sender).build())
            }
            .addOnFailureListener { error -> Toast.makeText(this, error.message ?: "Document scanner is unavailable", Toast.LENGTH_LONG).show() }
    }

    fun applyViewerImmersive(enabled: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (enabled) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyScreenshotProtection()
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        if (!SecureVault.hasPersistentStorageAccess(this)) {
            persistentStorageGate = true
            setContent {
                VaultTheme {
                    Box(
                        Modifier.fillMaxSize().background(VaultBackground).padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Icon(Icons.Outlined.Lock, null, Modifier.size(64.dp), tint = VaultSecure)
                            Text("Keep Secure Gallery media after uninstall", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                            Text(
                                "Vault Gallery now keeps secure media in Documents/VaultGallery/SecureVault. Android will not delete that folder when the app is removed. Access is required before the vault can open or import anything.",
                                color = VaultSecondary,
                                textAlign = TextAlign.Center,
                            )
                            Button(onClick = ::openPersistentStorageSettings) { Text("Allow persistent storage") }
                            Text("The folder is hidden from media scanners. Your PIN still gates the gallery.", color = VaultSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            return
        }
        val explicitIncoming = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra("import_uris", Uri::class.java).orEmpty()
        } else {
            @Suppress("DEPRECATION") intent.getParcelableArrayListExtra<Uri>("import_uris").orEmpty()
        }
        val sharedIncoming = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(
                if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM),
                intent.getStringExtra(Intent.EXTRA_STREAM)?.let(Uri::parse),
            )
            Intent.ACTION_SEND_MULTIPLE -> if (Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty() else @Suppress("DEPRECATION") intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
            else -> emptyList()
        }
        val clippedIncoming = buildList {
            val clip = intent.clipData ?: return@buildList
            repeat(clip.itemCount) { index -> clip.getItemAt(index).uri?.let(::add) }
        }
        val allIncoming = (explicitIncoming + sharedIncoming + clippedIncoming).distinct()
        val incomingMove = intent.getBooleanExtra("move_after_import", false)
        val incomingSourceKind = runCatching {
            TransferSourceKind.valueOf(intent.getStringExtra("transfer_source_kind").orEmpty())
        }.getOrDefault(TransferSourceKind.MEDIA)
        val incomingSourceFolderCount = intent.getIntExtra("transfer_source_folder_count", 0).coerceAtLeast(0)
        setContent {
            VaultTheme {
                var pendingIncoming by remember { mutableStateOf(allIncoming) }
                Box(Modifier.fillMaxSize()) {
                    SecureGalleryApp(
                        viewModel = secureViewModel,
                        onShare = ::shareSecure,
                        onPlay = ::playSecureVideo,
                        biometricsAvailable = biometricsAvailable(),
                        onBiometricUnlock = { showBiometricPrompt(enrollment = false) },
                        onBiometricEnrollment = { showBiometricPrompt(enrollment = true) },
                        incomingCount = pendingIncoming.size,
                        incomingMove = incomingMove,
                        incomingSourceKind = incomingSourceKind,
                        incomingSourceFolderCount = incomingSourceFolderCount,
                        onIncomingDestination = { album ->
                            secureViewModel.queueImport(pendingIncoming, move = incomingMove, targetAlbum = album)
                            pendingIncoming = emptyList()
                        },
                        onCancelIncoming = { pendingIncoming = emptyList() },
                    )
                }
            }
        }
    }

    override fun onStop() {
        // Security lifecycle is deliberately independent from playback/audio-focus policy. A call,
        // system chooser or another full-screen activity must never keep the unlocked vault and its
        // item keys resident after Secure Gallery is no longer visible. BiometricPrompt is the only
        // transient surface allowed to cover this activity without starting the background timer.
        if (!persistentStorageGate && !biometricInProgress && !isChangingConfigurations) {
            secureViewModel.onBackgrounded()
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        applyScreenshotProtection()
        if (persistentStorageGate) {
            if (SecureVault.hasPersistentStorageAccess(this)) recreate()
            return
        }
        secureViewModel.onForegrounded()
    }

    fun applyScreenshotProtection(
        enabled: Boolean = getSharedPreferences("secure-gallery-settings", 0)
            .getBoolean("protect_screenshots", false),
    ) {
        if (enabled) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenOffReceiver) }
        super.onDestroy()
    }

    private fun openPersistentStorageSettings() {
        if (Build.VERSION.SDK_INT >= 30) {
            val appPage = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .setData(Uri.parse("package:$packageName"))
            runCatching { startActivity(appPage) }.getOrElse {
                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            persistentStoragePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun biometricsAvailable(): Boolean = BiometricManager.from(this).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG,
    ) == BiometricManager.BIOMETRIC_SUCCESS

    private fun showBiometricPrompt(enrollment: Boolean) {
        if (!biometricsAvailable()) {
            secureViewModel.reportError("Set up a fingerprint or strong face unlock in Android settings first")
            return
        }
        val cipher = if (enrollment) secureViewModel.prepareBiometricEnrollment() else secureViewModel.prepareBiometricUnlock()
        if (cipher == null) return
        biometricEnrollment = enrollment
        biometricInProgress = true
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (enrollment) "Enable biometric unlock" else "Unlock Secure Gallery")
            .setSubtitle(if (enrollment) "Confirm your fingerprint or face to protect the vault key" else "Use your fingerprint or face")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(if (enrollment) "Not now" else "Use vault PIN")
            .build()
        biometricPrompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    private fun shareSecure(item: SecureItem, file: java.io.File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.secure.share", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share decrypted copy"))
    }

    fun shareSecureFiles(files: List<Pair<SecureItem, java.io.File>>) {
        if (files.isEmpty()) return
        val uris = ArrayList(files.map { (_, file) -> FileProvider.getUriForFile(this, "$packageName.secure.share", file) })
        val mime = files.map { it.first.mimeType }.distinct().singleOrNull() ?: "*/*"
        val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = mime
            if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first()) else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            clipData = android.content.ClipData.newRawUri(files.first().first.name, uris.first()).also { clip ->
                uris.drop(1).forEach { clip.addItem(android.content.ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share decrypted copies"))
    }

    private fun playSecureVideo(item: SecureItem) {
        val uri = com.danyal.vaultgallery.security.SecureMediaProvider.uri(this, item)
        // The standalone player is another activity in this app. Keep the already-authenticated
        // session alive while it owns playback; returning to Secure Gallery resumes normal timeout.
        secureViewModel.authorizeExternalAccess(60 * 60_000L)
        startActivity(
            Intent(this, VaultVideoPlayerActivity::class.java)
                .setDataAndType(uri, item.mimeType)
                .putExtra(Intent.EXTRA_TITLE, item.name)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }

}

private enum class SecureTab { PICTURES, ALBUMS, STORIES, MENU }

@Composable
private fun SecureGalleryApp(
    viewModel: SecureGalleryViewModel,
    onShare: (SecureItem, java.io.File) -> Unit,
    onPlay: (SecureItem) -> Unit,
    biometricsAvailable: Boolean,
    onBiometricUnlock: () -> Unit,
    onBiometricEnrollment: () -> Unit,
    incomingCount: Int,
    incomingMove: Boolean,
    incomingSourceKind: TransferSourceKind,
    incomingSourceFolderCount: Int,
    onIncomingDestination: (String) -> Unit,
    onCancelIncoming: () -> Unit,
) {
    val motion = LocalVaultMotion.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val securePreferences = remember { context.getSharedPreferences("secure-gallery-settings", 0) }
    val gridColumns = securePreferences.getInt("grid_columns", 4).coerceIn(3, 12)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val publicAlbums by produceState<List<GalleryAlbum>>(initialValue = emptyList(), state.unlocked) {
        value = if (state.unlocked) {
            val repository = MediaStoreRepository(context)
            repository.albums(repository.loadMedia(), mergeMatchingNames = true)
        } else emptyList()
    }
    var tab by remember { mutableStateOf(SecureTab.PICTURES) }
    var menuOpen by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<SecureItem?>(null) }
    var viewerItems by remember { mutableStateOf<List<SecureItem>>(emptyList()) }
    var editorItem by remember { mutableStateOf<SecureItem?>(null) }
    var editorLaunchMode by remember { mutableStateOf(EditorLaunchMode.STANDARD) }
    var secureCreativeRequest by remember { mutableStateOf<Pair<CreationType, List<SecureItem>>?>(null) }
    var settings by remember { mutableStateOf(false) }
    var trashOpen by remember { mutableStateOf(false) }
    var secureAlbum by remember { mutableStateOf<String?>(null) }
    var secureCollection by remember { mutableStateOf<String?>(null) }
    var secureCleanReason by remember { mutableStateOf<QualityReviewReason?>(null) }
    var secureSimilarIds by remember { mutableStateOf<Set<String>?>(null) }
    val verifiedSecureDuplicates by produceState<List<SecureItem>?>(initialValue = null, state.unlocked, state.items, secureCollection) {
        value = if (state.unlocked && secureCollection == "Clean out") viewModel.exactDuplicateItems(state.items) else null
    }
    val secureQualityCollections by produceState<Map<QualityReviewReason, List<SecureItem>>?>(initialValue = null, state.unlocked, state.items, secureCollection) {
        value = if (state.unlocked && secureCollection == "Clean out") viewModel.qualityReviewItems(state.items) else null
    }
    var resumeStateApplied by remember { mutableStateOf(false) }
    var automaticBiometricEnrollmentAttempted by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedAlbums by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pickedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val sharedGalleryPreferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    fun openSecureItem(item: SecureItem, visibleItems: List<SecureItem>) {
        if (item.isVideo && sharedGalleryPreferences.getBoolean("external_player", false)) {
            onPlay(item)
        } else {
            viewerItems = visibleItems
            viewer = item
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(250)) { uris ->
        uris.forEach { uri ->
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        pickedUris = uris
    }
    LaunchedEffect(state.unlocked) {
        if (!state.unlocked) {
            (context as? SecureGalleryActivity)?.restoreViewerOrientation()
            viewer = null
            menuOpen = false
            viewerItems = emptyList()
            editorItem = null
            settings = false
            trashOpen = false
            secureAlbum = null
            secureCollection = null
            secureCleanReason = null
            secureSimilarIds = null
            selectedIds = emptySet()
            selectedAlbums = emptySet()
            tab = SecureTab.PICTURES
            resumeStateApplied = false
            automaticBiometricEnrollmentAttempted = false
        }
    }
    LaunchedEffect(state.unlocked, state.biometricEnabled, biometricsAvailable) {
        if (
            state.unlocked &&
            biometricsAvailable &&
            !state.biometricEnabled &&
            !automaticBiometricEnrollmentAttempted
        ) {
            automaticBiometricEnrollmentAttempted = true
            delay(300)
            onBiometricEnrollment()
        }
    }
    LaunchedEffect(state.unlocked, state.items) {
        if (state.unlocked && !resumeStateApplied) {
            tab = runCatching {
                SecureTab.valueOf(securePreferences.getString("resume_tab", SecureTab.PICTURES.name).orEmpty())
            }.getOrDefault(SecureTab.PICTURES).let { if (it == SecureTab.MENU) SecureTab.PICTURES else it }
            val savedAlbum = securePreferences.getString("resume_album", null)
            val savedCollection = securePreferences.getString("resume_collection", null)
            secureAlbum = savedAlbum?.takeIf { album -> state.items.any { it.albumName == album } }
            secureCollection = savedCollection?.takeIf { it in setOf("Videos", "Favourites", "Recent", "Clean out", "Locations", "Shared albums") }
            if (secureAlbum != null || secureCollection != null) tab = SecureTab.PICTURES
            resumeStateApplied = true
        }
    }
    LaunchedEffect(state.unlocked, resumeStateApplied, tab, secureAlbum, secureCollection) {
        if (state.unlocked && resumeStateApplied) {
            securePreferences.edit()
                .putString("resume_tab", tab.name)
                .putString("resume_album", secureAlbum)
                .putString("resume_collection", secureCollection)
                .apply()
        }
    }
    LaunchedEffect(secureCollection) {
        if (secureCollection != "Similar") secureSimilarIds = null
    }
    LaunchedEffect(state.items, viewer?.id) {
        viewer?.let { current -> state.items.firstOrNull { it.id == current.id }?.let { refreshed -> viewer = refreshed } }
        if (viewerItems.isNotEmpty()) {
            val refreshedById = state.items.associateBy(SecureItem::id)
            viewerItems = viewerItems.mapNotNull { refreshedById[it.id] }
        }
    }
    LaunchedEffect(state.notice) {
        state.notice?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.consumeNotice()
        }
    }
    BackHandler(state.unlocked && (menuOpen || editorItem != null || viewer != null || settings || trashOpen || secureAlbum != null || secureCollection != null || secureSimilarIds != null || selectedIds.isNotEmpty() || selectedAlbums.isNotEmpty())) {
        when {
            menuOpen -> menuOpen = false
            editorItem != null -> editorItem = null
            viewer != null -> { (context as? SecureGalleryActivity)?.restoreViewerOrientation(); viewer = null }
            settings -> settings = false
            trashOpen -> trashOpen = false
            selectedIds.isNotEmpty() -> selectedIds = emptySet()
            selectedAlbums.isNotEmpty() -> selectedAlbums = emptySet()
            secureAlbum != null -> secureAlbum = null
            secureCollection != null || secureSimilarIds != null -> { secureCollection = null; secureSimilarIds = null }
        }
    }

    if (!state.unlocked) {
        SecureAuthScreen(state, viewModel::setup, viewModel::unlock, biometricsAvailable, onBiometricUnlock)
        return
    }
    secureCreativeRequest?.let { request ->
        SecureCreativeStudio(
            type = request.first,
            candidates = request.second,
            viewModel = viewModel,
            onBack = { secureCreativeRequest = null },
            onCreate = { selected, options ->
                secureCreativeRequest = null
                selectedIds = emptySet()
                viewModel.create(selected, request.first, options)
            },
        )
        return
    }
    if (editorItem != null) {
        if (editorItem!!.isVideo) {
            SecureVideoEditor(editorItem!!, viewModel, onBack = { editorItem = null }, onSaved = { editorItem = null; viewer = null })
        } else {
            SecurePhotoEditor(editorItem!!, viewModel, editorLaunchMode, onBack = { editorItem = null }, onSaved = { editorItem = null; viewer = null })
        }
        return
    }
    if (viewer != null) {
        val visibleItems = viewerItems.ifEmpty {
            (if (secureSimilarIds != null) {
                state.items.filter { it.id in secureSimilarIds.orEmpty() }
            } else if (secureCollection == "Clean out") {
                secureCleanReason?.let { secureQualityCollections?.get(it).orEmpty() } ?: verifiedSecureDuplicates.orEmpty()
            } else secureCollectionItems(state.items, secureCollection))
                .filter { secureAlbum == null || it.albumName == secureAlbum }
        }
        SecureViewer(
            item = viewer!!,
            items = visibleItems,
            viewModel = viewModel,
            publicAlbums = publicAlbums,
            onBack = { (context as? SecureGalleryActivity)?.restoreViewerOrientation(); viewer = null },
            onNavigate = { viewer = it },
            onShare = onShare,
            onPlay = onPlay,
            onEdit = { editorLaunchMode = EditorLaunchMode.STANDARD; editorItem = viewer },
            onAiAssist = { editorLaunchMode = EditorLaunchMode.AI_ASSIST; editorItem = viewer },
            onScanDocument = { editorLaunchMode = EditorLaunchMode.DOCUMENT_SCAN; editorItem = viewer },
            onFindSimilar = {
                val source = viewer ?: return@SecureViewer
                scope.launch {
                    Toast.makeText(context, "Finding visually similar secure photos…", Toast.LENGTH_SHORT).show()
                    val matches = viewModel.visuallySimilarItems(source, state.items)
                    if (matches.isEmpty()) {
                        Toast.makeText(context, "No close visual matches found", Toast.LENGTH_SHORT).show()
                    } else {
                        (context as? SecureGalleryActivity)?.restoreViewerOrientation()
                        viewer = null
                        secureAlbum = null
                        secureCollection = "Similar"
                        secureSimilarIds = (listOf(source) + matches).mapTo(LinkedHashSet(), SecureItem::id)
                    }
                }
            },
            onCreateGif = { secureCreativeRequest = CreationType.GIF to listOf(viewer!!) },
            onFavourite = { viewModel.setFavourite(viewer!!, !viewer!!.isFavourite) },
            onCopyOut = { album -> viewModel.export(listOf(viewer!!), false, album) },
            onMoveOut = { album -> viewModel.export(listOf(viewer!!), true, album); (context as? SecureGalleryActivity)?.restoreViewerOrientation(); viewer = null },
            onDelete = { viewModel.delete(viewer!!); (context as? SecureGalleryActivity)?.restoreViewerOrientation(); viewer = null },
        )
        return
    }
    if (settings) {
        SecureSettings(state, viewModel, biometricsAvailable, onBiometricEnrollment, onBack = { settings = false })
        return
    }
    if (trashOpen) {
        SecureTrashScreen(state, viewModel, onBack = { trashOpen = false })
        return
    }

    Scaffold(
        containerColor = VaultBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedContent(
                targetState = when { selectedIds.isNotEmpty() -> "media"; selectedAlbums.isNotEmpty() -> "albums"; else -> "navigation" },
                transitionSpec = { fadeIn(tween(motion.duration(motion.fastMs))) togetherWith fadeOut(tween(motion.duration(motion.fastMs))) },
                label = "secure bottom controls",
            ) { bottomMode ->
            if (bottomMode == "media") {
                val selectedItems = state.items.filter { it.id in selectedIds }
                SecureSelectionBar(
                    selectedItems = selectedItems,
                    existingAlbums = state.items.groupBy { it.albumName },
                    publicAlbums = publicAlbums,
                    onCopyOut = { album -> viewModel.export(selectedItems, false, album); selectedIds = emptySet() },
                    onMoveOut = { album -> viewModel.export(selectedItems, true, album); selectedIds = emptySet() },
                    onPlaceInAlbum = { name, copy -> viewModel.placeInAlbum(selectedItems, name, copy); selectedIds = emptySet() },
                    onCreate = { type ->
                        secureCreativeRequest = type to selectedItems
                    },
                    onShare = {
                        scope.launch {
                            val files = viewModel.shareFiles(selectedItems)
                            (context as? SecureGalleryActivity)?.shareSecureFiles(files)
                        }
                    },
                    onFavourite = { favourite -> viewModel.setFavourite(selectedItems, favourite); selectedIds = emptySet() },
                    onTag = { tag -> viewModel.updateMetadata(selectedItems, tag = tag); selectedIds = emptySet() },
                    onDate = { timestamp -> viewModel.updateMetadata(selectedItems, dateTakenMs = timestamp); selectedIds = emptySet() },
                    onLocation = { latitude, longitude -> viewModel.updateMetadata(selectedItems, latitude = latitude, longitude = longitude, replaceLocation = true); selectedIds = emptySet() },
                    onStoragePolicy = { policy -> viewModel.convertStoragePolicy(selectedItems, policy); selectedIds = emptySet() },
                    onDelete = { viewModel.delete(selectedItems); selectedIds = emptySet() },
                    onCancel = { selectedIds = emptySet() },
                )
            } else if (bottomMode == "albums") {
                val albumItems = state.items.filter { it.albumName in selectedAlbums }
                SecureAlbumSelectionBar(
                    selectedAlbums = selectedAlbums,
                    selectedItems = albumItems,
                    secureAlbums = state.items.groupBy { it.albumName },
                    publicAlbums = publicAlbums,
                    onCopyOut = { album -> viewModel.export(albumItems, false, album); selectedAlbums = emptySet() },
                    onMoveOut = { album -> viewModel.export(albumItems, true, album); selectedAlbums = emptySet() },
                    onRename = { name -> viewModel.placeInAlbum(albumItems, name, false); selectedAlbums = emptySet() },
                    onPlaceInAlbum = { name, copy -> viewModel.placeInAlbum(albumItems, name, copy); selectedAlbums = emptySet() },
                    onStoragePolicy = { policy -> viewModel.convertStoragePolicy(albumItems, policy); selectedAlbums = emptySet() },
                    onDelete = { viewModel.delete(albumItems); selectedAlbums = emptySet() },
                    onCancel = { selectedAlbums = emptySet() },
                )
            } else SecureBottomNavigation(if (menuOpen) SecureTab.MENU else tab) { next ->
                if (next == SecureTab.MENU) menuOpen = true
                else {
                    menuOpen = false
                    tab = next
                    selectedIds = emptySet()
                    selectedAlbums = emptySet()
                    if (next != SecureTab.PICTURES) { secureAlbum = null; secureCollection = null }
                }
            }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(VaultBackground)) {
            val destination = when (tab) {
                SecureTab.PICTURES -> "pictures-${secureAlbum ?: secureCollection ?: "root"}"
                else -> tab.name
            }
            AnimatedContent(
                targetState = destination,
                transitionSpec = { fadeIn(tween(motion.duration(motion.fastMs))) togetherWith fadeOut(tween(motion.duration(motion.fastMs))) },
                label = "secure navigation",
                modifier = Modifier.fillMaxSize(),
            ) { shownDestination ->
            when {
                shownDestination.startsWith("pictures-") && secureCollection == "Locations" -> SecureLocationsMapScreen(
                    media = state.items.filter { !it.isTrashed && it.latitude != null && it.longitude != null },
                    onBack = { secureCollection = null },
                    onOpen = { item, displayed -> openSecureItem(item, displayed) },
                )
                shownDestination.startsWith("pictures-") -> SecurePictures(
                    state = state,
                    gridColumns = gridColumns,
                    items = (if (secureSimilarIds != null) {
                        state.items.filter { it.id in secureSimilarIds.orEmpty() }
                    } else if (secureCollection == "Clean out") {
                        secureCleanReason?.let { secureQualityCollections?.get(it).orEmpty() } ?: verifiedSecureDuplicates.orEmpty()
                    } else secureCollectionItems(state.items, secureCollection))
                        .filter { secureAlbum == null || it.albumName == secureAlbum },
                    title = secureAlbum ?: secureCollection ?: "Secure Gallery",
                    onBack = if (secureAlbum == null && secureCollection == null) null else ({ secureAlbum = null; secureCollection = null }),
                    onImport = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                    selected = selectedIds,
                    onSelect = { id -> selectedIds = selectedIds.toMutableSet().apply { if (!add(id)) remove(id) } },
                    onSelectionSet = { selectedIds = it },
                    onSelectAll = { visible -> selectedIds = visible.mapTo(LinkedHashSet()) { it.id } },
                    onCreate = { type, visible ->
                        secureCreativeRequest = type to visible
                    },
                    cleanOutReason = secureCleanReason,
                    cleanOutCounts = if (secureCollection == "Clean out") {
                        QualityReviewReason.entries.filterNot { it == QualityReviewReason.BURST }.associateWith { secureQualityCollections?.get(it)?.size ?: 0 }
                    } else null,
                    duplicateCount = verifiedSecureDuplicates?.size ?: 0,
                    onCleanOutReasonChanged = { secureCleanReason = it },
                    onOpen = { item, displayed ->
                        if (selectedIds.isEmpty()) {
                            openSecureItem(item, displayed)
                        } else selectedIds = selectedIds.toMutableSet().apply { if (!add(item.id)) remove(item.id) }
                    },
                )
                shownDestination == SecureTab.ALBUMS.name -> SecureAlbums(
                    items = state.items,
                    publicAlbums = publicAlbums,
                    selected = selectedAlbums,
                    onToggleSelection = { album -> selectedAlbums = selectedAlbums.toMutableSet().apply { if (!add(album)) remove(album) } },
                    onOpen = { album -> secureAlbum = album; tab = SecureTab.PICTURES; selectedAlbums = emptySet() },
                    onCopyOut = { source, target -> viewModel.export(state.items.filter { it.albumName == source }, false, target) },
                    onMoveOut = { source, target -> viewModel.export(state.items.filter { it.albumName == source }, true, target) },
                )
                shownDestination == SecureTab.STORIES.name -> SecureStories(state.items) { story -> viewerItems = story; viewer = story.firstOrNull() }
                else -> SecurePictures(
                    state = state, gridColumns = gridColumns, items = state.items, title = "Secure Gallery", onBack = null,
                    onImport = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, selected = selectedIds,
                    onSelect = { id -> selectedIds = selectedIds.toMutableSet().apply { if (!add(id)) remove(id) } },
                    onSelectionSet = { selectedIds = it }, onSelectAll = { visible -> selectedIds = visible.mapTo(LinkedHashSet()) { it.id } },
                    onCreate = { type, visible ->
                        secureCreativeRequest = type to visible
                    }, onOpen = { item, displayed -> openSecureItem(item, displayed) },
                )
            }
            }
            if (secureCollection == "Clean out" && (verifiedSecureDuplicates == null || secureQualityCollections == null) && !state.busy) {
                Box(Modifier.fillMaxSize().background(VaultBackground.copy(alpha = .94f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        CircularProgressIndicator(color = VaultSecure)
                        Text("Reviewing secure media…", color = VaultSecondary)
                        Text("Exact hashes and in-memory thumbnails only", style = MaterialTheme.typography.bodySmall, color = VaultSecondary)
                    }
                }
            }
            if (state.busy) {
                Box(Modifier.fillMaxSize().background(Color(0xAA000000)), contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = VaultRaised), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            CircularProgressIndicator(color = VaultSecure)
                            Text(state.progress ?: "Working securely")
                            state.operationProgress?.let { percent ->
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { percent / 100f },
                                    color = VaultSecure,
                                    modifier = Modifier.width(230.dp),
                                )
                                Text("$percent%", color = VaultSecondary)
                            }
                            if (state.operationCancellable) {
                                TextButton(onClick = viewModel::cancelCreate) { Text("Cancel") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (menuOpen) ModalBottomSheet(onDismissRequest = { menuOpen = false }, containerColor = VaultRaised, dragHandle = null) {
        SecureMenuSheet(
            onVideos = { menuOpen = false; secureCollection = "Videos"; tab = SecureTab.PICTURES },
            onFavourites = { menuOpen = false; secureCollection = "Favourites"; tab = SecureTab.PICTURES },
            onRecent = { menuOpen = false; secureCollection = "Recent"; tab = SecureTab.PICTURES },
            onCleanOut = { menuOpen = false; secureCleanReason = null; secureCollection = "Clean out"; tab = SecureTab.PICTURES },
            onLocations = { menuOpen = false; secureCollection = "Locations"; tab = SecureTab.PICTURES },
            onShared = { menuOpen = false; secureCollection = "Shared albums"; tab = SecureTab.PICTURES },
            onImport = { menuOpen = false; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
            onSettings = { menuOpen = false; settings = true },
            onTrash = { menuOpen = false; trashOpen = true },
            onOpenGallery = { menuOpen = false; context.startActivity(Intent(context, MainGalleryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) },
            onLock = { viewModel.lock(); menuOpen = false },
        )
    }
    LaunchedEffect(state.unlocked, incomingCount, incomingSourceKind) {
        if (state.unlocked && incomingCount > 0) {
            // Preserve each source folder automatically. A missing secure album is created and
            // an existing album with the same name is merged without another decision screen.
            onIncomingDestination("")
        }
    }
    if (state.unlocked && pickedUris.isNotEmpty()) {
        SecureAlbumDestinationPicker(
            move = false,
            albums = state.items.groupBy { it.albumName },
            sourceAlbumNames = emptySet(),
            allowOriginalFolders = true,
            onDismiss = { pickedUris = emptyList() },
            onSelect = { album -> viewModel.queueImport(pickedUris, targetAlbum = album); pickedUris = emptyList() },
        )
    }
}

@Composable
private fun SecureAuthScreen(
    state: SecureGalleryState,
    onSetup: (String, String) -> Unit,
    onUnlock: (String) -> Unit,
    biometricsAvailable: Boolean,
    onBiometricUnlock: () -> Unit,
) {
    var secret by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var autoBiometricAttempted by remember { mutableStateOf(false) }
    var submittedPin by remember { mutableStateOf<String?>(null) }
    val pinFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val pinAllowed = !state.configured || state.unlockPolicy != SecureUnlockPolicy.BIOMETRIC_ONLY
    val biometricAllowed = state.configured && state.unlockPolicy != SecureUnlockPolicy.PIN_ONLY
    LaunchedEffect(state.configured, state.biometricEnabled, biometricsAvailable, state.unlockPolicy) {
        if (biometricAllowed && state.biometricEnabled && biometricsAvailable && !autoBiometricAttempted) {
            autoBiometricAttempted = true
            onBiometricUnlock()
        } else if (state.configured && pinAllowed) {
            delay(250)
            pinFocusRequester.requestFocus()
            keyboard?.show()
        }
    }
    LaunchedEffect(secret, state.configured, state.busy, pinAllowed) {
        if (state.configured && pinAllowed && secret.length == 4 && !state.busy && submittedPin != secret) {
            submittedPin = secret
            keyboard?.hide()
            onUnlock(secret)
        }
    }
    LaunchedEffect(state.error) {
        if (state.configured && state.error?.startsWith("Incorrect PIN") == true) {
            secret = ""
            submittedPin = null
            delay(120)
            pinFocusRequester.requestFocus()
            keyboard?.show()
        }
    }
    Column(
        Modifier.fillMaxSize().background(VaultBackground).statusBarsPadding().navigationBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(if (state.configured) Icons.Outlined.Lock else Icons.Outlined.LockOpen, null, Modifier.size(68.dp), tint = VaultSecure)
        Spacer(Modifier.height(20.dp))
        Text(if (state.configured) "Unlock Secure Gallery" else "Create Secure Gallery", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            if (state.configured && !pinAllowed) "Authenticate with your fingerprint or strong face unlock."
            else if (state.configured) "Enter your four-digit PIN. Secure Gallery unlocks automatically after the fourth digit."
            else "Choose a four-digit PIN. There is no password reset, so keep it somewhere safe.",
            color = VaultSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (pinAllowed) {
            OutlinedTextField(
                secret, { value ->
                    secret = value.filter(Char::isDigit).take(4)
                    if (secret.length < 4) submittedPin = null
                },
                label = { Text(if (state.configured) "PIN" else "New 4-digit PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.None),
                enabled = !state.busy,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(pinFocusRequester),
            )
        }
        if (!state.configured) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                confirmation, { value -> confirmation = value.filter(Char::isDigit).take(4) }, label = { Text("Confirm PIN") },
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            if (confirmation.isNotEmpty() && secret != confirmation) Text("PINs do not match", color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        Spacer(Modifier.height(22.dp))
        if (state.configured && pinAllowed) {
            if (state.busy) CircularProgressIndicator(Modifier.size(28.dp))
            else Text("Unlocks automatically after 4 digits", color = VaultSecondary, style = MaterialTheme.typography.bodyMedium)
        } else if (!state.configured) {
            Button(
                onClick = { onSetup(secret, confirmation) },
                enabled = !state.busy && secret.length == 4 && secret == confirmation,
                modifier = Modifier.fillMaxWidth(),
            ) { if (state.busy) CircularProgressIndicator(Modifier.size(22.dp)) else Text("Create Secure Gallery") }
        }
        if (biometricAllowed && state.biometricEnabled && biometricsAvailable) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onBiometricUnlock, modifier = Modifier.fillMaxWidth()) { Text("Unlock with fingerprint or face") }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (state.configured && !pinAllowed && !biometricsAvailable) "Biometric hardware is unavailable. Re-enable it in Android settings to unlock this biometric-only vault."
            else "After creation, open Menu → Settings to enable biometrics, change the PIN, and choose auto-lock timing.",
            color = VaultSecondary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SecureToolbar(title: String, back: (() -> Unit)? = null, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().height(88.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.padding(horizontal = 6.dp).size(36.dp).clip(RoundedCornerShape(18.dp)).background(Color(0x2238D3AA)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Lock, "Secure", tint = VaultSecure, modifier = Modifier.size(21.dp))
        }
        action?.invoke()
    }
}

@Composable
private fun SecureCreativeStudio(
    type: CreationType,
    candidates: List<SecureItem>,
    viewModel: SecureGalleryViewModel,
    onBack: () -> Unit,
    onCreate: (List<SecureItem>, CreativeOptions) -> Unit,
) {
    val context = LocalContext.current
    val usable = remember(candidates) { candidates.distinctBy { it.id } }
    val limit = if (type == CreationType.COLLAGE) 9 else 16
    var selectedIds by remember(usable, type) { mutableStateOf(usable.take(if (type == CreationType.COLLAGE) 4 else 8).map { it.id }) }
    var options by remember(type) { mutableStateOf(CreativeOptions()) }
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull().orEmpty().ifBlank { uri.lastPathSegment ?: "Audio track" }
            options = options.copy(movie = options.movie.copy(backgroundAudioUri = uri, backgroundAudioName = name))
        }
    }
    val selected = remember(usable, selectedIds, limit) { selectedIds.mapNotNull { id -> usable.firstOrNull { it.id == id } }.take(limit) }
    Column(Modifier.fillMaxSize().background(VaultBackground).navigationBarsPadding()) {
        SecureToolbar("Create ${type.title}", onBack) {
            Button(enabled = selected.isNotEmpty(), onClick = {
                onCreate(selected, options)
            }) { Text("Create") }
        }
        Text(
            "Choose up to $limit photos or videos. Numbers show the final order.",
            color = VaultSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (type == CreationType.COLLAGE && selected.isNotEmpty()) {
            LaunchedEffect(options.collage.layout, selected.size) {
                if (options.collage.layout == CollageLayout.FREEFORM && options.collage.freeformPlacements.size != selected.size) {
                    options = options.copy(collage = options.collage.copy(freeformPlacements = defaultFreeformPlacements(selected.size, options.collage)))
                }
            }
            BoxWithConstraints(
                Modifier.fillMaxWidth().height(210.dp).padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color(options.collage.background)),
            ) {
                if (options.collage.photoBackground) {
                    val backgroundItem = selected.first()
                    val background by produceState<android.graphics.Bitmap?>(initialValue = null, backgroundItem.id) {
                        value = viewModel.previewImageThumbnail(backgroundItem)
                    }
                    background?.let { bitmap ->
                        androidx.compose.foundation.Image(
                            bitmap.asImageBitmap(),
                            "Photo background",
                            Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Box(Modifier.fillMaxSize().background(Color(0x55000000)))
                    }
                }
                val latestOptions by rememberUpdatedState(options)
                val density = LocalDensity.current
                val previewWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                val previewHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
                val gapX = maxWidth * (options.collage.gap.toFloat() / options.collage.ratio.width)
                val gapY = maxHeight * (options.collage.gap.toFloat() / options.collage.ratio.height)
                collagePlacements(selected.size, options.collage).forEachIndexed { index, rect ->
                    val item = selected[index]
                    val thumbnail by produceState<android.graphics.Bitmap?>(initialValue = null, item.id) {
                        value = viewModel.previewImageThumbnail(item)
                    }
                    thumbnail?.let { bitmap ->
                        val itemWidth = (maxWidth * rect.width() - gapX * 2).coerceAtLeast(1.dp)
                        val itemHeight = (maxHeight * rect.height() - gapY * 2).coerceAtLeast(1.dp)
                        val corner = (minOf(itemWidth.value, itemHeight.value) *
                            options.collage.roundness.coerceIn(0, 100) / 200f).dp
                        val freeformModifier = if (options.collage.layout == CollageLayout.FREEFORM) Modifier
                            .graphicsLayer { rotationZ = options.collage.freeformPlacements.getOrNull(index)?.rotation ?: 0f }
                            .pointerInput(index, previewWidthPx, previewHeightPx) {
                                detectTransformGestures { _, pan, zoom, rotationChange ->
                                    val active = latestOptions.collage.freeformPlacements.getOrNull(index) ?: return@detectTransformGestures
                                    val changed = transformCollagePlacement(active, pan.x / previewWidthPx, pan.y / previewHeightPx, zoom, rotationChange)
                                    val placements = latestOptions.collage.freeformPlacements.toMutableList().apply { this[index] = changed }
                                    options = latestOptions.copy(collage = latestOptions.collage.copy(freeformPlacements = placements))
                                }
                            } else Modifier
                        androidx.compose.foundation.Image(
                            bitmap.asImageBitmap(),
                            item.name,
                            Modifier.offset(maxWidth * rect.left + gapX, maxHeight * rect.top + gapY)
                                .width(itemWidth)
                                .height(itemHeight)
                                .then(freeformModifier)
                                .clip(RoundedCornerShape(corner)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                selected.forEachIndexed { index, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}: ${item.name.take(10)}", color = Color.White)
                        TextButton(enabled = index > 0, onClick = { selectedIds = selectedIds.toMutableList().apply { add(index - 1, removeAt(index)) } }) { Text("←") }
                        TextButton(enabled = index < selected.lastIndex, onClick = { selectedIds = selectedIds.toMutableList().apply { add(index + 1, removeAt(index)) } }) { Text("→") }
                    }
                }
            }
        }
        run {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(usable, key = { it.id }) { item ->
                    val index = selectedIds.indexOf(item.id)
                    Box {
                        SecureTile(
                            item = item,
                            selected = index >= 0,
                            selectionMode = false,
                            onOpen = {
                                selectedIds = selectedIds.toMutableList().apply {
                                    if (index >= 0) remove(item.id) else if (size < limit) add(item.id)
                                }
                            },
                        )
                        if (index >= 0) Text(
                            "${index + 1}", color = Color.White,
                            modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(CircleShape).background(VaultSecure).padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (type) {
                CreationType.COLLAGE -> CollageDesignControls(
                    options.collage,
                    onChange = { options = options.copy(collage = it) },
                    onShuffle = { selectedIds = selectedIds.shuffled() },
                )
                CreationType.GIF -> {
                    Text("Frame speed: ${"%.1f".format(options.gifDelayMs / 1000f)} s", color = VaultSecondary)
                    Slider(options.gifDelayMs.toFloat(), { options = options.copy(gifDelayMs = it.toInt()) }, valueRange = 100f..2_000f)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(360, 480, 720).forEach { size ->
                            FilterChip(options.gifSize == size, { options = options.copy(gifSize = size) }, label = { Text("${size}p") })
                        }
                    }
                }
                CreationType.MOVIE -> {
                    Text("Each picture: ${options.movie.secondsPerImage} s", color = VaultSecondary)
                    Slider(
                        options.movie.secondsPerImage.toFloat(),
                        { options = options.copy(movie = options.movie.copy(secondsPerImage = it.toInt())) },
                        valueRange = 1f..5f,
                        steps = 3,
                    )
                    Text("Each video clip: up to ${options.movie.maxVideoSeconds} s", color = VaultSecondary)
                    Slider(
                        options.movie.maxVideoSeconds.toFloat(),
                        { options = options.copy(movie = options.movie.copy(maxVideoSeconds = it.toInt())) },
                        valueRange = 2f..30f,
                        steps = 27,
                    )
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        MovieRatio.entries.forEach { ratio ->
                            FilterChip(options.movie.ratio == ratio, { options = options.copy(movie = options.movie.copy(ratio = ratio)) }, label = { Text(ratio.label) })
                        }
                        listOf(480, 720, 1080).forEach { height ->
                            FilterChip(options.movie.height == height, { options = options.copy(movie = options.movie.copy(height = height)) }, label = { Text("${height}p") })
                        }
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        MovieTransition.entries.forEach { transition ->
                            FilterChip(options.movie.transition == transition, { options = options.copy(movie = options.movie.copy(transition = transition)) }, label = { Text(transition.label) })
                        }
                    }
                    androidx.compose.material3.OutlinedTextField(
                        value = options.movie.title,
                        onValueChange = { options = options.copy(movie = options.movie.copy(title = it.take(80))) },
                        label = { Text("Title overlay (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { musicPicker.launch(arrayOf("audio/*")) }) {
                            Text(if (options.movie.backgroundAudioUri == null) "Add music" else "Change music")
                        }
                        if (options.movie.backgroundAudioUri != null) {
                            Text(
                                options.movie.backgroundAudioName,
                                color = VaultSecondary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                            )
                            TextButton(onClick = {
                                options = options.copy(movie = options.movie.copy(backgroundAudioUri = null, backgroundAudioName = ""))
                            }) { Text("Remove") }
                        }
                    }
                }
            }
            if (type == CreationType.COLLAGE && options.collage.layout == CollageLayout.FREEFORM) {
                Text("Drag to move. Pinch to resize and rotate each photo.", color = VaultSecondary)
            }
            Text(
                when (type) {
                    CreationType.COLLAGE -> "Layout, background, border, ratio and ordering are applied inside Secure Gallery."
                    CreationType.GIF -> "The animation stays in the source secure album and storage mode."
                    CreationType.MOVIE -> "The H.264 movie stays in the source secure album and storage mode."
                },
                color = VaultSecondary,
            )
        }
    }
}

@Composable
private fun SecurePictures(
    state: SecureGalleryState,
    gridColumns: Int,
    items: List<SecureItem>,
    title: String,
    onBack: (() -> Unit)?,
    onImport: () -> Unit,
    selected: Set<String>,
    onSelect: (String) -> Unit,
    onSelectionSet: (Set<String>) -> Unit,
    onSelectAll: (List<SecureItem>) -> Unit,
    onCreate: (CreationType, List<SecureItem>) -> Unit,
    onOpen: (SecureItem, List<SecureItem>) -> Unit,
    cleanOutReason: QualityReviewReason? = null,
    cleanOutCounts: Map<QualityReviewReason, Int>? = null,
    duplicateCount: Int = 0,
    onCleanOutReasonChanged: (QualityReviewReason?) -> Unit = {},
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var createDialog by remember { mutableStateOf(false) }
    var slideshow by remember { mutableStateOf(false) }
    val preferences = remember { context.getSharedPreferences("secure-gallery-settings", 0) }
    var sort by remember { mutableStateOf(mediaSortFrom(preferences.getString("media_sort", null))) }
    var sortDialog by remember { mutableStateOf(false) }
    val visible = remember(items, query, sort) {
        items.filter { query.isBlank() || it.name.contains(query, true) || it.albumName.contains(query, true) || it.mimeType.contains(query, true) }
            .sortedSecureByPreference(sort)
    }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val orderedIds = remember(visible) { visible.map { it.id } }
    val latestSelected by rememberUpdatedState(selected)
    val latestSelectionSet by rememberUpdatedState(onSelectionSet)
    var dragAnchor by remember { mutableStateOf<String?>(null) }
    var dragBase by remember { mutableStateOf<Set<String>>(emptySet()) }
    var dragSelect by remember { mutableStateOf(true) }
    val visibleColumns = remember(gridColumns) { mutableIntStateOf(gridColumns.coerceIn(3, 12)) }

    fun itemAt(x: Float, y: Float): String? = gridState.layoutInfo.visibleItemsInfo
        .lastOrNull { info ->
            info.key is String && info.key in orderedIds && x >= info.offset.x && x <= info.offset.x + info.size.width &&
                y >= info.offset.y && y <= info.offset.y + info.size.height
        }?.key as? String

    fun updateSlideSelection(current: String) {
        val anchor = dragAnchor ?: return
        latestSelectionSet(GalleryLogic.slideSelection(orderedIds, dragBase, anchor, current, dragSelect))
    }
    if (slideshow) {
        SecureSlideshow(visible.filterNot { it.isVideo }, viewModel = androidx.lifecycle.viewmodel.compose.viewModel(), onBack = { slideshow = false })
        return
    }
    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) {
            CollectionSelectionHeader(
                items = visible,
                selected = selected,
                keyOf = { it.id },
                dateOf = { it.dateTakenMs },
                onSelectionSet = onSelectionSet,
                onCancel = { onSelectionSet(emptySet()) },
            )
        } else SecureToolbar(title, onBack) {
            IconButton(onClick = { searchOpen = !searchOpen }) { Icon(Icons.Outlined.Search, "Search secure media") }
            IconButton(onClick = onImport) { Icon(Icons.Outlined.Upload, "Import media") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Select") }, onClick = { menu = false; visible.firstOrNull()?.let { onSelect(it.id) } })
                    DropdownMenuItem(text = { Text("Sort: ${sort.label}") }, onClick = { menu = false; sortDialog = true })
                    DropdownMenuItem(text = { Text("Create") }, onClick = { menu = false; createDialog = true })
                    DropdownMenuItem(text = { Text("Start slideshow") }, onClick = {
                        menu = false
                        if (visible.any { !it.isVideo }) slideshow = true else Toast.makeText(context, "No pictures to show", Toast.LENGTH_SHORT).show()
                    })
                }
            }
        }
        if (searchOpen) OutlinedTextField(query, { query = it }, label = { Text("Search names and albums") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp))
        if (cleanOutCounts != null) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = cleanOutReason == null,
                        onClick = { onCleanOutReasonChanged(null) },
                        label = { Text("Exact duplicates ($duplicateCount)") },
                    )
                }
                lazyRowItems(QualityReviewReason.entries.filterNot { it == QualityReviewReason.BURST }, key = { it.name }) { reason ->
                    FilterChip(
                        selected = cleanOutReason == reason,
                        onClick = { onCleanOutReasonChanged(reason) },
                        label = { Text("${reason.label} (${cleanOutCounts[reason] ?: 0})") },
                    )
                }
            }
            Text(
                "Review suggestions only. Nothing is selected or deleted automatically.",
                color = VaultSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(28.dp)) {
                    Icon(if (cleanOutCounts == null) Icons.Outlined.Lock else Icons.Outlined.Check, null, Modifier.size(58.dp), tint = VaultSecure)
                    Text(if (cleanOutCounts == null) "Your vault is empty" else "Nothing to review here", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (cleanOutCounts == null) "Import photos or videos. Originals remain unchanged unless you delete them separately from the public gallery."
                        else "Try another review category. Vault Gallery never deletes suggested items automatically.",
                        color = VaultSecondary,
                        textAlign = TextAlign.Center,
                    )
                    if (cleanOutCounts == null) Button(onClick = onImport) { Icon(Icons.Outlined.Upload, null); Spacer(Modifier.width(8.dp)); Text("Import media") }
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(visibleColumns.intValue), contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.pinchToResizeGrid(visibleColumns, 3, 12, gridState) { columns ->
                    preferences.edit().putInt("grid_columns", columns).apply()
                }.pointerInput(visible) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                        itemAt(longPress.position.x, longPress.position.y)?.let { id ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                            itemAt(change.position.x, change.position.y)?.let(::updateSlideSelection)
                        }
                        dragAnchor = null
                    }
                },
            ) {
                val datedGroups: List<Pair<String?, List<SecureItem>>> = if (sort == MediaSort.NEWEST || sort == MediaSort.OLDEST) {
                    visible.groupBy { item ->
                        java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(item.dateTakenMs))
                    }.map { it.key to it.value }
                } else listOf(null to visible)
                datedGroups.forEach { (date, datedItems) ->
                    if (date != null) item(key = "date-$date", span = { GridItemSpan(maxLineSpan) }) {
                        Text(date, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp))
                    }
                    items(datedItems, key = { it.id }) { item ->
                        SecureTile(item, selected = item.id in selected, selectionMode = selected.isNotEmpty(), onSelect = onSelect, onOpen = { opened -> onOpen(opened, visible) })
                    }
                }
            }
        }
    }
    if (createDialog) CreateMediaSheet(onDismiss = { createDialog = false }) { type ->
        createDialog = false
        onCreate(type, visible)
    }
    if (sortDialog) MediaSortDialog(sort, onSelect = { selectedSort -> sort = selectedSort; preferences.edit().putString("media_sort", selectedSort.name).apply(); sortDialog = false }, onDismiss = { sortDialog = false })
}

@Composable
private fun SecureSlideshow(items: List<SecureItem>, viewModel: SecureGalleryViewModel, onBack: () -> Unit) {
    if (items.isEmpty()) { onBack(); return }
    val motion = LocalVaultMotion.current
    var index by remember(items) { mutableIntStateOf(0) }
    LaunchedEffect(index, items) { delay(3_500); if (index < items.lastIndex) index++ else onBack() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AnimatedContent(
            items[index],
            label = "secure slideshow",
            transitionSpec = { fadeIn(tween(motion.duration(motion.deliberateMs))) togetherWith fadeOut(tween(motion.duration(motion.deliberateMs))) },
        ) { SecureImage(it, viewModel) }
        IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(10.dp)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Close slideshow", tint = Color.White) }
        Text("${index + 1} / ${items.size}", color = Color.White, modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(22.dp))
    }
}

@Composable
private fun SecureTile(
    item: SecureItem,
    modifier: Modifier = Modifier.aspectRatio(1f),
    selected: Boolean = false,
    selectionMode: Boolean = false,
    interactive: Boolean = true,
    onSelect: ((String) -> Unit)? = null,
    onOpen: (SecureItem) -> Unit,
) {
    val viewModel: SecureGalleryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val interaction = if (interactive) Modifier.combinedClickable(
        onClick = { if (selectionMode && onSelect != null) onSelect(item.id) else onOpen(item) },
        onLongClick = { onSelect?.invoke(item.id) ?: onOpen(item) },
    ) else Modifier
    Box(modifier.clip(RoundedCornerShape(1.dp)).background(VaultSurface).then(interaction), contentAlignment = Alignment.Center) {
        if (item.isVideo) {
            val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, item.id) { value = viewModel.previewVideoThumbnail(item) }
            if (bitmap != null) androidx.compose.foundation.Image(bitmap!!.asImageBitmap(), item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Row(
                Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(5.dp))
                    .background(Color(0x99000000)).padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.PlayArrow, null, Modifier.size(14.dp), tint = Color.White)
                Text(GalleryLogic.durationLabel(item.durationMs), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        } else {
            val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, item.id) { value = viewModel.previewImageThumbnail(item) }
            if (bitmap != null) androidx.compose.foundation.Image(bitmap!!.asImageBitmap(), item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        if (selected) Box(Modifier.fillMaxSize().background(Color(0x55000000))) {
            Text("✓", color = Color.White, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(18.dp)).background(VaultSecure).padding(horizontal = 8.dp, vertical = 3.dp))
        }
    }
}

@Composable
private fun SecureSelectionBar(
    selectedItems: List<SecureItem>,
    existingAlbums: Map<String, List<SecureItem>>,
    publicAlbums: List<GalleryAlbum>,
    onCopyOut: (String) -> Unit,
    onMoveOut: (String) -> Unit,
    onPlaceInAlbum: (String, Boolean) -> Unit,
    onCreate: (CreationType) -> Unit,
    onShare: () -> Unit,
    onFavourite: (Boolean) -> Unit,
    onTag: (String) -> Unit,
    onDate: (Long) -> Unit,
    onLocation: (Double?, Double?) -> Unit,
    onStoragePolicy: (SecureStoragePolicy) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    val count = selectedItems.size
    var secureTransferMove by remember { mutableStateOf<Boolean?>(null) }
    var publicTransferMove by remember { mutableStateOf<Boolean?>(null) }
    var createDialog by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var deletePrompt by remember { mutableStateOf(false) }
    var tagDialog by remember { mutableStateOf(false) }
    var dateDialog by remember { mutableStateOf(false) }
    var locationDialog by remember { mutableStateOf(false) }
    var tagText by remember(selectedItems) { mutableStateOf(selectedItems.map { it.tag }.distinct().singleOrNull().orEmpty()) }
    var dateText by remember(selectedItems) { mutableStateOf(selectedItems.firstOrNull()?.dateTakenMs?.toString().orEmpty()) }
    var latitudeText by remember(selectedItems) { mutableStateOf(selectedItems.firstOrNull()?.latitude?.toString().orEmpty()) }
    var longitudeText by remember(selectedItems) { mutableStateOf(selectedItems.firstOrNull()?.longitude?.toString().orEmpty()) }
    Column(Modifier.fillMaxWidth().background(VaultBackground).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$count selected", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
        Row(Modifier.fillMaxWidth().height(78.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            SecureMenuAction(Icons.Outlined.Movie, "Create") { createDialog = true }
            SecureMenuAction(Icons.Outlined.Share, "Copy") { onCopyOut("") }
            SecureMenuAction(Icons.Outlined.Upload, "Move") { onMoveOut("") }
            Box {
                SecureMenuAction(Icons.Outlined.MoreVert, "More") { moreMenu = true }
                DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                    DropdownMenuItem(text = { Text("Copy to secure album") }, leadingIcon = { Icon(Icons.Outlined.Collections, null) }, onClick = { moreMenu = false; secureTransferMove = false })
                    DropdownMenuItem(text = { Text("Move to secure album") }, leadingIcon = { Icon(Icons.Outlined.Upload, null) }, onClick = { moreMenu = false; secureTransferMove = true })
                    DropdownMenuItem(text = { Text("Copy and merge into one Gallery album") }, onClick = { moreMenu = false; publicTransferMove = false })
                    DropdownMenuItem(text = { Text("Move and merge into one Gallery album") }, onClick = { moreMenu = false; publicTransferMove = true })
                    DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(Icons.Outlined.Share, null) }, onClick = { moreMenu = false; onShare() })
                    DropdownMenuItem(text = { Text(if (selectedItems.all { it.isFavourite }) "Remove from favourites" else "Add to favourites") }, leadingIcon = { Icon(Icons.Outlined.Favorite, null) }, onClick = {
                        moreMenu = false; onFavourite(!selectedItems.all { it.isFavourite })
                    })
                    DropdownMenuItem(text = { Text("Add tag") }, onClick = { moreMenu = false; tagDialog = true })
                    DropdownMenuItem(text = { Text("Edit date and time") }, onClick = { moreMenu = false; dateDialog = true })
                    DropdownMenuItem(text = { Text("Edit location") }, onClick = { moreMenu = false; locationDialog = true })
                    if (selectedItems.any { it.storagePolicy != SecureStoragePolicy.ENCRYPTED }) DropdownMenuItem(text = { Text("Encrypt selected items") }, onClick = { moreMenu = false; onStoragePolicy(SecureStoragePolicy.ENCRYPTED) })
                    if (selectedItems.any { it.storagePolicy != SecureStoragePolicy.LOCKED_ONLY }) DropdownMenuItem(text = { Text("Use locked-only storage") }, onClick = { moreMenu = false; onStoragePolicy(SecureStoragePolicy.LOCKED_ONLY) })
                    DropdownMenuItem(text = { Text("Move to Secure recycle bin") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { moreMenu = false; deletePrompt = true })
                }
            }
        }
    }
    publicTransferMove?.let { move ->
        PublicAlbumDestinationPicker(
            move = move,
            albums = publicAlbums,
            sourceAlbumNames = selectedItems.mapTo(LinkedHashSet()) { it.albumName },
            allowOriginalFolders = true,
            onDismiss = { publicTransferMove = null },
            onSelect = { target -> publicTransferMove = null; if (move) onMoveOut(target) else onCopyOut(target) },
        )
    }
    secureTransferMove?.let { move ->
        SecureAlbumDestinationPicker(
            move = move,
            albums = existingAlbums,
            sourceAlbumNames = selectedItems.mapTo(LinkedHashSet()) { it.albumName },
            onDismiss = { secureTransferMove = null },
            onSelect = { target -> secureTransferMove = null; onPlaceInAlbum(target, !move) },
        )
    }
    if (createDialog) CreateMediaSheet(onDismiss = { createDialog = false }) { type ->
        createDialog = false
        onCreate(type)
    }
    if (tagDialog) AlertDialog(
        onDismissRequest = { tagDialog = false },
        title = { Text("Add secure tag") },
        text = { OutlinedTextField(tagText, { tagText = it.take(60) }, label = { Text("Tag") }, singleLine = true) },
        dismissButton = { TextButton(onClick = { tagDialog = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { tagDialog = false; onTag(tagText.trim()) }) { Text("Save") } },
    )
    if (dateDialog) AlertDialog(
        onDismissRequest = { dateDialog = false },
        title = { Text("Edit date and time") },
        text = { OutlinedTextField(dateText, { dateText = it.filter(Char::isDigit).take(16) }, label = { Text("Unix time in milliseconds") }, singleLine = true) },
        dismissButton = { TextButton(onClick = { dateDialog = false }) { Text("Cancel") } },
        confirmButton = { TextButton(enabled = dateText.toLongOrNull() != null, onClick = { dateText.toLongOrNull()?.let(onDate); dateDialog = false }) { Text("Save") } },
    )
    if (locationDialog) AlertDialog(
        onDismissRequest = { locationDialog = false },
        title = { Text("Edit location") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(latitudeText, { latitudeText = it.take(16) }, label = { Text("Latitude") }, singleLine = true)
            OutlinedTextField(longitudeText, { longitudeText = it.take(16) }, label = { Text("Longitude") }, singleLine = true)
        } },
        dismissButton = { TextButton(onClick = { locationDialog = false; onLocation(null, null) }) { Text("Remove") } },
        confirmButton = { TextButton(enabled = latitudeText.toDoubleOrNull()?.let { it in -90.0..90.0 } == true && longitudeText.toDoubleOrNull()?.let { it in -180.0..180.0 } == true, onClick = {
            onLocation(latitudeText.toDoubleOrNull(), longitudeText.toDoubleOrNull()); locationDialog = false
        }) { Text("Save") } },
    )
    if (deletePrompt) AlertDialog(
        onDismissRequest = { deletePrompt = false },
        title = { Text("Move to Secure recycle bin?") },
        text = { Text("$count selected item${if (count == 1) "" else "s"} will be kept in the Secure recycle bin until you remove them permanently.") },
        dismissButton = { TextButton(onClick = { deletePrompt = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { deletePrompt = false; onDelete() }) { Text("Move to bin") } },
    )
}

@Composable
private fun SecureAlbumDestinationPicker(
    move: Boolean,
    albums: Map<String, List<SecureItem>>,
    sourceAlbumNames: Set<String>,
    sourceKind: TransferSourceKind = TransferSourceKind.MEDIA,
    sourceFolderCount: Int = sourceAlbumNames.size,
    allowOriginalFolders: Boolean = false,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var createDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    var pendingMerge by remember { mutableStateOf<String?>(null) }
    val destinations = remember(albums, query, move, sourceAlbumNames, sourceKind) {
        albums.entries
            .filter { query.isBlank() || it.key.contains(query, ignoreCase = true) }
            .filterNot { shouldExcludeTransferDestination(it.key, sourceAlbumNames, move, sourceKind) }
            .sortedBy { it.key.lowercase() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = VaultRaised, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (sourceKind == TransferSourceKind.ALBUMS) "${if (move) "Move" else "Copy"} album folder${if (sourceFolderCount == 1) "" else "s"}"
                        else if (move) "Move to secure album" else "Copy to secure album",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(if (sourceKind == TransferSourceKind.ALBUMS) "Choose a secure destination folder. No individual media is being selected." else "The transfer remains inside Secure Gallery", color = VaultSecondary)
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
            OutlinedTextField(
                query,
                { query = it.take(60) },
                label = { Text("Search secure albums") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp, max = maxOf(280.dp, currentWindowHeightDp() * .58f)),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                if (allowOriginalFolders) item(key = "keep-original-secure-folders") {
                    Column(Modifier.fillMaxWidth().clickable { onSelect("") }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(VaultSurface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Folder, "Keep original folders", Modifier.size(54.dp), tint = VaultSecure)
                            Icon(Icons.Outlined.Lock, null, Modifier.align(Alignment.BottomEnd).padding(10.dp).size(18.dp), tint = VaultSecure)
                        }
                        Text("Original folders", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                        Text("Create or merge", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                item(key = "create-secure-album") {
                    Column(Modifier.fillMaxWidth().clickable { createDialog = true }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(VaultSurface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Add, "Create secure album", Modifier.size(44.dp), tint = VaultSecure)
                            Icon(Icons.Outlined.Lock, null, Modifier.align(Alignment.BottomEnd).padding(10.dp).size(18.dp), tint = VaultSecure)
                        }
                        Text(if (sourceKind == TransferSourceKind.ALBUMS) "New destination" else "Create album", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                        Text("Locked", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(destinations, key = { "secure-destination-${it.key}" }) { (name, albumItems) ->
                    Column(Modifier.fillMaxWidth().clickable {
                        if (sourceKind == TransferSourceKind.ALBUMS) pendingMerge = name else onSelect(name)
                    }) {
                        if (sourceKind == TransferSourceKind.ALBUMS) {
                            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(VaultSurface), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Folder, name, Modifier.size(64.dp), tint = VaultSecure)
                                Icon(Icons.Outlined.Lock, null, Modifier.align(Alignment.TopEnd).padding(10.dp).size(18.dp), tint = VaultSecure)
                                Text("${albumItems.size}", modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(CircleShape).background(VaultRaised).padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp))) {
                                SecureTile(albumItems.first(), interactive = false, onOpen = {})
                            }
                        }
                        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                        Text(if (sourceKind == TransferSourceKind.ALBUMS) "Secure destination" else "${albumItems.size} item${if (albumItems.size == 1) "" else "s"}", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    if (createDialog) AlertDialog(
        onDismissRequest = { createDialog = false },
        title = { Text(if (sourceKind == TransferSourceKind.ALBUMS) "Create secure destination" else "Create secure album") },
        text = { OutlinedTextField(newAlbumName, { newAlbumName = it.take(60) }, label = { Text("Album name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        dismissButton = { TextButton(onClick = { createDialog = false }) { Text("Cancel") } },
        confirmButton = { TextButton(enabled = newAlbumName.isNotBlank(), onClick = {
            createDialog = false
            onSelect(sanitizeAlbumName(newAlbumName))
        }) { Text("Create") } },
    )
    pendingMerge?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingMerge = null },
            title = { Text("${if (move) "Move" else "Copy"} into $target?") },
            text = {
                Text(
                    "$sourceFolderCount album folder${if (sourceFolderCount == 1) "" else "s"} will be ${if (move) "moved" else "copied"} and merged into “$target”. " +
                        if (move) "Source folders disappear only after every secure item is moved." else "Source folders remain unchanged.",
                )
            },
            dismissButton = { TextButton(onClick = { pendingMerge = null }) { Text("Cancel") } },
            confirmButton = { TextButton(onClick = { pendingMerge = null; onSelect(target) }) { Text(if (move) "Move and merge" else "Copy and merge") } },
        )
    }
}

@Composable
private fun SecureAlbumSelectionBar(
    selectedAlbums: Set<String>,
    selectedItems: List<SecureItem>,
    secureAlbums: Map<String, List<SecureItem>>,
    publicAlbums: List<GalleryAlbum>,
    onCopyOut: (String) -> Unit,
    onMoveOut: (String) -> Unit,
    onRename: (String) -> Unit,
    onPlaceInAlbum: (String, Boolean) -> Unit,
    onStoragePolicy: (SecureStoragePolicy) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    var more by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var deletePrompt by remember { mutableStateOf(false) }
    var publicTransferMove by remember { mutableStateOf<Boolean?>(null) }
    var secureTransferMove by remember { mutableStateOf<Boolean?>(null) }
    var albumName by remember(selectedAlbums) { mutableStateOf(selectedAlbums.singleOrNull().orEmpty()) }
    Column(Modifier.fillMaxWidth().background(VaultBackground).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${selectedAlbums.size} album${if (selectedAlbums.size == 1) "" else "s"} selected", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
        Row(Modifier.fillMaxWidth().height(78.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            SecureMenuAction(Icons.Outlined.Share, "Copy") { onCopyOut("") }
            SecureMenuAction(Icons.Outlined.Upload, "Move") { onMoveOut("") }
            SecureMenuAction(Icons.Outlined.Delete, "Delete") { deletePrompt = true }
            Box {
                SecureMenuAction(Icons.Outlined.MoreVert, "More") { more = true }
                DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                    if (selectedAlbums.size == 1) DropdownMenuItem(
                        text = { Text("Rename album") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                        onClick = { more = false; rename = true },
                    )
                    DropdownMenuItem(text = { Text("Copy into secure album") }, onClick = { more = false; secureTransferMove = false })
                    DropdownMenuItem(text = { Text("Move into secure album") }, onClick = { more = false; secureTransferMove = true })
                    DropdownMenuItem(text = { Text("Copy and merge into one Gallery album") }, onClick = { more = false; publicTransferMove = false })
                    DropdownMenuItem(text = { Text("Move and merge into one Gallery album") }, onClick = { more = false; publicTransferMove = true })
                    if (selectedItems.any { it.storagePolicy != SecureStoragePolicy.ENCRYPTED }) DropdownMenuItem(text = { Text("Encrypt selected albums") }, onClick = { more = false; onStoragePolicy(SecureStoragePolicy.ENCRYPTED) })
                    if (selectedItems.any { it.storagePolicy != SecureStoragePolicy.LOCKED_ONLY }) DropdownMenuItem(text = { Text("Use locked-only storage") }, onClick = { more = false; onStoragePolicy(SecureStoragePolicy.LOCKED_ONLY) })
                }
            }
        }
    }
    publicTransferMove?.let { move ->
        PublicAlbumDestinationPicker(
            move = move,
            albums = publicAlbums,
            sourceAlbumNames = emptySet(),
            sourceKind = TransferSourceKind.ALBUMS,
            sourceFolderCount = selectedAlbums.size,
            allowOriginalFolders = true,
            onDismiss = { publicTransferMove = null },
            onSelect = { target -> publicTransferMove = null; if (move) onMoveOut(target) else onCopyOut(target) },
        )
    }
    secureTransferMove?.let { move ->
        SecureAlbumDestinationPicker(
            move = move,
            albums = secureAlbums,
            sourceAlbumNames = selectedAlbums,
            sourceKind = TransferSourceKind.ALBUMS,
            onDismiss = { secureTransferMove = null },
            onSelect = { target -> secureTransferMove = null; onPlaceInAlbum(target, !move) },
        )
    }
    if (rename) AlertDialog(
        onDismissRequest = { rename = false },
        title = { Text("Rename secure album") },
        text = { OutlinedTextField(albumName, { albumName = it.take(60) }, label = { Text("Album name") }, singleLine = true) },
        dismissButton = { TextButton(onClick = { rename = false }) { Text("Cancel") } },
        confirmButton = { TextButton(enabled = albumName.isNotBlank(), onClick = { rename = false; onRename(albumName.trim()) }) { Text("Rename") } },
    )
    if (deletePrompt) AlertDialog(
        onDismissRequest = { deletePrompt = false },
        title = { Text("Move albums to Secure recycle bin?") },
        text = { Text("${selectedItems.size} secure item${if (selectedItems.size == 1) "" else "s"} will be moved to the Secure recycle bin.") },
        dismissButton = { TextButton(onClick = { deletePrompt = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { deletePrompt = false; onDelete() }) { Text("Move to bin") } },
    )
}

@Composable
private fun SecureAlbums(
    items: List<SecureItem>,
    publicAlbums: List<GalleryAlbum>,
    selected: Set<String>,
    onToggleSelection: (String) -> Unit,
    onOpen: (String) -> Unit,
    onCopyOut: (String, String) -> Unit,
    onMoveOut: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val preferences = remember { context.getSharedPreferences("secure-album-settings", 0) }
    var coverVersion by remember { mutableIntStateOf(0) }
    val albums = remember(items, coverVersion) { items.groupBy { it.albumName }.toList() }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    var publicTransfer by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var coverAlbum by remember { mutableStateOf<String?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(preferences.getString("sort", "Name A-Z") ?: "Name A-Z") }
    var sortDialog by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var essentialDialog by remember { mutableStateOf(false) }
    var hiddenDialog by remember { mutableStateOf(false) }
    var showEssential by remember { mutableStateOf(preferences.getBoolean("show_essential", false)) }
    var essentialNames by remember { mutableStateOf(preferences.getStringSet("essential_names", emptySet()).orEmpty()) }
    var hiddenNames by remember { mutableStateOf(preferences.getStringSet("hidden_names", emptySet()).orEmpty()) }
    val albumColumns = remember {
        mutableIntStateOf(if (preferences.getInt("grid_columns", 3) == 1) 1 else 3)
    }
    val albumGridState = rememberLazyGridState()
    val listMode = albumColumns.intValue == 1
    val visibleAlbums = remember(albums, query, sortMode, showEssential, essentialNames, hiddenNames) {
        albums.filter { it.first !in hiddenNames }
            .filter { !showEssential || it.first in essentialNames }
            .filter { query.isBlank() || it.first.contains(query, true) }.let { filtered ->
            when (sortMode) {
                "Name Z-A" -> filtered.sortedByDescending { it.first.lowercase() }
                "Newest" -> filtered.sortedByDescending { pair -> pair.second.maxOfOrNull { it.dateTakenMs } ?: 0L }
                "Oldest" -> filtered.sortedBy { pair -> pair.second.minOfOrNull { it.dateTakenMs } ?: 0L }
                "Most items" -> filtered.sortedByDescending { it.second.size }
                else -> filtered.sortedBy { it.first.lowercase() }
            }.withSecureCameraFirst()
        }
    }
    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) SelectionHeader(
            selectedCount = selected.size,
            totalCount = visibleAlbums.size,
            onSelectAll = {
                if (selected.size == visibleAlbums.size) selected.toList().forEach(onToggleSelection)
                else visibleAlbums.filterNot { it.first in selected }.forEach { onToggleSelection(it.first) }
            },
            onCancel = { selected.toList().forEach(onToggleSelection) },
        ) else SecureToolbar("Secure albums", action = {
            IconButton(onClick = { searchOpen = !searchOpen }) { Icon(Icons.Outlined.Search, "Search secure albums") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Album options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Select albums") }, onClick = { menu = false; visibleAlbums.firstOrNull()?.let { onToggleSelection(it.first) } })
                    DropdownMenuItem(text = { Text("Sort: $sortMode") }, onClick = { menu = false; sortDialog = true })
                    DropdownMenuItem(text = { Text("Choose essential albums") }, onClick = { menu = false; essentialDialog = true })
                    DropdownMenuItem(text = { Text(if (showEssential) "Show all albums" else "Show essential albums") }, onClick = {
                        menu = false; showEssential = !showEssential; preferences.edit().putBoolean("show_essential", showEssential).apply()
                    })
                    DropdownMenuItem(text = { Text("Hide albums") }, onClick = { menu = false; hiddenDialog = true })
                }
            }
        })
        AnimatedVisibility(searchOpen && selected.isEmpty()) {
            OutlinedTextField(query, { query = it }, label = { Text("Search albums") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
        }
        if (items.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No secure albums", color = VaultSecondary) }
        else LazyVerticalGrid(
            state = albumGridState,
            columns = GridCells.Fixed(albumColumns.intValue),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.pinchToResizeGrid(albumColumns, 1, 3, albumGridState, semanticExtremes = true) { columns ->
                preferences.edit().putInt("grid_columns", columns).apply()
            },
        ) {
            items(visibleAlbums, key = { it.first }) { (name, albumItems) ->
                val coverId = preferences.getString("cover_$name", null)
                val cover = albumItems.firstOrNull { it.id == coverId } ?: albumItems.first()
                val interaction = Modifier.combinedClickable(
                    onClick = { if (selected.isNotEmpty()) onToggleSelection(name) else onOpen(name) },
                    onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggleSelection(name) },
                )
                if (listMode) {
                    Row(interaction.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(104.dp).clip(RoundedCornerShape(18.dp)).background(VaultSurface), contentAlignment = Alignment.Center) {
                            SecureTile(cover, interactive = false, onOpen = {})
                            if (selected.isEmpty()) IconButton(
                                onClick = { selectedAlbum = name },
                                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).clip(RoundedCornerShape(18.dp)).background(Color(0x99000000)),
                            ) { Icon(Icons.Outlined.MoreVert, "Album actions", tint = Color.White) }
                            if (name in selected) {
                                Box(Modifier.fillMaxSize().background(Color(0x55000000)))
                                Box(Modifier.align(Alignment.TopStart).padding(8.dp).size(30.dp).clip(CircleShape).background(VaultSecure), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Check, "Selected", tint = Color.Black, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            Text(name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${albumItems.size} items", color = VaultSecondary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 5.dp))
                        }
                    }
                } else {
                    Column(interaction) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(VaultSurface), contentAlignment = Alignment.Center) {
                            SecureTile(cover, interactive = false, onOpen = {})
                            if (selected.isEmpty()) IconButton(
                                onClick = { selectedAlbum = name },
                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(18.dp)).background(Color(0x99000000)),
                            ) { Icon(Icons.Outlined.MoreVert, "Album actions", tint = Color.White) }
                            if (name in selected) {
                                Box(Modifier.fillMaxSize().background(Color(0x55000000)))
                                Box(Modifier.align(Alignment.TopStart).padding(9.dp).size(30.dp).clip(CircleShape).background(VaultSecure), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Check, "Selected", tint = Color.Black, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        Text(albumItems.size.toString(), color = VaultSecondary)
                    }
                }
            }
        }
    }
    if (sortDialog) AlertDialog(
        onDismissRequest = { sortDialog = false },
        title = { Text("Sort secure albums") },
        text = { Column { listOf("Name A-Z", "Name Z-A", "Newest", "Oldest", "Most items").forEach { option ->
            Row(Modifier.fillMaxWidth().clickable { sortMode = option; preferences.edit().putString("sort", option).apply(); sortDialog = false }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(option, modifier = Modifier.weight(1f))
                if (option == sortMode) Icon(Icons.Outlined.Check, null, tint = VaultSecure)
            }
        } } },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { sortDialog = false }) { Text("Cancel") } },
    )
    if (essentialDialog) AlertDialog(
        onDismissRequest = { essentialDialog = false },
        title = { Text("Essential secure albums") },
        text = { Column(Modifier.heightIn(max = currentWindowHeightDp() * .58f).verticalScroll(rememberScrollState())) {
            albums.sortedBy { it.first.lowercase() }.forEach { (name, _) ->
                val checked = name in essentialNames
                Row(Modifier.fillMaxWidth().clickable {
                    essentialNames = essentialNames.toMutableSet().apply { if (!add(name)) remove(name) }
                    preferences.edit().putStringSet("essential_names", essentialNames).apply()
                }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, modifier = Modifier.weight(1f)); Switch(checked, null)
                }
            }
        } },
        confirmButton = { TextButton(onClick = { essentialDialog = false }) { Text("Done") } },
    )
    if (hiddenDialog) AlertDialog(
        onDismissRequest = { hiddenDialog = false },
        title = { Text("Hide secure albums") },
        text = { Column(Modifier.heightIn(max = currentWindowHeightDp() * .58f).verticalScroll(rememberScrollState())) {
            albums.sortedBy { it.first.lowercase() }.forEach { (name, _) ->
                val hidden = name in hiddenNames
                Row(Modifier.fillMaxWidth().clickable {
                    hiddenNames = hiddenNames.toMutableSet().apply { if (!add(name)) remove(name) }
                    preferences.edit().putStringSet("hidden_names", hiddenNames).apply()
                }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, modifier = Modifier.weight(1f)); Switch(hidden, null)
                }
            }
        } },
        confirmButton = { TextButton(onClick = { hiddenDialog = false }) { Text("Done") } },
    )
    selectedAlbum?.let { album ->
        AlertDialog(
            onDismissRequest = { selectedAlbum = null },
            title = { Text(album) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("All items keep the album name “$album” when transferred to the normal Gallery.")
                Button(onClick = { selectedAlbum = null; coverAlbum = album }) { Text("Change album cover") }
                Button(onClick = { selectedAlbum = null; onCopyOut(album, "") }) { Text("Copy to Gallery — keep album name") }
                Button(onClick = { selectedAlbum = null; onMoveOut(album, "") }) { Text("Move to Gallery — keep album name") }
                TextButton(onClick = { selectedAlbum = null; publicTransfer = album to false }) { Text("Copy and merge into another album") }
                TextButton(onClick = { selectedAlbum = null; publicTransfer = album to true }) { Text("Move and merge into another album") }
            } },
            confirmButton = { TextButton(onClick = { selectedAlbum = null }) { Text("Done") } },
        )
    }
    publicTransfer?.let { (source, move) ->
        PublicAlbumDestinationPicker(
            move = move,
            albums = publicAlbums,
            sourceAlbumNames = emptySet(),
            sourceKind = TransferSourceKind.ALBUMS,
            sourceFolderCount = 1,
            allowOriginalFolders = true,
            onDismiss = { publicTransfer = null },
            onSelect = { target -> publicTransfer = null; if (move) onMoveOut(source, target) else onCopyOut(source, target) },
        )
    }
    coverAlbum?.let { album ->
        val choices = items.filter { it.albumName == album }
        AlertDialog(
            onDismissRequest = { coverAlbum = null },
            title = { Text("Choose cover for $album") },
            text = { LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.heightIn(max = currentWindowHeightDp() * .55f), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                items(choices, key = { it.id }) { item -> SecureTile(item, onOpen = {
                    preferences.edit().putString("cover_$album", item.id).apply(); coverVersion++; coverAlbum = null
                }) }
            } },
            confirmButton = { TextButton(onClick = { coverAlbum = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SecureStories(items: List<SecureItem>, onOpen: (List<SecureItem>) -> Unit) {
    val viewModel: SecureGalleryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    var autoStories by remember { mutableStateOf(preferences.getBoolean("auto_stories", true)) }
    var hideScreenshots by remember { mutableStateOf(preferences.getBoolean("stories_hide_screenshots", false)) }
    var menu by remember { mutableStateOf(false) }
    val formatter = remember { java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()) }
    val stories = remember(items, autoStories, hideScreenshots) {
        if (!autoStories) emptyList() else items.filter { !it.isVideo && (!hideScreenshots || !it.albumName.contains("screenshot", true)) }.groupBy { formatter.format(java.util.Date(it.dateTakenMs)) }
            .entries.sortedByDescending { it.value.maxOfOrNull(SecureItem::dateTakenMs) ?: 0L }
            .filter { it.value.size >= 2 }
    }
    var slideshow by remember { mutableStateOf<List<SecureItem>?>(null) }
    val storyColumns = remember { mutableIntStateOf(2) }
    slideshow?.let { story -> SecureSlideshow(story, viewModel, onBack = { slideshow = null }); return }
    Column(Modifier.fillMaxSize()) {
        SecureToolbar("Secure stories", action = {
            IconButton(
                enabled = stories.isNotEmpty(),
                onClick = { slideshow = stories.firstOrNull()?.value },
            ) { Icon(Icons.Outlined.PlayArrow, "Play latest secure story") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Story settings") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(if (autoStories) "Turn off automatic stories" else "Turn on automatic stories") }, onClick = {
                        menu = false; autoStories = !autoStories; preferences.edit().putBoolean("auto_stories", autoStories).apply()
                    })
                    DropdownMenuItem(text = { Text(if (hideScreenshots) "Show screenshots in stories" else "Hide screenshots from stories") }, onClick = {
                        menu = false; hideScreenshots = !hideScreenshots; preferences.edit().putBoolean("stories_hide_screenshots", hideScreenshots).apply()
                    })
                }
            }
        })
        if (stories.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Movie, null, Modifier.size(48.dp), tint = VaultSecondary)
                Text("No secure stories", style = MaterialTheme.typography.headlineMedium, color = VaultSecondary)
                Text("A private story appears when a month has at least two encrypted pictures.", color = VaultSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 28.dp))
            }
        } else LazyVerticalGrid(
            columns = GridCells.Fixed(storyColumns.intValue),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.pinchToResizeGrid(storyColumns, 1, 3),
        ) {
            items(stories, key = { it.key }) { story ->
                val storyItems = story.value.sortedByDescending(SecureItem::dateTakenMs)
                Column(Modifier.clip(RoundedCornerShape(20.dp)).clickable { onOpen(storyItems) }) {
                    Box(Modifier.fillMaxWidth().aspectRatio(.82f).clip(RoundedCornerShape(22.dp)).background(VaultSurface), contentAlignment = Alignment.Center) {
                        SecureTile(storyItems.first(), interactive = false, onOpen = {})
                        Box(Modifier.align(Alignment.BottomStart).padding(8.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xBB10231F)).padding(horizontal = 9.dp, vertical = 5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Outlined.Lock, null, Modifier.size(15.dp), tint = VaultSecure)
                                Text("Secure", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    Text(story.key, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                    Text("${storyItems.size} moments", color = VaultSecondary)
                }
            }
        }
    }
}

@Composable
private fun SecureMenuSheet(
    onVideos: () -> Unit,
    onFavourites: () -> Unit,
    onRecent: () -> Unit,
    onCleanOut: () -> Unit,
    onLocations: () -> Unit,
    onShared: () -> Unit,
    onImport: () -> Unit,
    onSettings: () -> Unit,
    onTrash: () -> Unit,
    onOpenGallery: () -> Unit,
    onLock: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SecureMenuAction(Icons.Outlined.Movie, "Videos", onVideos)
            SecureMenuAction(Icons.Outlined.Favorite, "Favourites", onFavourites)
            SecureMenuAction(Icons.Outlined.PhotoLibrary, "Recent", onRecent)
            SecureMenuAction(Icons.Outlined.CleaningServices, "Clean out", onCleanOut)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SecureMenuAction(Icons.Outlined.LocationOn, "Locations", onLocations)
            SecureMenuAction(Icons.Outlined.FolderShared, "Shared\nalbums", onShared)
            SecureMenuAction(Icons.Outlined.Delete, "Recycle bin", onTrash)
            SecureMenuAction(Icons.Outlined.Settings, "Settings", onSettings)
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = VaultSurface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).fillMaxSize().clickable(onClick = onOpenGallery).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, "Gallery", tint = VaultSecure)
                    Text("Go to Gallery", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    Text("›", style = MaterialTheme.typography.headlineMedium)
                }
                IconButton(onClick = onImport) { Icon(Icons.Outlined.Upload, "Import", tint = VaultSecure) }
                IconButton(onClick = onLock) { Icon(Icons.Outlined.Lock, "Lock now", tint = VaultSecure) }
                Spacer(Modifier.width(8.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

private fun secureCollectionItems(items: List<SecureItem>, collection: String?): List<SecureItem> = when (collection) {
    "Videos" -> items.filter(SecureItem::isVideo)
    "Favourites" -> items.filter(SecureItem::isFavourite)
    "Recent" -> items.sortedByDescending(SecureItem::dateTakenMs)
    "Locations" -> items.filter { it.latitude != null && it.longitude != null }
    // Secure duplicate eligibility is resolved asynchronously by exactDuplicateItems().
    "Clean out" -> emptyList()
    "Shared albums" -> items
    else -> items
}

@Composable
private fun SecureMenu(
    onVideos: () -> Unit,
    onFavourites: () -> Unit,
    onRecent: () -> Unit,
    onImport: () -> Unit,
    onSettings: () -> Unit,
    onTrash: () -> Unit,
    onOpenGallery: () -> Unit,
    onLock: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        SecureToolbar("Secure menu")
        Spacer(Modifier.weight(1f))
        Card(colors = CardDefaults.cardColors(containerColor = VaultRaised), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    SecureMenuAction(Icons.Outlined.Movie, "Videos", onVideos)
                    SecureMenuAction(Icons.Outlined.Favorite, "Favourites", onFavourites)
                    SecureMenuAction(Icons.Outlined.PhotoLibrary, "Recent", onRecent)
                    SecureMenuAction(Icons.Outlined.Upload, "Import", onImport)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    SecureMenuAction(Icons.Outlined.PhotoLibrary, "Open Gallery", onOpenGallery)
                    SecureMenuAction(Icons.Outlined.Delete, "Recycle bin", onTrash)
                    SecureMenuAction(Icons.Outlined.Settings, "Settings", onSettings)
                    SecureMenuAction(Icons.Outlined.Lock, "Lock now", onLock)
                }
            }
        }
    }
}

@Composable
private fun SecureMenuAction(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Column(
        Modifier.width(78.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, text, Modifier.size(28.dp), tint = VaultPrimary)
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SecureBottomNavigation(selected: SecureTab, onSelected: (SecureTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(VaultBackground).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(82.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            SecureNav(Icons.Outlined.Image, Icons.Filled.FilledImage, "Pictures", selected == SecureTab.PICTURES) { onSelected(SecureTab.PICTURES) }
            SecureNav(Icons.Outlined.Collections, Icons.Filled.FilledCollections, "Albums", selected == SecureTab.ALBUMS) { onSelected(SecureTab.ALBUMS) }
            SecureNav(Icons.Outlined.Movie, Icons.Filled.FilledMovie, "Stories", selected == SecureTab.STORIES) { onSelected(SecureTab.STORIES) }
            SecureNav(Icons.Outlined.Menu, Icons.Filled.FilledMenu, "Menu", selected == SecureTab.MENU) { onSelected(SecureTab.MENU) }
        }
    }
}

@Composable
private fun SecureNav(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) VaultPrimary else VaultSecondary
    Column(Modifier.width(84.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(if (selected) selectedIcon else icon, text, Modifier.size(29.dp), tint = color)
        Text(text, color = color, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun SecureViewer(
    item: SecureItem,
    items: List<SecureItem>,
    viewModel: SecureGalleryViewModel,
    publicAlbums: List<GalleryAlbum>,
    onBack: () -> Unit,
    onNavigate: (SecureItem) -> Unit,
    onShare: (SecureItem, java.io.File) -> Unit,
    onPlay: (SecureItem) -> Unit,
    onEdit: () -> Unit,
    onAiAssist: () -> Unit,
    onScanDocument: () -> Unit,
    onFindSimilar: () -> Unit,
    onCreateGif: () -> Unit,
    onFavourite: () -> Unit,
    onCopyOut: (String) -> Unit,
    onMoveOut: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val approvedApps = remember(item.mimeType) {
        val store = ApprovedPlayerStore(context)
        store.packages().mapNotNull { packageName ->
            runCatching {
                packageName to context.packageManager.getApplicationLabel(
                    context.packageManager.getApplicationInfo(packageName, 0),
                ).toString()
            }.getOrNull()
        }.sortedBy { it.second.lowercase() }
    }
    // Viewer behavior is intentionally shared with the public gallery. Security settings may
    // govern access and storage, but must not silently remove navigation or playback controls.
    val autoPlayVideo = true
    var shareWarning by remember { mutableStateOf(false) }
    var deleteWarning by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    var publicTransferMove by remember { mutableStateOf<Boolean?>(null) }
    var secureTransferMove by remember { mutableStateOf<Boolean?>(null) }
    var chromeVisible by remember { mutableStateOf(true) }
    var videoMuted by remember { mutableStateOf(true) }
    var stripTargetId by remember { mutableStateOf<String?>(null) }
    var stripTargetPositionMs by remember { mutableLongStateOf(0L) }
    var stripTargetPlaying by remember { mutableStateOf(false) }
    var lastStripSeekDispatchMs by remember(item.id) { mutableLongStateOf(0L) }
    var stripScrubInProgress by remember(item.id) { mutableStateOf(false) }
    var resumeAfterStripScrub by remember(item.id) { mutableStateOf(false) }
    var recognizedDocument by remember(item.id) { mutableStateOf<RecognizedDocument?>(null) }
    var showRecognizedText by remember(item.id) { mutableStateOf(false) }
    var motionPhotoPlaying by remember(item.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val inlinePlayer = remember(context) { buildSamsungGalleryPlayer(context) }
    var dragY by remember(item.id) { mutableFloatStateOf(0f) }
    var infoGestureEnabled by remember(item.id) { mutableStateOf(true) }
    var viewerHeight by remember { mutableFloatStateOf(1f) }
    val index = items.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = index, pageCount = { items.size })
    val latestOnNavigate by rememberUpdatedState(onNavigate)
    val latestItemId by rememberUpdatedState(item.id)
    val secureOcrBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, item.id) {
        value = if (item.isVideo) null else viewModel.previewImage(item, 3200)
    }
    val motionPhoto by produceState<ExtractedMotionPhoto?>(initialValue = null, item.id, item.isVideo) {
        value = if (!item.isVideo) withContext(Dispatchers.IO) {
            val source = viewModel.playbackFile(item) ?: return@withContext null
            try {
                runCatching { extractMotionPhoto(context, source, "secure-${item.id}-${item.dateTakenMs}") }.getOrNull()
            } finally {
                viewModel.releasePlaybackFile(item, source)
            }
        } else null
    }
    DisposableEffect(inlinePlayer) { onDispose { inlinePlayer.release() } }
    DisposableEffect(motionPhoto?.file) {
        val file = motionPhoto?.file
        onDispose { file?.delete() }
    }
    LaunchedEffect(item.id, secureOcrBitmap) {
        delay(300)
        recognizedDocument = if (!item.isVideo) secureOcrBitmap?.let { bitmap ->
            runCatching { recognizeDocumentText(bitmap, "secure:${item.id}:${item.dateTakenMs}") }
                .getOrNull()?.takeIf(RecognizedDocument::hasReliableText)
        } else null
    }
    LaunchedEffect(chromeVisible) {
        (context as? SecureGalleryActivity)?.applyViewerImmersive(!chromeVisible)
    }
    DisposableEffect(Unit) {
        onDispose { (context as? SecureGalleryActivity)?.applyViewerImmersive(false) }
    }
    LaunchedEffect(item.id, item.isVideo, motionPhoto?.file, motionPhotoPlaying) {
        inlinePlayer.stop()
        inlinePlayer.clearMediaItems()
        if (item.isVideo) {
            val sourceReady = viewModel.dataSource(item)?.let { dataSource ->
                inlinePlayer.setMediaSource(ProgressiveMediaSource.Factory(dataSource)
                    .createMediaSource(MediaItem.fromUri("secure://${item.id}")))
                true
            } ?: false
            if (sourceReady) {
                val stripControlled = stripTargetId == item.id
                if (stripControlled) inlinePlayer.seekTo(stripTargetPositionMs)
                inlinePlayer.prepare()
                inlinePlayer.playWhenReady = if (stripControlled) stripTargetPlaying else autoPlayVideo
            }
        } else if (motionPhotoPlaying && motionPhoto != null) {
            inlinePlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(motionPhoto!!.file)))
            inlinePlayer.prepare()
            inlinePlayer.playWhenReady = true
        }
    }
    LaunchedEffect(pagerState, items) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { page ->
            items.getOrNull(page)?.takeIf { it.id != latestItemId }?.let(latestOnNavigate)
        }
    }
    LaunchedEffect(item.id, items) {
        val target = items.indexOfFirst { it.id == item.id }
        if (target >= 0 && !pagerState.isScrollInProgress && pagerState.currentPage != target) {
            // Keep the full-size preview locked to the item under the filmstrip's fixed centre.
            // Per-page animations cannot keep up with a continuous strip drag and cause visible
            // catch-up travel, especially across large secure albums.
            pagerState.scrollToPage(target)
        }
    }
    fun settleInfo(show: Boolean) {
        if (show) details = true
        val start = dragY
        scope.launch {
            animate(
                initialValue = start,
                targetValue = 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 720f),
            ) { value, _ -> dragY = value }
        }
    }
    Box(Modifier.fillMaxSize().onSizeChanged { viewerHeight = it.height.toFloat() }.background(Color.Black).viewerInfoGesture(
        key = item.id,
        enabled = infoGestureEnabled,
        onDrag = { dragY = it },
        onRelease = ::settleInfo,
    )) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                pagerSnapDistance = PagerSnapDistance.atMost(1),
            ),
            // The zoom surface releases outward horizontal drags at its content edge, so the
            // parent pager remains live even while an image is enlarged.
            userScrollEnabled = true,
            key = { page -> items[page].id },
        ) { page ->
            val candidate = items[page]
            Box(Modifier.fillMaxSize().background(Color.Black).graphicsLayer {
                translationY = if (candidate.id == item.id) dragY * .16f else 0f
            }) {
                if (candidate.id != item.id) {
                    SecureAdjacentPreview(candidate, viewModel, chromeVisible)
                } else if (item.isVideo) SecureVideo(
                    item = item,
                    viewModel = viewModel,
                    player = inlinePlayer,
                    chromeVisible = chromeVisible,
                    muted = videoMuted,
                    onChromeToggle = { chromeVisible = !chromeVisible },
                    onMutedChange = { videoMuted = it },
                    onZoomChanged = { zoomed -> infoGestureEnabled = !zoomed },
                ) else if (motionPhotoPlaying && motionPhoto != null) {
                    SamsungInlineVideoPlayer(
                        player = inlinePlayer,
                        mediaKey = "secure-motion-${item.id}",
                        durationHintMs = motionPhoto!!.durationMs,
                        chromeVisible = chromeVisible,
                        muted = videoMuted,
                        bottomPadding = SamsungViewerActionBarHeight.value.toInt(),
                        onChromeToggle = { chromeVisible = !chromeVisible },
                        onMutedChange = { videoMuted = it },
                        onZoomChanged = { zoomed -> infoGestureEnabled = !zoomed },
                    )
                } else {
                    SecureImage(
                        item = item,
                        viewModel = viewModel,
                        onTap = { chromeVisible = !chromeVisible },
                        onZoomChanged = { zoomed -> infoGestureEnabled = !zoomed },
                    )
                    if (motionPhoto != null && chromeVisible) {
                        IconButton(
                            onClick = { motionPhotoPlaying = true },
                            modifier = Modifier.align(Alignment.Center).size(64.dp)
                                .background(Color(0xB827272B), CircleShape),
                        ) {
                            Icon(Icons.Outlined.PlayArrow, "Play Motion Photo", tint = Color.White, modifier = Modifier.size(38.dp))
                        }
                    }
                }
            }
        }
        if (showRecognizedText && !item.isVideo) {
            recognizedDocument?.let { document ->
                OcrWordOverlay(document, item.width, item.height, Modifier.fillMaxSize())
            }
        }
        AnimatedVisibility(chromeVisible, modifier = Modifier.align(Alignment.TopCenter)) { Row(Modifier.fillMaxWidth().background(Color(0x52000000)).statusBarsPadding().height(80.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            SamsungTextRecognitionButton(
                visible = recognizedDocument != null,
                onClick = { showRecognizedText = true },
            )
            Icon(
                if (item.storagePolicy == SecureStoragePolicy.ENCRYPTED) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                if (item.storagePolicy == SecureStoragePolicy.ENCRYPTED) "Encrypted" else "Locked-only",
                tint = VaultSecure,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            Box {
                IconButton(onClick = { more = true }) { Icon(Icons.Outlined.MoreVert, "More options", tint = Color.White) }
                DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                    DropdownMenuItem(text = { Text("Details") }, onClick = { more = false; details = true })
                    DropdownMenuItem(text = { Text("Copy to Gallery") }, onClick = { more = false; onCopyOut("") })
                    DropdownMenuItem(text = { Text("Move to Gallery") }, onClick = { more = false; onMoveOut("") })
                    if (item.isVideo) DropdownMenuItem(text = { Text("Open in Video player") }, onClick = {
                        more = false
                        onPlay(item)
                    })
                    approvedApps.forEach { (packageName, label) ->
                        DropdownMenuItem(text = { Text("Open in $label") }, onClick = {
                            more = false
                            launchApprovedSecureApp(context, viewModel, item, packageName)
                        })
                    }
                    if (!item.isVideo) DropdownMenuItem(text = { Text("Scan document") }, onClick = {
                        more = false; onScanDocument()
                    })
                    if (!item.isVideo) DropdownMenuItem(text = { Text("Find visually similar") }, onClick = {
                        more = false; onFindSimilar()
                    })
                    DropdownMenuItem(text = { Text("Copy to secure album") }, onClick = { more = false; secureTransferMove = false })
                    DropdownMenuItem(text = { Text("Move to secure album") }, onClick = { more = false; secureTransferMove = true })
                    DropdownMenuItem(
                        text = { Text(if (item.storagePolicy == SecureStoragePolicy.ENCRYPTED) "Use locked-only storage" else "Encrypt this item") },
                        onClick = {
                            more = false
                            viewModel.convertStoragePolicy(listOf(item), if (item.storagePolicy == SecureStoragePolicy.ENCRYPTED) SecureStoragePolicy.LOCKED_ONLY else SecureStoragePolicy.ENCRYPTED)
                        },
                    )
                    if (item.isVideo) DropdownMenuItem(text = { Text("Create GIF") }, onClick = {
                        more = false; onCreateGif()
                    })
                    if (!item.isVideo) DropdownMenuItem(text = { Text("Copy to clipboard") }, onClick = {
                        more = false
                        scope.launch {
                            viewModel.shareFile(item)?.let { file ->
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.secure.share", file)
                                context.getSystemService(android.content.ClipboardManager::class.java)
                                    .setPrimaryClip(android.content.ClipData.newUri(context.contentResolver, item.name, uri))
                                Toast.makeText(context, "Copied secure image", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                    if (!item.isVideo) DropdownMenuItem(text = { Text("Set as wallpaper") }, onClick = {
                        more = false
                        scope.launch {
                            val bytes = viewModel.previewBytes(item) ?: return@launch
                            withContext(Dispatchers.IO) {
                                try { android.app.WallpaperManager.getInstance(context).setStream(bytes.inputStream()) } finally { bytes.fill(0) }
                            }
                            Toast.makeText(context, "Wallpaper updated", Toast.LENGTH_SHORT).show()
                        }
                    })
                    if (!item.isVideo) DropdownMenuItem(text = { Text("Print") }, onClick = {
                        more = false
                        scope.launch {
                            val bytes = viewModel.previewBytes(item) ?: return@launch
                            val bitmap = try { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) } finally { bytes.fill(0) }
                            bitmap?.let { androidx.print.PrintHelper(context).apply { scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT }.printBitmap(item.name, it) }
                        }
                    })
                    DropdownMenuItem(text = { Text(if (item.isVideo) "Edit video" else "AI photo assist") }, leadingIcon = { Icon(if (item.isVideo) Icons.Outlined.Edit else Icons.Outlined.AutoAwesome, null) }, onClick = { more = false; if (item.isVideo) onEdit() else onAiAssist() })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Move to Secure recycle bin") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { more = false; deleteWarning = true })
                }
            }
        } }
        AnimatedVisibility(chromeVisible, modifier = Modifier.align(Alignment.BottomCenter)) {
            SamsungCenterFilmstrip(
                items = items,
                currentKey = item.id,
                key = { it.id },
                isVideo = { it.isVideo },
                durationMs = { it.durationMs },
                mediaWidth = { it.width },
                mediaHeight = { it.height },
                videoPositionMs = { candidate -> if (candidate.id == item.id) inlinePlayer.currentPosition else 0L },
                videoPlaying = { candidate -> candidate.id == item.id && inlinePlayer.isPlaying },
                // Secure media is never materialized as a plaintext file. The encrypted
                // contact-sheet provider below supplies the filmstrip frames instead.
                videoFrameSource = { null },
                videoFrames = { candidate, modifier -> SecureVideoContactSheet(candidate, viewModel, modifier) },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                    .padding(bottom = SamsungViewerActionBarHeight).height(49.dp),
                onCurrentChanged = { candidate ->
                    if (candidate.id != item.id) onNavigate(candidate)
                },
                onVideoTapped = { candidate ->
                    stripTargetId = candidate.id
                    stripTargetPositionMs = if (candidate.id == item.id) inlinePlayer.currentPosition else 0L
                    stripTargetPlaying = candidate.id == item.id && inlinePlayer.isPlaying
                    if (candidate.id != item.id) onNavigate(candidate)
                },
                onVideoScrub = { candidate, position, final ->
                    if (candidate.id != item.id) {
                        stripTargetId = candidate.id
                        stripTargetPositionMs = position
                        stripTargetPlaying = false
                        onNavigate(candidate)
                    } else {
                        stripTargetId = item.id
                        stripTargetPositionMs = position
                        stripTargetPlaying = if (stripScrubInProgress) resumeAfterStripScrub else inlinePlayer.playWhenReady
                        if (final) {
                            inlinePlayer.setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
                            inlinePlayer.seekTo(position)
                            lastStripSeekDispatchMs = android.os.SystemClock.uptimeMillis()
                            if (inlinePlayer.isScrubbingModeEnabled) inlinePlayer.setScrubbingModeEnabled(false)
                            stripTargetPlaying = resumeAfterStripScrub
                            inlinePlayer.playWhenReady = resumeAfterStripScrub
                            stripScrubInProgress = false
                        } else {
                            if (!stripScrubInProgress) {
                                resumeAfterStripScrub = inlinePlayer.playWhenReady &&
                                    inlinePlayer.playbackState != Player.STATE_ENDED
                                stripScrubInProgress = true
                            }
                            if (!inlinePlayer.isScrubbingModeEnabled) inlinePlayer.setScrubbingModeEnabled(true)
                            val now = android.os.SystemClock.uptimeMillis()
                            if (now - lastStripSeekDispatchMs >= 32L) {
                                inlinePlayer.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
                                inlinePlayer.seekTo(position)
                                inlinePlayer.playWhenReady = resumeAfterStripScrub
                                lastStripSeekDispatchMs = now
                            }
                        }
                    }
                },
                thumbnail = { candidate, modifier ->
                    SecureTile(candidate, modifier = modifier, interactive = false, onOpen = onNavigate)
                },
            )
        }
        if (dragY < -8f) Card(
            colors = CardDefaults.cardColors(containerColor = VaultSurface),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 96.dp).graphicsLayer {
                translationY = (viewerHeight * .18f + dragY).coerceAtLeast(0f)
            },
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.albumName, color = VaultSecondary)
                Text("Release for encrypted details", color = VaultSecure, style = MaterialTheme.typography.labelLarge)
            }
        }
        AnimatedVisibility(chromeVisible, modifier = Modifier.align(Alignment.BottomCenter)) { Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(SamsungViewerActionBarHeight).background(Color(0x66000000)).padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SamsungViewerAction(Icons.Outlined.Favorite, if (item.isFavourite) "Remove from favourites" else "Add to favourites", onClick = onFavourite)
            SamsungViewerAction(Icons.Outlined.Edit, "Edit", onClick = onEdit)
            SamsungViewerAction(Icons.Outlined.AutoAwesome, "AI photo assist", enabled = !item.isVideo, onClick = onAiAssist)
            SamsungViewerAction(Icons.Outlined.Share, "Share") { shareWarning = true }
            SamsungViewerAction(Icons.Outlined.Delete, "Move to Secure recycle bin") { deleteWarning = true }
        } }
    }
    if (showRecognizedText) recognizedDocument?.let { document ->
        RecognizedTextSheet(
            document = document,
            onDismiss = { showRecognizedText = false },
            onScanDocument = {
                showRecognizedText = false
                onScanDocument()
            },
        )
    }
    if (shareWarning) AlertDialog(
        onDismissRequest = { shareWarning = false },
        title = { Text("Share decrypted copy?") },
        text = { Text("The receiving app can keep an unencrypted copy outside Secure Gallery.") },
        dismissButton = { TextButton(onClick = { shareWarning = false }) { Text("Cancel") } },
        confirmButton = {
            TextButton(onClick = {
                shareWarning = false
                scope.launch {
                    val file = viewModel.shareFile(item)
                    if (file != null) onShare(item, file)
                }
            }) { Text("Share") }
        },
    )
    if (deleteWarning) AlertDialog(
        onDismissRequest = { deleteWarning = false }, title = { Text("Move to recycle bin?") },
        text = { Text("The secure item can be restored for 30 days, then it is removed automatically.") },
        dismissButton = { TextButton(onClick = { deleteWarning = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { deleteWarning = false; onDelete() }) { Text("Move") } },
    )
    publicTransferMove?.let { move ->
        PublicAlbumDestinationPicker(
            move = move,
            albums = publicAlbums,
            sourceAlbumNames = setOf(item.albumName),
            allowOriginalFolders = true,
            onDismiss = { publicTransferMove = null },
            onSelect = { target -> publicTransferMove = null; if (move) onMoveOut(target) else onCopyOut(target) },
        )
    }
    secureTransferMove?.let { move ->
        SecureAlbumDestinationPicker(
            move = move,
            albums = viewModel.state.value.items.groupBy { it.albumName },
            sourceAlbumNames = setOf(item.albumName),
            onDismiss = { secureTransferMove = null },
            onSelect = { target -> secureTransferMove = null; viewModel.placeInAlbum(listOf(item), target, copy = !move) },
        )
    }
    if (details) SamsungSecureDetailsSheet(
        item,
        onDismiss = { details = false },
        onEdit = { details = false; onEdit() },
        onTag = { tag -> viewModel.updateMetadata(listOf(item), tag = tag) },
        onRating = { rating -> viewModel.updateMetadata(listOf(item), rating = rating) },
    )
}

@Composable
private fun SamsungSecureDetailsSheet(
    item: SecureItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onTag: (String) -> Unit,
    onRating: (Int) -> Unit,
) {
    val context = LocalContext.current
    var editingTag by remember { mutableStateOf(false) }
    var tag by remember(item.id, item.tag) { mutableStateOf(item.tag) }
    var rating by remember(item.id, item.rating) { mutableIntStateOf(item.rating) }
    var extendedMetadataVisible by remember { mutableStateOf(false) }
    val megapixels = (item.width.toLong() * item.height.toLong()) / 1_000_000.0
    val technicalInfo by produceState<VideoTechnicalInfo?>(initialValue = null, item.id, item.isVideo) {
        value = if (item.isVideo) withContext(Dispatchers.IO) {
            runCatching { readVideoTechnicalInfo(context, SecureMediaProvider.uri(context, item)) }.getOrNull()
        } else null
    }
    val secureUri = remember(item.id) { SecureMediaProvider.uri(context, item) }
    val imageInfo by produceState<ImageTechnicalInfo?>(initialValue = null, item.id, item.isVideo) {
        value = if (!item.isVideo) withContext(Dispatchers.IO) {
            runCatching { readImageTechnicalInfo(context, secureUri) }.getOrNull()
        } else null
    }
    val extendedMetadata by produceState<List<ExtendedMetadataSection>?>(initialValue = null, item.id) {
        value = withContext(Dispatchers.IO) { runCatching { readExtendedMetadata(context, secureUri) }.getOrDefault(emptyList()) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        shape = RectangleShape,
        dragHandle = null,
        scrimColor = Color.Transparent,
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.58f).padding(horizontal = 24.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(item.dateTakenMs)), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onEdit) { Text("Edit") }
            }
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text("/Secure Gallery/${item.albumName}", color = VaultSecondary)
            if (item.isVideo) {
                val resolutionClass = videoResolutionClass(item.width, item.height)
                Text("${GalleryLogic.fileSizeLabel(item.sizeBytes)}  |  ${item.width} × ${item.height}${resolutionClass?.let { "  |  $it" }.orEmpty()}", color = VaultSecondary)
                val details = buildList {
                    add(GalleryLogic.durationLabel(item.durationMs))
                    technicalInfo?.videoCodec?.let(::add)
                    technicalInfo?.audioCodec?.let(::add)
                    technicalInfo?.frameRate?.let { add("$it fps") }
                    technicalInfo?.bitrate?.let(::add)
                    technicalInfo?.colorSpace?.let(::add)
                    technicalInfo?.hdr?.let(::add)
                }.joinToString("  |  ")
                Text(details, color = VaultSecondary)
            } else {
                Text("${GalleryLogic.fileSizeLabel(item.sizeBytes)}  |  ${item.width} × ${item.height}  |  ${"%.1f".format(java.util.Locale.ROOT, megapixels)} MP", color = VaultSecondary)
                Text(item.mimeType, color = VaultSecondary)
                imageInfo?.camera?.let { Text("Camera  $it", color = VaultSecondary) }
                imageInfo?.lens?.let { Text("Lens  $it", color = VaultSecondary) }
                listOfNotNull(imageInfo?.exposure, imageInfo?.aperture, imageInfo?.iso, imageInfo?.focalLength)
                    .takeIf { it.isNotEmpty() }?.let { Text(it.joinToString("  |  "), color = VaultSecondary) }
                imageInfo?.colorSpace?.let { Text("Colour space  $it", color = VaultSecondary) }
            }
            val latitude = imageInfo?.latitude ?: item.latitude
            val longitude = imageInfo?.longitude ?: item.longitude
            if (latitude != null && longitude != null) Text("Location  $latitude, $longitude", color = VaultSecondary)
            if (!extendedMetadata.isNullOrEmpty()) {
                TextButton(onClick = { extendedMetadataVisible = !extendedMetadataVisible }) { Text(if (extendedMetadataVisible) "Hide extended metadata" else "Show extended metadata") }
                if (extendedMetadataVisible) Column(
                    Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    extendedMetadata.orEmpty().forEach { section ->
                        Text(section.name, style = MaterialTheme.typography.titleSmall)
                        section.values.forEach { (name, value) -> Text("$name  $value", color = VaultSecondary, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rating", color = VaultSecondary, modifier = Modifier.width(72.dp))
                (1..5).forEach { value ->
                    IconButton(onClick = {
                        rating = if (rating == value) 0 else value
                        onRating(rating)
                    }) {
                        Icon(
                            if (value <= rating) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                            "$value star rating",
                            tint = if (value <= rating) Color(0xFFFFD60A) else VaultSecondary,
                        )
                    }
                }
            }
            if (editingTag) {
                OutlinedTextField(tag, { tag = it.take(60) }, label = { Text("Tag") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.align(Alignment.End)) {
                    TextButton(onClick = { editingTag = false }) { Text("Cancel") }
                    TextButton(onClick = { onTag(tag.trim()); editingTag = false }) { Text("Save") }
                }
            } else TextButton(onClick = { editingTag = true }) { Text(if (tag.isBlank()) "Add tag" else "Tag: $tag") }
        }
    }
}

@Composable
private fun SecureDetailsSheet(item: SecureItem, onDismiss: () -> Unit, onEdit: () -> Unit, onTag: (String) -> Unit) {
    var editingTag by remember { mutableStateOf(false) }
    var tag by remember(item.id, item.tag) { mutableStateOf(item.tag) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = VaultSurface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(item.dateTakenMs)), style = MaterialTheme.typography.headlineMedium)
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text("Secure Gallery/${item.albumName}/${item.name}", color = VaultSecondary)
            Text(item.albumName, color = VaultSecondary)
            val megapixels = (item.width.toLong() * item.height.toLong()) / 1_000_000.0
            Text("${GalleryLogic.fileSizeLabel(item.sizeBytes)}   ${item.width} × ${item.height}   ${"%.1f".format(java.util.Locale.ROOT, megapixels)} MP", color = VaultSecondary)
            if (item.isVideo) Text("Duration ${GalleryLogic.durationLabel(item.durationMs)}", color = VaultSecondary)
            Text(item.mimeType, color = VaultSecondary)
            if (item.latitude != null && item.longitude != null) Text("${item.latitude}, ${item.longitude}", color = VaultSecondary)
            if (editingTag) {
                OutlinedTextField(tag, { tag = it.take(60) }, label = { Text("Tag") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.align(Alignment.End)) {
                    TextButton(onClick = { editingTag = false }) { Text("Cancel") }
                    TextButton(onClick = { onTag(tag.trim()); editingTag = false }) { Text("Save") }
                }
            } else TextButton(onClick = { editingTag = true }) { Text(if (tag.isBlank()) "Add tag" else "Tag: $tag") }
            HorizontalDivider(color = Color(0xFF3A3A3E))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}

@Composable
private fun SecurePhotoEditor(item: SecureItem, viewModel: SecureGalleryViewModel, launchMode: EditorLaunchMode, onBack: () -> Unit, onSaved: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, item.id) { value = viewModel.previewImage(item, Int.MAX_VALUE) }
    PhotoEditorScreen(item.name, bitmap, state.busy, onBack, launchMode, "secure://${item.id}") { edited, mode, exportOptions ->
        viewModel.saveEditedImage(item, edited, replaceOriginal = mode == EditorSaveMode.REPLACE, exportOptions = exportOptions)
        onSaved()
    }
}

@Composable
private fun SecureVideoEditor(item: SecureItem, viewModel: SecureGalleryViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val sourceUri = remember(item.id) { SecureMediaProvider.uri(context, item) }
    VideoEditorScreen(item.name, sourceUri, item.durationMs, accent = VaultSecure, onBack = onBack, projectSource = "secure://${item.id}") { output, result, mode ->
        viewModel.saveEditedVideo(item, output, result.width, result.height, result.durationMs, replaceOriginal = mode == VideoEditorSaveMode.REPLACE)
        onSaved()
    }
}

@Composable
private fun SecureVideoContactSheet(item: SecureItem, viewModel: SecureGalleryViewModel, modifier: Modifier = Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, item.id) {
        value = if (item.isVideo) viewModel.previewVideoContactSheet(item) else null
    }
    if (bitmap != null) androidx.compose.foundation.Image(
        bitmap = bitmap!!.asImageBitmap(),
        contentDescription = "Video frames",
        contentScale = ContentScale.FillBounds,
        modifier = modifier,
    ) else SecureTile(item, modifier = modifier, interactive = false, onOpen = {})
}

@Composable
private fun SecureAdjacentPreview(item: SecureItem, viewModel: SecureGalleryViewModel, chromeVisible: Boolean) {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        // Adjacent pages are precomposed for immediate finger tracking, so use the protected
        // 512 px thumbnail cache here. Decoding three full-resolution secure images at once made
        // the first paging gesture hitch and created unnecessary decrypted bitmap pressure.
        val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, item.id) {
            value = if (item.isVideo) viewModel.previewVideoThumbnail(item)
            else viewModel.previewImageThumbnail(item)
        }
        if (bitmap != null) androidx.compose.foundation.Image(
            bitmap!!.asImageBitmap(),
            item.name,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        ) else CircularProgressIndicator(color = VaultSecure)
        if (item.isVideo) SamsungAdjacentVideoChrome(
            durationMs = item.durationMs,
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SecureImage(
    item: SecureItem,
    viewModel: SecureGalleryViewModel,
    interactive: Boolean = true,
    onTap: () -> Unit = {},
    onZoomChanged: (Boolean) -> Unit = {},
) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, item.id) {
        value = viewModel.previewImage(item)
    }
    // Never recycle a bitmap while it is owned by Compose. Disposal and a render pass can
    // overlap, especially when the pager keeps adjacent pages precomposed. Recycling here caused
    // the repeatable "Canvas: trying to use a recycled bitmap" Secure Gallery crash.
    val zoomState = rememberZoomState()
    LaunchedEffect(zoomState, interactive) {
        if (interactive) snapshotFlow { zoomState.zoomable.transform.scaleX > zoomState.zoomable.minScale * 1.01f }
            .distinctUntilChanged()
            .collect(onZoomChanged)
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (bitmap == null) CircularProgressIndicator(color = VaultSecure)
        else if (interactive) {
            ZoomImage(
                painter = BitmapPainter(bitmap!!.asImageBitmap()),
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                zoomState = zoomState,
                onTap = { onTap() },
            )
        } else {
            androidx.compose.foundation.Image(
                bitmap!!.asImageBitmap(),
                item.name,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        if (isRawImage(item.mimeType, item.name)) {
            Text(
                "RAW preview",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp)
                    .background(Color(0x99000000), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun SecureVideo(
    item: SecureItem,
    viewModel: SecureGalleryViewModel,
    player: ExoPlayer,
    chromeVisible: Boolean,
    muted: Boolean,
    onChromeToggle: () -> Unit,
    onMutedChange: (Boolean) -> Unit,
    onZoomChanged: (Boolean) -> Unit,
) {
    SamsungInlineVideoPlayer(
        player = player,
        mediaKey = item.id,
        durationHintMs = item.durationMs,
        chromeVisible = chromeVisible,
        muted = muted,
        bottomPadding = SamsungViewerActionBarHeight.value.toInt(),
        onChromeToggle = onChromeToggle,
        onMutedChange = onMutedChange,
        onZoomChanged = onZoomChanged,
        onCaptureFrame = { viewModel.captureVideoFrame(item, player.currentPosition) },
    )
}

private fun launchApprovedSecureApp(context: Context, viewModel: SecureGalleryViewModel, item: SecureItem, packageName: String) {
    val store = ApprovedPlayerStore(context)
    if (!store.isApproved(packageName)) return viewModel.reportError("Approve this app in Secure settings first")
    val uri = SecureMediaProvider.uri(context, item)
    viewModel.authorizeExternalAccess(store.accessDurationMs())
    context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, item.mimeType)
        setPackage(packageName)
        clipData = android.content.ClipData.newRawUri(item.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType
        setPackage(packageName)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newRawUri(item.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val intent = when {
        viewIntent.resolveActivity(context.packageManager) != null -> viewIntent
        sendIntent.resolveActivity(context.packageManager) != null -> sendIntent
        else -> null
    }
    if (intent == null) {
        context.revokeUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        viewModel.reportError("${runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)) }.getOrDefault("This app")} does not accept ${if (item.isVideo) "videos" else "images"}")
    } else runCatching { context.startActivity(intent) }
        .onFailure {
            context.revokeUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.reportError("The approved app could not open this media")
        }
}

@Composable
private fun SecureSettings(
    state: SecureGalleryState,
    viewModel: SecureGalleryViewModel,
    biometricsAvailable: Boolean,
    onBiometricEnrollment: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var resetPrompt by remember { mutableStateOf(false) }
    var changePrompt by remember { mutableStateOf(false) }
    var recoveryPrompt by remember { mutableStateOf(false) }
    var recoveryPassphrase by remember { mutableStateOf("") }
    var recoveryConfirmation by remember { mutableStateOf("") }
    var pendingRecoveryPassphrase by remember { mutableStateOf<String?>(null) }
    var backupPrompt by remember { mutableStateOf(false) }
    var backupPassphrase by remember { mutableStateOf("") }
    var backupConfirmation by remember { mutableStateOf("") }
    var pendingBackupPassphrase by remember { mutableStateOf<String?>(null) }
    var restorePrompt by remember { mutableStateOf(false) }
    var restorePassphrase by remember { mutableStateOf("") }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var timeoutMenu by remember { mutableStateOf(false) }
    var unlockPolicyMenu by remember { mutableStateOf(false) }
    var gridMenu by remember { mutableStateOf(false) }
    val displayPrefs = remember { context.getSharedPreferences("secure-gallery-settings", 0) }
    val galleryPrefs = remember { context.getSharedPreferences("gallery-settings", 0) }
    var gridColumns by remember { mutableIntStateOf(displayPrefs.getInt("grid_columns", 4).coerceIn(3, 12)) }
    var protectScreenshots by remember { mutableStateOf(displayPrefs.getBoolean("protect_screenshots", false)) }
    var fullScreen by remember { mutableStateOf(galleryPrefs.getBoolean("full_screen_scroll", false)) }
    var externalPlayer by remember { mutableStateOf(galleryPrefs.getBoolean("external_player", false)) }
    var followAndroidAudioFocus by remember {
        mutableStateOf(galleryAudioFocusBehavior(context) == GalleryAudioFocusBehavior.FOLLOW_ANDROID)
    }
    var autoStories by remember { mutableStateOf(galleryPrefs.getBoolean("auto_stories", true)) }
    var essentialAlbums by remember { mutableStateOf(galleryPrefs.getBoolean("essential", true)) }
    var mergeAlbums by remember { mutableStateOf(galleryPrefs.getBoolean("merge_albums", true)) }
    var conflictPolicy by remember { mutableStateOf(transferConflictPolicy(context)) }
    var conflictPolicyMenu by remember { mutableStateOf(false) }
    var informationDialog by remember { mutableStateOf<String?>(null) }
    var diagnosticsOpen by remember { mutableStateOf(false) }
    var transferHistoryOpen by remember { mutableStateOf(false) }
    var launcherVisible by remember { mutableStateOf(isSecureLauncherVisible(context)) }
    var typed by remember { mutableStateOf("") }
    var currentSecret by remember { mutableStateOf("") }
    var newSecret by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var storageConversion by remember { mutableStateOf<SecureStoragePolicy?>(null) }
    val playerStore = remember { ApprovedPlayerStore(context) }
    val accessHistory = remember { SecureAccessHistoryStore(context) }
    var approvedPackages by remember { mutableStateOf(playerStore.packages()) }
    var playerDurationMenu by remember { mutableStateOf(false) }
    var appPickerOpen by remember { mutableStateOf(false) }
    var appSearch by remember { mutableStateOf("") }
    val availableApps = remember {
        @Suppress("DEPRECATION")
        context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.enabled && it.packageName != context.packageName }
            .map { app -> app.packageName to context.packageManager.getApplicationLabel(app).toString() }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
            .toList()
    }
    val recoveryKeyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.vaultgallery.recovery-key+json"),
    ) { destination ->
        val passphrase = pendingRecoveryPassphrase
        pendingRecoveryPassphrase = null
        if (destination != null && passphrase != null) viewModel.exportRecoveryKey(destination, passphrase)
    }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.vaultgallery.backup"),
    ) { destination ->
        val passphrase = pendingBackupPassphrase
        pendingBackupPassphrase = null
        if (destination != null && passphrase != null) viewModel.exportEncryptedBackup(destination, passphrase)
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { source ->
        if (source != null) {
            pendingRestoreUri = source
            restorePassphrase = ""
            restorePrompt = true
        }
    }
    if (diagnosticsOpen) {
        DeviceDiagnosticsScreen(onBack = { diagnosticsOpen = false })
        return
    }
    if (transferHistoryOpen) {
        TransferHistoryScreen(onBack = { transferHistoryOpen = false })
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SecureToolbar("Secure settings", onBack)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 18.dp)) }
        Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Unlock and privacy", style = MaterialTheme.typography.titleLarge)
                SecureSettingSwitch(
                    title = "Biometric unlock",
                    subtitle = if (biometricsAvailable) "Use your fingerprint or face to unlock Secure Gallery" else "Set up a strong biometric in Android settings first",
                    checked = state.biometricEnabled,
                    enabled = biometricsAvailable,
                ) { enabled -> if (enabled) onBiometricEnrollment() else viewModel.disableBiometric() }
                Box {
                    val policyLabel = when (state.unlockPolicy) {
                        SecureUnlockPolicy.BIOMETRIC_OR_PIN -> "Biometric or PIN"
                        SecureUnlockPolicy.BIOMETRIC_ONLY -> "Biometric only"
                        SecureUnlockPolicy.PIN_ONLY -> "PIN only"
                    }
                    SecureSettingLink("Unlock method", policyLabel) { unlockPolicyMenu = true }
                    DropdownMenu(expanded = unlockPolicyMenu, onDismissRequest = { unlockPolicyMenu = false }) {
                        listOf(
                            SecureUnlockPolicy.BIOMETRIC_OR_PIN to "Biometric or PIN",
                            SecureUnlockPolicy.BIOMETRIC_ONLY to "Biometric only",
                            SecureUnlockPolicy.PIN_ONLY to "PIN only",
                        ).forEach { (policy, label) ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(label)
                                        if (policy == SecureUnlockPolicy.BIOMETRIC_ONLY) {
                                            Text("PIN remains the recovery credential but is hidden at unlock", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                },
                                enabled = policy != SecureUnlockPolicy.BIOMETRIC_ONLY || state.biometricEnabled,
                                onClick = { viewModel.setUnlockPolicy(policy); unlockPolicyMenu = false },
                            )
                        }
                    }
                }
                Box {
                    SecureSettingLink("Auto-lock", lockTimeoutLabel(state.lockTimeoutMs)) { timeoutMenu = true }
                    DropdownMenu(expanded = timeoutMenu, onDismissRequest = { timeoutMenu = false }) {
                        listOf(0L to "Immediately", 30_000L to "30 seconds", 60_000L to "1 minute", 300_000L to "5 minutes", 900_000L to "15 minutes").forEach { (millis, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setLockTimeout(millis); timeoutMenu = false })
                        }
                    }
                }
                SecureSettingSwitch(
                    "Lock when app goes to background",
                    "Apply the selected auto-lock delay after leaving Secure Gallery",
                    state.lockOnBackground,
                    true,
                    viewModel::setLockOnBackground,
                )
                SecureSettingSwitch(
                    "Lock when screen turns off",
                    "Lock immediately when Android switches the display off",
                    state.lockOnScreenOff,
                    true,
                    viewModel::setLockOnScreenOff,
                )
                SecureSettingSwitch(
                    "Protect screenshots and screen recording",
                    "Optional. Off by default so screenshots work unless you explicitly protect the vault window.",
                    protectScreenshots,
                    true,
                ) { enabled ->
                    protectScreenshots = enabled
                    displayPrefs.edit().putBoolean("protect_screenshots", enabled).apply()
                    (context as? SecureGalleryActivity)?.applyScreenshotProtection(enabled)
                }
                SecureSettingSwitch(
                    "Show separate launcher icon",
                    "Gallery can still open the vault when this icon is hidden",
                    launcherVisible,
                    true,
                ) { visible -> setSecureLauncherVisible(context, visible); launcherVisible = visible }
                Button(onClick = { changePrompt = true }, modifier = Modifier.fillMaxWidth()) { Text("Change four-digit PIN") }
                OutlinedButton(onClick = { recoveryPassphrase = ""; recoveryConfirmation = ""; recoveryPrompt = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Export encrypted recovery key")
                }
                Text("The recovery-key file contains no photos. Store it separately from your phone and remember its passphrase.", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Button(
                    onClick = { backupPassphrase = ""; backupConfirmation = ""; backupPrompt = true },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Back up Secure Gallery") }
                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("application/vnd.vaultgallery.backup", "application/octet-stream")) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Restore encrypted backup") }
                Text("Backups contain media and metadata, are protected by a separate 12-character passphrase, and merge without replacing existing items.", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Secure storage", style = MaterialTheme.typography.titleLarge)
                Text("Choose the protection used for new imports. Existing items never change unless you explicitly convert them.", color = VaultSecondary)
                SecureSettingSwitch(
                    "Encrypt new imports",
                    if (state.defaultStoragePolicy == SecureStoragePolicy.ENCRYPTED) "Maximum privacy; approved players receive temporary access" else "Off; new files stay locked in Secure Gallery for faster playback",
                    state.defaultStoragePolicy == SecureStoragePolicy.ENCRYPTED,
                    true,
                ) { encrypted -> viewModel.setDefaultStoragePolicy(if (encrypted) SecureStoragePolicy.ENCRYPTED else SecureStoragePolicy.LOCKED_ONLY) }
                HorizontalDivider()
                Text("${state.encryptedCount} encrypted • ${state.lockedOnlyCount} locked-only", color = VaultSecondary)
                if (state.lockedOnlyCount > 0) Button(onClick = { storageConversion = SecureStoragePolicy.ENCRYPTED }, modifier = Modifier.fillMaxWidth()) {
                    Text("Encrypt all locked-only items")
                }
                if (state.encryptedCount > 0) Button(onClick = { storageConversion = SecureStoragePolicy.LOCKED_ONLY }, modifier = Modifier.fillMaxWidth()) {
                    Text("Convert all to locked-only")
                }
                Text("Locked-only media is PIN/biometric gated in the app but is not encrypted at rest. Secure files live in Documents/VaultGallery/SecureVault, survive uninstall, and stay hidden from normal media scanning. Any approved external player can retain media it opens.", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Approved external apps", style = MaterialTheme.typography.titleLarge)
                Text("You decide which installed apps may receive a temporary, read-only secure-media stream. No app receives the vault key.", color = VaultSecondary)
                Text(
                    if (approvedPackages.isEmpty()) "No apps approved" else "${approvedPackages.size} app${if (approvedPackages.size == 1) "" else "s"} approved",
                    color = if (approvedPackages.isEmpty()) VaultSecondary else VaultSecure,
                    style = MaterialTheme.typography.titleSmall,
                )
                approvedPackages.take(4).forEach { packageName ->
                    val label = availableApps.firstOrNull { it.first == packageName }?.second ?: packageName
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Check, null, tint = VaultSecure, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (approvedPackages.size > 4) Text("+ ${approvedPackages.size - 4} more", color = VaultSecondary)
                OutlinedButton(onClick = { appSearch = ""; appPickerOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose apps")
                }
                Box {
                    val durationLabel = when (playerStore.accessDurationMs()) { 60_000L -> "1 minute"; 5 * 60_000L -> "5 minutes"; 30 * 60_000L -> "30 minutes"; 60 * 60_000L -> "1 hour"; else -> "15 minutes" }
                    SecureSettingLink("Access after leaving Gallery", durationLabel) { playerDurationMenu = true }
                    DropdownMenu(expanded = playerDurationMenu, onDismissRequest = { playerDurationMenu = false }) {
                        listOf(60_000L to "1 minute", 5 * 60_000L to "5 minutes", 15 * 60_000L to "15 minutes", 30 * 60_000L to "30 minutes", 60 * 60_000L to "1 hour").forEach { (duration, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { playerStore.setAccessDurationMs(duration); playerDurationMenu = false })
                        }
                    }
                }
                SecureSettingLink("External access history", "Recent approved and blocked reads") {
                    val events = accessHistory.list()
                    val formatter = java.text.SimpleDateFormat("d MMM, h:mm:ss a", java.util.Locale.getDefault())
                    informationDialog = if (events.isEmpty()) {
                        "No external app has requested secure media yet."
                    } else events.take(30).joinToString("\n") { event ->
                        val label = runCatching {
                            val application = context.packageManager.getApplicationInfo(event.packageName, 0)
                            context.packageManager.getApplicationLabel(application).toString()
                        }.getOrDefault(event.packageName)
                        "${formatter.format(java.util.Date(event.timestampMs))}  ${if (event.allowed) "Allowed" else "Blocked"}  $label"
                    }
                }
                Text("Lock now immediately revokes future reads. An external app may retain media it has already buffered or copied.", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Viewing", style = MaterialTheme.typography.titleLarge)
                Box {
                    SecureSettingLink("Grid layout", "$gridColumns columns") { gridMenu = true }
                    DropdownMenu(expanded = gridMenu, onDismissRequest = { gridMenu = false }) {
                        (3..12).forEach { columns -> DropdownMenuItem(text = { Text("$columns columns") }, onClick = {
                            gridColumns = columns; displayPrefs.edit().putInt("grid_columns", columns).apply(); gridMenu = false
                        }) }
                    }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Shared Gallery behavior", style = MaterialTheme.typography.titleLarge)
                SecureSettingSwitch("Full screen scrolling", "Use immersive edge-to-edge viewing in both galleries", fullScreen, true) {
                    fullScreen = it; galleryPrefs.edit().putBoolean("full_screen_scroll", it).apply()
                }
                SecureSettingSwitch("Open videos in Video Player", "Use the standalone player when requested", externalPlayer, true) {
                    externalPlayer = it; galleryPrefs.edit().putBoolean("external_player", it).apply()
                }
                SecureSettingSwitch("Pause for calls and other audio", "When off, Gallery keeps playing through transient audio-focus changes", followAndroidAudioFocus, true) {
                    followAndroidAudioFocus = it
                    galleryPrefs.edit().putString(
                        "audio_focus_behavior",
                        if (it) GalleryAudioFocusBehavior.FOLLOW_ANDROID.name else GalleryAudioFocusBehavior.CONTINUE_PLAYBACK.name,
                    ).apply()
                }
                SecureSettingSwitch("Auto create stories", "Create local monthly stories", autoStories, true) {
                    autoStories = it; galleryPrefs.edit().putBoolean("auto_stories", it).apply()
                }
                SecureSettingSwitch("Select essential albums", "Limit the main Albums tab to chosen folders", essentialAlbums, true) {
                    essentialAlbums = it; galleryPrefs.edit().putBoolean("essential", it).apply()
                }
                SecureSettingSwitch("Merge albums", "Merge matching folder names in the main Gallery", mergeAlbums, true) {
                    mergeAlbums = it; galleryPrefs.edit().putBoolean("merge_albums", it).apply()
                }
                Box {
                    SecureSettingLink("Matching file names", conflictPolicy.label) { conflictPolicyMenu = true }
                    DropdownMenu(expanded = conflictPolicyMenu, onDismissRequest = { conflictPolicyMenu = false }) {
                        TransferConflictPolicy.entries.forEach { policy ->
                            DropdownMenuItem(text = { Text(policy.label) }, onClick = {
                                conflictPolicy = policy
                                galleryPrefs.edit().putString("transfer_conflict_policy", policy.name).apply()
                                conflictPolicyMenu = false
                            })
                        }
                    }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("Privacy and information", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
                SecureSettingLink("Privacy Policy", "Local-first processing") { informationDialog = "Vault Gallery processes media locally. OCR runs on device. Secure media stays in app-private storage; items you explicitly encrypt are decrypted only for an authorized view, edit, share, copy, or move." }
                SecureSettingLink("Permissions", "Review Android access") {
                    context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                }
                SecureSettingLink("Open-source licenses", "Libraries used by the app") { informationDialog = "ML Kit — Google terms\nOpenCV — Apache 2.0\nPhotoEditor — MIT\nMedia3 — Apache 2.0\nCoil — Apache 2.0\nZoomImage — Apache 2.0\nTink — Apache 2.0\nSQLCipher — BSD-style\nargon2kt — Apache 2.0" }
                SecureSettingLink("Device and media diagnostics", "Playback, export, AI and storage") { diagnosticsOpen = true }
                SecureSettingLink("Transfer history", "Per-item success and failure reports") { transferHistoryOpen = true }
                SecureSettingLink("Before uninstalling or resetting", "Data-safety checklist") {
                    informationDialog = "Before removing Vault Gallery:\n\n1. Create an encrypted Secure Gallery backup and verify you remember its separate passphrase.\n2. Keep an independent copy of irreplaceable media.\n3. Finish or cancel every active transfer.\n4. Remember that approved-app choices, notification settings, and other app preferences are removed by Android during uninstall.\n5. Secure media under Documents/VaultGallery/SecureVault is user-owned and remains on the device, but do not rely on that as your only backup.\n\nAndroid does not let an app intercept or display its own warning inside the system uninstall confirmation."
                }
                SecureSettingLink("About Gallery", BuildConfig.VERSION_NAME) { informationDialog = "Vault Gallery ${BuildConfig.VERSION_NAME}\nA local-first Gallery and locked Secure Gallery with optional encryption, shared viewers and editors, slide selection, OCR, document scanning, creation tools and background transfers." }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("Protection status", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Persistent user-owned storage • Optional encryption • Device-bound keys • Timed auto-lock • " +
                        if (protectScreenshots) "Screenshots protected" else "Screenshots allowed",
                    color = VaultSecondary,
                )
                Button(onClick = viewModel::lock, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Lock, null); Spacer(Modifier.width(8.dp)); Text("Lock now") }
                Button(onClick = {
                    viewModel.clearTemporaryFiles()
                    Toast.makeText(context, "Temporary share files cleared", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.fillMaxWidth()) { Text("Clear temporary share files") }
                Button(
                    onClick = viewModel::scanIntegrity,
                    enabled = !state.integrityScanning,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.integrityScanning) "Checking every item…" else "Check vault integrity") }
                OutlinedButton(
                    onClick = viewModel::repairVaultSafely,
                    enabled = !state.busy && !state.integrityScanning,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Safe repair") }
                Text(
                    "Integrity check authenticates encrypted streams and verifies lengths. Safe repair only removes abandoned partial files and rebuilds metadata; it never deletes orphaned media.",
                    color = VaultSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { resetPrompt = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset Secure Gallery", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    informationDialog?.let { message -> AlertDialog(
        onDismissRequest = { informationDialog = null },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = { informationDialog = null }) { Text("Done") } },
    ) }
    state.integrityReport?.let { report ->
        AlertDialog(
            onDismissRequest = viewModel::dismissIntegrityReport,
            title = { Text(if (report.healthy) "Vault is healthy" else "Vault integrity report") },
            text = {
                Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${report.verifiedItems} of ${report.checkedItems} items verified")
                    report.issues.forEach { issue ->
                        Text("${issue.itemName}: ${issue.problem}", color = MaterialTheme.colorScheme.error)
                    }
                    if (report.orphanFiles.isNotEmpty()) {
                        Text("Preserved orphan files", style = MaterialTheme.typography.titleMedium)
                        report.orphanFiles.forEach { Text(it, color = VaultSecondary, style = MaterialTheme.typography.bodySmall) }
                    }
                    if (!report.healthy) Text("No source or orphan was deleted. Run Safe repair for recoverable metadata and abandoned partial files.", color = VaultSecondary)
                }
            },
            confirmButton = { TextButton(onClick = viewModel::dismissIntegrityReport) { Text("Done") } },
        )
    }
    if (appPickerOpen) {
        val filteredApps = availableApps.filter { (packageName, label) ->
            appSearch.isBlank() || label.contains(appSearch, ignoreCase = true) || packageName.contains(appSearch, ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { appPickerOpen = false },
            title = { Column { Text("Allow secure-media access"); Text("Every app starts blocked", color = VaultSecondary, style = MaterialTheme.typography.bodySmall) } },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = appSearch,
                        onValueChange = { appSearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        label = { Text("Search installed apps") },
                    )
                    Text("Approved apps can only open an item after you explicitly choose them from that item's menu.", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        items(filteredApps.size, key = { filteredApps[it].first }) { index ->
                            val (packageName, label) = filteredApps[index]
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                                    playerStore.setApproved(packageName, packageName !in approvedPackages)
                                    approvedPackages = playerStore.packages()
                                }.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = packageName in approvedPackages,
                                    onCheckedChange = { approved ->
                                        playerStore.setApproved(packageName, approved)
                                        approvedPackages = playerStore.packages()
                                    },
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(packageName, color = VaultSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { appPickerOpen = false }) { Text("Done") } },
            dismissButton = {
                if (approvedPackages.isNotEmpty()) TextButton(onClick = {
                    approvedPackages.forEach { playerStore.setApproved(it, false) }
                    approvedPackages = playerStore.packages()
                }) { Text("Block all") }
            },
        )
    }
    storageConversion?.let { target ->
        AlertDialog(
            onDismissRequest = { storageConversion = null },
            title = { Text(if (target == SecureStoragePolicy.ENCRYPTED) "Encrypt secure media?" else "Use locked-only storage?") },
            text = { Text(if (target == SecureStoragePolicy.ENCRYPTED) "Each locked-only item will be encrypted and verified before its plaintext original is removed. The encrypted file remains in the uninstall-safe persistent vault." else "Encrypted originals will only be removed after their locked-only replacements are verified. Files remain in the uninstall-safe persistent vault but are no longer encrypted at rest.") },
            dismissButton = { TextButton(onClick = { storageConversion = null }) { Text("Cancel") } },
            confirmButton = { TextButton(onClick = {
                val targetItems = state.items.filter { it.storagePolicy != target }
                storageConversion = null
                viewModel.convertStoragePolicy(targetItems, target)
            }) { Text("Convert") } },
        )
    }
    if (changePrompt) AlertDialog(
        onDismissRequest = { changePrompt = false },
        title = { Text("Change four-digit PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(currentSecret, { currentSecret = it.take(64) }, label = { Text("Current PIN") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                OutlinedTextField(newSecret, { newSecret = it.filter(Char::isDigit).take(4) }, label = { Text("New 4-digit PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
                OutlinedTextField(confirmation, { confirmation = it.filter(Char::isDigit).take(4) }, label = { Text("Confirm new PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
            }
        },
        dismissButton = { TextButton(onClick = { changePrompt = false }) { Text("Cancel") } },
        confirmButton = {
            TextButton(enabled = currentSecret.length >= 4 && newSecret.length == 4 && newSecret == confirmation, onClick = {
                viewModel.changeSecret(currentSecret, newSecret, confirmation)
                currentSecret = ""; newSecret = ""; confirmation = ""; changePrompt = false
            }) { Text("Change") }
        },
    )

    if (recoveryPrompt) AlertDialog(
        onDismissRequest = { recoveryPrompt = false },
        title = { Text("Protect recovery key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose a separate passphrase with at least 12 characters. This file can recover the vault encryption key, so do not store it beside an unlocked phone.")
                OutlinedTextField(
                    recoveryPassphrase,
                    { recoveryPassphrase = it.take(128) },
                    label = { Text("Recovery passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                OutlinedTextField(
                    recoveryConfirmation,
                    { recoveryConfirmation = it.take(128) },
                    label = { Text("Confirm passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                if (recoveryConfirmation.isNotEmpty() && recoveryPassphrase != recoveryConfirmation) {
                    Text("Passphrases do not match", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = { TextButton(onClick = { recoveryPrompt = false }) { Text("Cancel") } },
        confirmButton = {
            TextButton(
                enabled = recoveryPassphrase.length >= 12 && recoveryPassphrase == recoveryConfirmation,
                onClick = {
                    pendingRecoveryPassphrase = recoveryPassphrase
                    recoveryPrompt = false
                    recoveryPassphrase = ""
                    recoveryConfirmation = ""
                    recoveryKeyLauncher.launch("vault-gallery-recovery-${System.currentTimeMillis()}.vgrk")
                },
            ) { Text("Choose location") }
        },
    )
    if (backupPrompt) AlertDialog(
        onDismissRequest = { backupPrompt = false },
        title = { Text("Protect encrypted backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose a separate passphrase with at least 12 characters. Losing it makes the backup unrecoverable.")
                OutlinedTextField(
                    backupPassphrase,
                    { backupPassphrase = it.take(128) },
                    label = { Text("Backup passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                OutlinedTextField(
                    backupConfirmation,
                    { backupConfirmation = it.take(128) },
                    label = { Text("Confirm passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                if (backupConfirmation.isNotEmpty() && backupPassphrase != backupConfirmation) {
                    Text("Passphrases do not match", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = { TextButton(onClick = { backupPrompt = false }) { Text("Cancel") } },
        confirmButton = {
            TextButton(
                enabled = backupPassphrase.length >= 12 && backupPassphrase == backupConfirmation,
                onClick = {
                    pendingBackupPassphrase = backupPassphrase
                    backupPrompt = false
                    backupPassphrase = ""
                    backupConfirmation = ""
                    backupLauncher.launch("vault-gallery-backup-${System.currentTimeMillis()}.vgb")
                },
            ) { Text("Choose location") }
        },
    )
    if (restorePrompt) AlertDialog(
        onDismissRequest = { restorePrompt = false; pendingRestoreUri = null; restorePassphrase = "" },
        title = { Text("Restore encrypted backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter the passphrase used when this backup was created. Existing Secure Gallery items will not be replaced.")
                OutlinedTextField(
                    restorePassphrase,
                    { restorePassphrase = it.take(128) },
                    label = { Text("Backup passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { restorePrompt = false; pendingRestoreUri = null; restorePassphrase = "" }) { Text("Cancel") }
        },
        confirmButton = {
            TextButton(
                enabled = restorePassphrase.length >= 12 && pendingRestoreUri != null,
                onClick = {
                    val source = pendingRestoreUri
                    val passphrase = restorePassphrase
                    restorePrompt = false
                    pendingRestoreUri = null
                    restorePassphrase = ""
                    if (source != null) viewModel.restoreEncryptedBackup(source, passphrase)
                },
            ) { Text("Restore") }
        },
    )
    if (resetPrompt) AlertDialog(
        onDismissRequest = { resetPrompt = false; typed = "" }, title = { Text("Reset encrypted vault?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("All encrypted media and keys will be lost. Type DELETE to confirm.")
                OutlinedTextField(typed, { typed = it }, label = { Text("DELETE") }, singleLine = true)
            }
        },
        dismissButton = { TextButton(onClick = { resetPrompt = false; typed = "" }) { Text("Cancel") } },
        confirmButton = { TextButton(enabled = typed == "DELETE", onClick = { resetPrompt = false; viewModel.reset() }) { Text("Reset") } },
    )
}

@Composable
private fun SecureSettingSwitch(title: String, subtitle: String, checked: Boolean, enabled: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = VaultSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            enabled = enabled,
            modifier = Modifier.semantics {
                contentDescription = title
                stateDescription = when {
                    !enabled -> if (checked) "On, unavailable" else "Off, unavailable"
                    checked -> "On"
                    else -> "Off"
                }
            },
        )
    }
}

@Composable
private fun SecureSettingLink(title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, color = VaultSecondary)
    }
}

private fun lockTimeoutLabel(timeoutMs: Long): String = when (timeoutMs) {
    0L -> "Immediately"
    30_000L -> "30 seconds"
    60_000L -> "1 minute"
    300_000L -> "5 minutes"
    else -> "15 minutes"
}

internal fun isSecureLauncherVisible(context: Context): Boolean {
    val component = secureLauncherComponent(context)
    return context.packageManager.getComponentEnabledSetting(component) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
}

internal fun setSecureLauncherVisible(context: Context, visible: Boolean) {
    val component = secureLauncherComponent(context)
    context.packageManager.setComponentEnabledSetting(
        component,
        if (visible) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP,
    )
    Toast.makeText(context, if (visible) "Secure Gallery icon shown" else "Secure Gallery icon hidden", Toast.LENGTH_SHORT).show()
}

/** Manifest component classes are resolved from the Gradle namespace, not the debug applicationId. */
private fun secureLauncherComponent(context: Context): ComponentName = ComponentName(
    context.packageName,
    "${SecureGalleryActivity::class.java.name.substringBeforeLast('.')}.SecureGalleryLauncher",
)

@Composable
private fun SecureTrashScreen(state: SecureGalleryState, viewModel: SecureGalleryViewModel, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<SecureItem?>(null) }
    var emptyPrompt by remember { mutableStateOf(false) }
    val columns = remember { mutableIntStateOf(4) }
    Column(Modifier.fillMaxSize()) {
        SecureToolbar("Recycle bin", onBack) {
            if (state.trashItems.isNotEmpty()) TextButton(onClick = { emptyPrompt = true }) { Text("Empty") }
        }
        Text("Items are deleted automatically after 30 days.", color = VaultSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (state.trashItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Recycle bin is empty", color = VaultSecondary) }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns.intValue),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.pinchToResizeGrid(columns, 3, 12),
            ) { items(state.trashItems, key = { it.id }) { SecureTile(it) { item -> selected = item } } }
        }
    }
    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(item.name) },
            text = { Text("Restore this secure item or delete it permanently?") },
            dismissButton = { TextButton(onClick = { selected = null; viewModel.deletePermanently(item) }) { Text("Delete permanently", color = MaterialTheme.colorScheme.error) } },
            confirmButton = { TextButton(onClick = { selected = null; viewModel.restore(item) }) { Text("Restore") } },
        )
    }
    if (emptyPrompt) AlertDialog(
        onDismissRequest = { emptyPrompt = false },
        title = { Text("Empty recycle bin?") },
        text = { Text("All ${state.trashItems.size} secure items will be permanently deleted. This cannot be undone.") },
        dismissButton = { TextButton(onClick = { emptyPrompt = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { emptyPrompt = false; viewModel.emptyTrash() }) { Text("Delete all", color = MaterialTheme.colorScheme.error) } },
    )
}
