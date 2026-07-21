@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.danyal.vaultgallery

import android.content.Intent
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.danyal.vaultgallery.security.SecureItem
import com.danyal.vaultgallery.ui.VaultBackground
import com.danyal.vaultgallery.ui.VaultPrimary
import com.danyal.vaultgallery.ui.VaultRaised
import com.danyal.vaultgallery.ui.VaultSecondary
import com.danyal.vaultgallery.ui.VaultSecure
import com.danyal.vaultgallery.ui.VaultSurface
import com.danyal.vaultgallery.ui.VaultTheme
import kotlinx.coroutines.launch

class SecureGalleryActivity : FragmentActivity() {
    private val secureViewModel by viewModels<SecureGalleryViewModel>()
    private var biometricInProgress = false
    private var biometricEnrollment = false

    private val biometricPrompt by lazy {
        BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    biometricInProgress = false
                    if (biometricEnrollment) secureViewModel.completeBiometricEnrollment()
                    else secureViewModel.completeBiometricUnlock()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val incoming = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra("import_uris", Uri::class.java).orEmpty()
        } else {
            @Suppress("DEPRECATION") intent.getParcelableArrayListExtra<Uri>("import_uris").orEmpty()
        }
        secureViewModel.queueImport(incoming)
        setContent {
            VaultTheme {
                Box(Modifier.fillMaxSize()) {
                    SecureGalleryApp(
                        viewModel = secureViewModel,
                        onShare = ::shareSecure,
                        biometricsAvailable = biometricsAvailable(),
                        onBiometricUnlock = { showBiometricPrompt(enrollment = false) },
                        onBiometricEnrollment = { showBiometricPrompt(enrollment = true) },
                    )
                }
            }
        }
    }

    override fun onPause() {
        if (!biometricInProgress) secureViewModel.onBackgrounded()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        secureViewModel.onForegrounded()
    }

    private fun biometricsAvailable(): Boolean = BiometricManager.from(this).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG,
    ) == BiometricManager.BIOMETRIC_SUCCESS

    private fun showBiometricPrompt(enrollment: Boolean) {
        if (!biometricsAvailable()) {
            secureViewModel.reportError("Set up a fingerprint or strong face unlock in Android settings first")
            return
        }
        val prepared = if (enrollment) secureViewModel.prepareBiometricEnrollment() else secureViewModel.prepareBiometricUnlock()
        if (!prepared) return
        biometricEnrollment = enrollment
        biometricInProgress = true
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (enrollment) "Enable biometric unlock" else "Unlock Secure Gallery")
            .setSubtitle(if (enrollment) "Confirm your identity to protect the vault key" else "Use biometrics or your device screen lock")
            .apply {
                if (enrollment || Build.VERSION.SDK_INT < 30) {
                    setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    setNegativeButtonText(if (enrollment) "Not now" else "Use vault PIN")
                } else {
                    setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                }
            }
            .build()
        biometricPrompt.authenticate(info)
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
}

private enum class SecureTab { PICTURES, ALBUMS, STORIES, MENU }

@Composable
private fun SecureGalleryApp(
    viewModel: SecureGalleryViewModel,
    onShare: (SecureItem, java.io.File) -> Unit,
    biometricsAvailable: Boolean,
    onBiometricUnlock: () -> Unit,
    onBiometricEnrollment: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(SecureTab.PICTURES) }
    var viewer by remember { mutableStateOf<SecureItem?>(null) }
    var settings by remember { mutableStateOf(false) }
    var trashOpen by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(100)) { uris -> viewModel.import(uris) }
    LaunchedEffect(state.unlocked) { if (!state.unlocked) { viewer = null; settings = false; tab = SecureTab.PICTURES } }
    BackHandler(state.unlocked && (viewer != null || settings || trashOpen)) {
        when {
            viewer != null -> viewer = null
            settings -> settings = false
            trashOpen -> trashOpen = false
        }
    }

    if (!state.unlocked) {
        SecureAuthScreen(state, viewModel::setup, viewModel::unlock, biometricsAvailable, onBiometricUnlock)
        return
    }
    if (viewer != null) {
        SecureViewer(viewer!!, viewModel, onBack = { viewer = null }, onShare = onShare, onDelete = { viewModel.delete(viewer!!); viewer = null })
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
        bottomBar = { SecureBottomNavigation(tab) { tab = it } },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(VaultBackground)) {
            when (tab) {
                SecureTab.PICTURES -> SecurePictures(
                    state,
                    onImport = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    },
                    onOpen = { viewer = it },
                )
                SecureTab.ALBUMS -> SecureAlbums(state.items) { tab = SecureTab.PICTURES }
                SecureTab.STORIES -> SecureStories(state.items) { viewer = it }
                SecureTab.MENU -> SecureMenu(
                    onImport = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    },
                    onSettings = { settings = true },
                    onTrash = { trashOpen = true },
                    onLock = viewModel::lock,
                )
            }
            if (state.busy) {
                Box(Modifier.fillMaxSize().background(Color(0xAA000000)), contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = VaultRaised), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            CircularProgressIndicator(color = VaultSecure)
                            Text(state.progress ?: "Working securely")
                        }
                    }
                }
            }
        }
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
    LaunchedEffect(state.configured, state.biometricEnabled, biometricsAvailable) {
        if (state.configured && state.biometricEnabled && biometricsAvailable && !autoBiometricAttempted) {
            autoBiometricAttempted = true
            onBiometricUnlock()
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
            if (state.configured) "Enter your PIN or passphrase. Secure content stays encrypted until unlock."
            else "Choose at least six characters. There is no password reset; losing it makes this vault inaccessible.",
            color = VaultSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            secret, { secret = it },
            label = { Text(if (state.configured) "PIN or passphrase" else "New PIN or passphrase") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!state.configured) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                confirmation, { confirmation = it }, label = { Text("Confirm") },
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = { if (state.configured) onUnlock(secret) else onSetup(secret, confirmation) },
            enabled = !state.busy && secret.length >= 6,
            modifier = Modifier.fillMaxWidth(),
        ) { if (state.busy) CircularProgressIndicator(Modifier.size(22.dp)) else Text(if (state.configured) "Unlock" else "Create encrypted vault") }
        if (state.configured && state.biometricEnabled && biometricsAvailable) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onBiometricUnlock, modifier = Modifier.fillMaxWidth()) { Text("Unlock with fingerprint or face") }
        }
        Spacer(Modifier.height(16.dp))
        Text("Application-level protection; not an operating-system container.", color = VaultSecondary, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SecureToolbar(title: String, back: (() -> Unit)? = null, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().height(92.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        Icon(Icons.Outlined.Lock, null, tint = VaultSecure, modifier = Modifier.padding(horizontal = 8.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
private fun SecurePictures(state: SecureGalleryState, onImport: () -> Unit, onOpen: (SecureItem) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SecureToolbar("Secure Gallery") { IconButton(onClick = onImport) { Icon(Icons.Outlined.Upload, "Import media") } }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
        if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(28.dp)) {
                    Icon(Icons.Outlined.Lock, null, Modifier.size(58.dp), tint = VaultSecure)
                    Text("Your vault is empty", style = MaterialTheme.typography.headlineMedium)
                    Text("Import photos or videos. Originals remain unchanged unless you delete them separately from the public gallery.", color = VaultSecondary, textAlign = TextAlign.Center)
                    Button(onClick = onImport) { Icon(Icons.Outlined.Upload, null); Spacer(Modifier.width(8.dp)); Text("Import media") }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4), contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp),
            ) { items(state.items, key = { it.id }) { SecureTile(it, onOpen) } }
        }
    }
}

@Composable
private fun SecureTile(item: SecureItem, onOpen: (SecureItem) -> Unit) {
    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(2.dp)).background(VaultSurface).clickable { onOpen(item) }, contentAlignment = Alignment.Center) {
        if (item.isVideo) {
            Icon(Icons.Outlined.PlayArrow, item.name, Modifier.size(36.dp), tint = VaultSecure)
        } else {
            val viewModel: SecureGalleryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, item.id) {
                value = viewModel.previewBytes(item)?.let { bytes -> try {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    val sample = generateSequence(1) { it * 2 }.first { bounds.outWidth / it <= 512 && bounds.outHeight / it <= 512 }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                } finally { bytes.fill(0) } }
            }
            if (bitmap != null) androidx.compose.foundation.Image(bitmap!!.asImageBitmap(), item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else CircularProgressIndicator(Modifier.size(24.dp), color = VaultSecure)
        }
        Icon(Icons.Outlined.Lock, "Encrypted", Modifier.align(Alignment.BottomEnd).padding(5.dp).size(16.dp), tint = VaultSecure)
    }
}

@Composable
private fun SecureAlbums(items: List<SecureItem>, onOpen: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SecureToolbar("Secure albums")
        if (items.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No secure albums", color = VaultSecondary) }
        else Column(Modifier.padding(14.dp).width(145.dp).clickable(onClick = onOpen)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(VaultSurface), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(52.dp), tint = VaultSecure)
            }
            Text("Imported", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Text(items.size.toString(), color = VaultSecondary)
        }
    }
}

@Composable
private fun SecureStories(items: List<SecureItem>, onOpen: (SecureItem) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SecureToolbar("Secure stories")
        if (items.size < 2) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Movie, null, Modifier.size(48.dp), tint = VaultSecondary)
                Text("No secure stories", style = MaterialTheme.typography.headlineMedium, color = VaultSecondary)
                Text("Import at least two items to create a private local story.", color = VaultSecondary)
            }
        } else Column(Modifier.padding(16.dp).width(180.dp).clickable { onOpen(items.first()) }) {
            Box(Modifier.fillMaxWidth().aspectRatio(0.82f).clip(RoundedCornerShape(22.dp)).background(VaultSurface), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Lock, "Private story", Modifier.size(54.dp), tint = VaultSecure)
            }
            Text("Private moments", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Text("${items.size} encrypted items", color = VaultSecondary)
        }
    }
}

@Composable
private fun SecureMenu(onImport: () -> Unit, onSettings: () -> Unit, onTrash: () -> Unit, onLock: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SecureToolbar("Secure menu")
        Spacer(Modifier.weight(1f))
        Card(colors = CardDefaults.cardColors(containerColor = VaultRaised), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceAround) {
                SecureMenuAction(Icons.Outlined.Upload, "Import", onImport)
                SecureMenuAction(Icons.Outlined.Delete, "Recycle bin", onTrash)
                SecureMenuAction(Icons.Outlined.Settings, "Settings", onSettings)
                SecureMenuAction(Icons.Outlined.Lock, "Lock now", onLock)
            }
        }
    }
}

@Composable
private fun SecureMenuAction(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Column(Modifier.width(94.dp).clickable(onClick = onClick).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, text, Modifier.size(30.dp), tint = VaultSecure); Text(text, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SecureBottomNavigation(selected: SecureTab, onSelected: (SecureTab) -> Unit) {
    Row(Modifier.fillMaxWidth().height(82.dp).background(VaultBackground).navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        SecureNav(Icons.Outlined.Image, "Pictures", selected == SecureTab.PICTURES) { onSelected(SecureTab.PICTURES) }
        SecureNav(Icons.Outlined.Collections, "Albums", selected == SecureTab.ALBUMS) { onSelected(SecureTab.ALBUMS) }
        SecureNav(Icons.Outlined.Movie, "Stories", selected == SecureTab.STORIES) { onSelected(SecureTab.STORIES) }
        SecureNav(Icons.Outlined.Menu, "Menu", selected == SecureTab.MENU) { onSelected(SecureTab.MENU) }
    }
}

@Composable
private fun SecureNav(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) VaultSecure else VaultSecondary
    Column(Modifier.width(84.dp).clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, text, tint = color); Text(text, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SecureViewer(item: SecureItem, viewModel: SecureGalleryViewModel, onBack: () -> Unit, onShare: (SecureItem, java.io.File) -> Unit, onDelete: () -> Unit) {
    var shareWarning by remember { mutableStateOf(false) }
    var deleteWarning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (item.isVideo) SecureVideo(item, viewModel) else SecureImage(item, viewModel)
        SecureToolbar(item.name, onBack)
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().background(Color(0xDD151517)).padding(12.dp), horizontalArrangement = Arrangement.SpaceAround) {
            SecureMenuAction(Icons.Outlined.Share, "Share", { shareWarning = true })
            SecureMenuAction(Icons.Outlined.Delete, "Delete", { deleteWarning = true })
        }
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
        text = { Text("The encrypted item can be restored for 30 days, then it is removed automatically.") },
        dismissButton = { TextButton(onClick = { deleteWarning = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { deleteWarning = false; onDelete() }) { Text("Move") } },
    )
}

@Composable
private fun SecureImage(item: SecureItem, viewModel: SecureGalleryViewModel) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, item.id) {
        value = viewModel.previewBytes(item)?.let { bytes ->
            try { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) } finally { bytes.fill(0) }
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (bitmap == null) CircularProgressIndicator(color = VaultSecure)
        else androidx.compose.foundation.Image(bitmap!!.asImageBitmap(), item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun SecureVideo(item: SecureItem, viewModel: SecureGalleryViewModel) {
    val context = LocalContext.current
    val factory = remember(item.id) { viewModel.dataSource(item) }
    if (factory == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Vault locked") }; return }
    val player = remember(item.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaSource(ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri("secure://${item.id}")))
            prepare(); playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxSize())
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
    var timeoutMenu by remember { mutableStateOf(false) }
    var launcherVisible by remember { mutableStateOf(isSecureLauncherVisible(context)) }
    var typed by remember { mutableStateOf("") }
    var currentSecret by remember { mutableStateOf("") }
    var newSecret by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SecureToolbar("Secure settings", onBack)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 18.dp)) }
        Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Unlock and privacy", style = MaterialTheme.typography.titleLarge)
                SecureSettingSwitch(
                    title = "Biometric unlock",
                    subtitle = if (biometricsAvailable) "Strong biometrics protect a separate vault-key envelope" else "Set up a strong biometric in Android settings first",
                    checked = state.biometricEnabled,
                    enabled = biometricsAvailable,
                ) { enabled -> if (enabled) onBiometricEnrollment() else viewModel.disableBiometric() }
                Box {
                    SecureSettingLink("Auto-lock", lockTimeoutLabel(state.lockTimeoutMs)) { timeoutMenu = true }
                    DropdownMenu(expanded = timeoutMenu, onDismissRequest = { timeoutMenu = false }) {
                        listOf(0L to "Immediately", 30_000L to "30 seconds", 60_000L to "1 minute", 300_000L to "5 minutes", 900_000L to "15 minutes").forEach { (millis, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setLockTimeout(millis); timeoutMenu = false })
                        }
                    }
                }
                SecureSettingSwitch(
                    "Show separate launcher icon",
                    "Gallery can still open the vault when this icon is hidden",
                    launcherVisible,
                    true,
                ) { visible -> setSecureLauncherVisible(context, visible); launcherVisible = visible }
                Button(onClick = { changePrompt = true }, modifier = Modifier.fillMaxWidth()) { Text("Change PIN or passphrase") }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("Protection status", style = MaterialTheme.typography.titleLarge)
                Text("Encrypted media and metadata • Device-bound key wrapping • Screenshot protection • Timed auto-lock", color = VaultSecondary)
                Button(onClick = viewModel::lock, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Lock, null); Spacer(Modifier.width(8.dp)); Text("Lock now") }
                Button(onClick = {
                    viewModel.clearTemporaryFiles()
                    Toast.makeText(context, "Temporary share files cleared", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.fillMaxWidth()) { Text("Clear temporary share files") }
                TextButton(onClick = { resetPrompt = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset Secure Gallery", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (changePrompt) AlertDialog(
        onDismissRequest = { changePrompt = false },
        title = { Text("Change PIN or passphrase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(currentSecret, { currentSecret = it }, label = { Text("Current") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                OutlinedTextField(newSecret, { newSecret = it }, label = { Text("New") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Confirm new") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            }
        },
        dismissButton = { TextButton(onClick = { changePrompt = false }) { Text("Cancel") } },
        confirmButton = {
            TextButton(enabled = currentSecret.length >= 6 && newSecret.length >= 6 && newSecret == confirmation, onClick = {
                viewModel.changeSecret(currentSecret, newSecret, confirmation)
                currentSecret = ""; newSecret = ""; confirmation = ""; changePrompt = false
            }) { Text("Change") }
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
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
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
    val component = ComponentName(context.packageName, "${context.packageName}.SecureGalleryLauncher")
    return context.packageManager.getComponentEnabledSetting(component) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
}

internal fun setSecureLauncherVisible(context: Context, visible: Boolean) {
    val component = ComponentName(context.packageName, "${context.packageName}.SecureGalleryLauncher")
    context.packageManager.setComponentEnabledSetting(
        component,
        if (visible) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP,
    )
    Toast.makeText(context, if (visible) "Secure Gallery icon shown" else "Secure Gallery icon hidden", Toast.LENGTH_SHORT).show()
}

@Composable
private fun SecureTrashScreen(state: SecureGalleryState, viewModel: SecureGalleryViewModel, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<SecureItem?>(null) }
    var emptyPrompt by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        SecureToolbar("Recycle bin", onBack) {
            if (state.trashItems.isNotEmpty()) TextButton(onClick = { emptyPrompt = true }) { Text("Empty") }
        }
        Text("Items are deleted automatically after 30 days.", color = VaultSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (state.trashItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Recycle bin is empty", color = VaultSecondary) }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) { items(state.trashItems, key = { it.id }) { SecureTile(it) { item -> selected = item } } }
        }
    }
    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(item.name) },
            text = { Text("Restore this encrypted item or delete it permanently?") },
            dismissButton = { TextButton(onClick = { selected = null; viewModel.deletePermanently(item) }) { Text("Delete permanently", color = MaterialTheme.colorScheme.error) } },
            confirmButton = { TextButton(onClick = { selected = null; viewModel.restore(item) }) { Text("Restore") } },
        )
    }
    if (emptyPrompt) AlertDialog(
        onDismissRequest = { emptyPrompt = false },
        title = { Text("Empty recycle bin?") },
        text = { Text("All ${state.trashItems.size} encrypted items will be permanently deleted. This cannot be undone.") },
        dismissButton = { TextButton(onClick = { emptyPrompt = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { emptyPrompt = false; viewModel.emptyTrash() }) { Text("Delete all", color = MaterialTheme.colorScheme.error) } },
    )
}
