package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.danyal.vaultgallery.ui.VaultBlue
import com.danyal.vaultgallery.ui.VaultRaised
import com.danyal.vaultgallery.ui.VaultSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class LabCategory(val title: String, val icon: ImageVector) {
    SELECT("Select", Icons.Outlined.TouchApp),
    ENHANCE("Enhance", Icons.Outlined.Tune),
    PEOPLE("People", Icons.Outlined.FaceRetouchingNatural),
    RESTORE("Restore", Icons.Outlined.Restore),
    DOCUMENT("Document", Icons.Outlined.Description),
    CREATE("Create", Icons.Outlined.Palette),
}

private val AiStudioYellow = Color(0xFFFFD60A)

private enum class LabEngine(val label: String) {
    CLASSICAL("LOCAL TOOL"),
    NEURAL("NEURAL MODEL"),
}

private data class LabAction(
    val title: String,
    val description: String,
    val transform: suspend (Bitmap) -> Bitmap,
    val engine: LabEngine = LabEngine.CLASSICAL,
)
private data class PreviewResult(val loading: Boolean, val bitmap: Bitmap? = null, val error: String? = null)

private fun actionsFor(category: LabCategory): List<LabAction> = when (category) {
    LabCategory.SELECT -> emptyList()
    LabCategory.ENHANCE -> listOf(
        LabAction("Auto enhance", "Balanced exposure, tone and local contrast", ::autoEnhancePhoto),
        LabAction("Low-light rescue", "Lift a dark scene while protecting highlights", ::recoverLowLightPhoto),
        LabAction("Denoise", "Remove grain and chroma speckles", ::denoisePhoto),
        LabAction("Clarity", "Recover texture without crushing shadows", ::improveClarityPhoto),
        LabAction("Smart sharpen", "Crisp edges with restrained halos", ::smartSharpenPhoto),
        LabAction("White balance", "Neutralize an unwanted color cast", ::whiteBalancePhoto),
        LabAction("Dehaze", "Restore contrast through haze", ::dehazePhoto),
        LabAction("HDR detail", "Controlled micro-contrast and depth", ::hdrDetailPhoto),
        LabAction("Lift shadows", "Open dark areas naturally", ::liftShadowsPhoto),
        LabAction("Recover whites", "Restore harsh bright areas", ::recoverHighlightsPhoto),
    )
    LabCategory.PEOPLE -> listOf(
        LabAction("Portrait blur", "Neural person mask with natural depth", ::aiPortraitBackgroundBlur, LabEngine.NEURAL),
        LabAction("Studio light", "Relight the subject and shape the scene", ::aiPortraitSpotlight, LabEngine.NEURAL),
        LabAction("Color focus", "Keep the subject in color", ::aiPortraitColorPop, LabEngine.NEURAL),
        LabAction("Clean backdrop", "Replace distractions with a studio sweep", ::aiPortraitStudioBackground, LabEngine.NEURAL),
        LabAction("Natural skin", "Subject-aware skin treatment", ::aiPortraitSmoothPhoto, LabEngine.NEURAL),
        LabAction("Warm subject", "Selective portrait grading", ::aiPortraitWarmPhoto, LabEngine.NEURAL),
        LabAction("Night portrait", "Balance subject and dark background", ::aiPortraitNightPhoto, LabEngine.NEURAL),
    )
    LabCategory.RESTORE -> listOf(
        LabAction("Restore faded", "Repair faded contrast and color", ::restoreFadedPhoto),
        LabAction("2x upscale", "Lanczos upscale with restrained sharpening", ::upscalePhoto),
        LabAction("Remove noise", "Clean scanned grain and sensor noise", ::denoisePhoto),
        LabAction("Repair color", "Correct aged or incorrect color casts", ::whiteBalancePhoto),
        LabAction("Recover detail", "Bring definition back to a soft capture", ::improveClarityPhoto),
        LabAction("Soft glow", "Gentle highlight diffusion", ::softGlowPhoto),
    )
    LabCategory.DOCUMENT -> listOf(
        LabAction("Clean color", "Flatten shadows while preserving colored ink", ::cleanColorDocumentPhoto),
        LabAction("Clean B&W", "High-contrast page for OCR and printing", ::cleanDocumentPhoto),
        LabAction("Remove shadows", "Correct uneven page lighting", ::removeDocumentShadowsPhoto),
        LabAction("Ink boost", "Recover faint printed detail", ::inkBoostDocumentPhoto),
        LabAction("Text clarity", "Sharpen fine characters", ::smartSharpenPhoto),
        LabAction("Grayscale", "Neutral grayscale without hard thresholding", ::grayscaleDocumentPhoto),
    )
    LabCategory.CREATE -> listOf(
        LabAction("Cinematic", "Teal shadows with warm highlights", ::cinematicPhoto),
        LabAction("Matte film", "Soft contrast with lifted blacks", ::mattePhoto),
        LabAction("Noir", "High-contrast monochrome", ::noirPhoto),
        LabAction("Graphic novel", "Edge-preserving illustrated rendering", ::comicPhoto),
        LabAction("Pencil", "Monochrome hand-drawn rendering", ::pencilSketchPhoto),
        LabAction("Watercolor", "Painterly full-resolution stylization", ::watercolorPhoto),
        LabAction("Pixel art", "Purposeful block-pixel treatment", ::pixelatePhoto),
    )
}

/** Dedicated, full-screen AI workspace shared by the normal and Secure Gallery editors. */
@Composable
internal fun AiPhotoLabScreen(
    source: Bitmap,
    onCancel: () -> Unit,
    onDone: (Bitmap) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val aiRuntime = remember(context) { AiExecutionRuntime(context) }
    val scope = rememberCoroutineScope()
    var current by remember(source) { mutableStateOf(source.mutableSoftwareBitmap()) }
    var history by remember(source) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var future by remember(source) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var category by remember { mutableStateOf(LabCategory.SELECT) }
    var selected by remember { mutableStateOf<LabAction?>(null) }
    var strength by remember { mutableFloatStateOf(1f) }
    var comparing by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    var activeJob by remember { mutableStateOf<Job?>(null) }
    var selectionSource by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var discardPrompt by remember { mutableStateOf(false) }
    fun historyLimit(value: Bitmap): Int = if (value.allocationByteCount >= 96 * 1024 * 1024) 1 else 3

    fun requestClose() {
        if (history.isNotEmpty() || selected != null) discardPrompt = true else onCancel()
    }

    BackHandler(enabled = true, onBack = ::requestClose)

    val previewSource = remember(current) { current.aiPreview(768) }
    // Expensive inference belongs to tool selection, not to every strength-slider pixel. The
    // result is cached once and composited over the source by Compose, so strength follows the
    // thumb at display rate. Apply performs the exact full-resolution blend.
    val candidate by produceState(initialValue = PreviewResult(selected != null), previewSource, selected?.title) {
        val action = selected
        value = if (action == null) PreviewResult(false) else withContext(Dispatchers.Default) {
            runCatching {
                val transformed = aiRuntime.run(
                    AiOperationSpec(action.title.lowercase().replace(' ', '-'), if (action.engine == LabEngine.NEURAL) AiBackend.ML_KIT else AiBackend.OPENCV, memoryMultiplier = 2),
                    previewSource,
                    action.transform,
                )
                PreviewResult(false, bitmap = transformed)
            }
                .getOrElse { PreviewResult(false, error = it.message ?: "Preview unavailable") }
        }
    }

    selectionSource?.let { selection ->
        AiSelectionStudio(
            source = selection,
            onCancel = { selectionSource = null },
            onApply = { result ->
                history = (history + current).takeLast(historyLimit(current))
                future = emptyList()
                current = result
                selectionSource = null
                selected = null
            },
        )
        return
    }

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = ::requestClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Close editing studio", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text("Editing Studio", color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text("On-device models and local tools", color = VaultSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(modifier = Modifier.size(40.dp), enabled = history.isNotEmpty() && !applying, onClick = {
                    future = (future + current).takeLast(historyLimit(current))
                    current = history.last()
                    history = history.dropLast(1)
                    selected = null
                }) { Icon(Icons.Outlined.Undo, "Undo", tint = if (history.isNotEmpty()) Color.White else VaultSecondary) }
                IconButton(modifier = Modifier.size(40.dp), enabled = future.isNotEmpty() && !applying, onClick = {
                    history = (history + current).takeLast(historyLimit(current))
                    current = future.last()
                    future = future.dropLast(1)
                    selected = null
                }) { Icon(Icons.Outlined.Redo, "Redo", tint = if (future.isNotEmpty()) Color.White else VaultSecondary) }
                Button(
                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (applying) Color(0xFFB3261E) else Color.White,
                        contentColor = if (applying) Color.White else Color.Black,
                    ),
                    onClick = {
                        if (applying) {
                            activeJob?.cancel()
                        } else {
                            val pending = selected
                            if (pending == null) onDone(current)
                            else activeJob = scope.launch {
                                applying = true
                                try {
                                    val transformed = aiRuntime.run(
                                        AiOperationSpec(pending.title.lowercase().replace(' ', '-'), if (pending.engine == LabEngine.NEURAL) AiBackend.ML_KIT else AiBackend.OPENCV),
                                        current,
                                        pending.transform,
                                    )
                                    onDone(withContext(Dispatchers.Default) { blendLabEffect(current, transformed, strength) })
                                } catch (_: CancellationException) {
                                    // User-requested cancellation leaves the current edit intact.
                                } catch (failure: Throwable) {
                                    error = "${pending.title} failed: ${failure.message ?: "unknown error"}"
                                } finally {
                                    applying = false
                                    activeJob = null
                                }
                            }
                        }
                    },
                ) {
                    Icon(if (applying) Icons.AutoMirrored.Outlined.ArrowBack else Icons.Outlined.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (applying) "Cancel" else "Done")
                }
            }
        },
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().heightIn(min = 272.dp, max = 350.dp)
                    .background(Color.Black)
                    .navigationBarsPadding().padding(top = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(LabCategory.entries) { item ->
                        val active = category == item
                        Column(
                            Modifier.width(72.dp).combinedClickable(onClick = {
                                category = item
                                selected = null
                            }).padding(vertical = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                Modifier.size(48.dp).background(if (active) Color.White else Color(0xFF242427), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Icon(item.icon, item.title, tint = if (active) Color.Black else Color.White, modifier = Modifier.size(24.dp)) }
                            Text(item.title, color = if (active) AiStudioYellow else Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (category == LabCategory.SELECT) {
                    Card(
                        onClick = { selectionSource = current },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(124.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF242427)),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Row(Modifier.fillMaxSize().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(66.dp).background(
                                    Brush.linearGradient(listOf(Color(0xFFFFD60A), Color(0xFFFF9F0A))),
                                    RoundedCornerShape(21.dp),
                                ),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Outlined.AutoAwesome, null, tint = Color.Black, modifier = Modifier.size(34.dp)) }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Magic Select", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(8.dp))
                                    Text("SMART CUT", color = AiStudioYellow, style = MaterialTheme.typography.labelSmall)
                                }
                                Text("Tap an object, refine it with Add, Subtract or Lasso, then erase, relight, isolate, blur or create a cutout.", color = Color(0xFFD4DCF0), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Text("Selection remains editable until you confirm an effect.", color = VaultSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 18.dp))
                } else {
                    val actions = actionsFor(category)
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(actions, key = { it.title }) { action ->
                            LabActionCard(
                                source = current,
                                action = action,
                                selected = selected?.title == action.title,
                                onClick = { selected = action; strength = 1f },
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(selected?.title ?: "Choose a tool", color = Color.White, style = MaterialTheme.typography.titleSmall)
                            Text(selected?.description ?: "A live preview appears above. Nothing changes until Apply.", color = VaultSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                        }
                        Button(enabled = selected != null && !applying, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), onClick = {
                            val action = selected ?: return@Button
                            scope.launch {
                                applying = true
                                runCatching { withContext(Dispatchers.Default) { blendLabEffect(current, action.transform(current), strength) } }
                                    .onSuccess { result ->
                                        history = (history + current).takeLast(historyLimit(current))
                                        future = emptyList()
                                        current = result
                                        selected = null
                                    }
                                    .onFailure { error = "${action.title} failed: ${it.message ?: "unknown error"}" }
                                applying = false
                            }
                        }) { Text(if (applying) "Working…" else "Apply") }
                    }
                    if (selected != null) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Strength ${(strength * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(94.dp))
                            Slider(
                                value = strength,
                                onValueChange = { strength = it },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = AiStudioYellow, activeTrackColor = AiStudioYellow, inactiveTrackColor = Color(0xFF45454A)),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(Color.Black)
                .pointerInput(source, selected?.title) {
                    detectTapGestures(onPress = {
                        val reveal = scope.launch {
                            kotlinx.coroutines.delay(140)
                            comparing = true
                        }
                        tryAwaitRelease()
                        reveal.cancel()
                        comparing = false
                    })
                },
            contentAlignment = Alignment.Center,
        ) {
            val shown: Bitmap = when {
                comparing -> if (selected != null) current else history.lastOrNull() ?: source
                selected != null -> previewSource
                else -> current
            }
            Image(shown.asImageBitmap(), "AI preview", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            if (!comparing && selected != null) candidate.bitmap?.let { effectPreview ->
                Image(
                    effectPreview.asImageBitmap(),
                    "AI effect preview",
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    alpha = strength,
                )
            }
            Text(
                when { comparing -> "BEFORE"; selected != null -> "LIVE PREVIEW"; else -> "EDITED" },
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                    .background(Color(0xAA17171B), RoundedCornerShape(18.dp)).padding(horizontal = 11.dp, vertical = 6.dp),
            )
            if (applying || candidate.loading) {
                Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AiStudioYellow)
                }
            }
            candidate.error?.let { message ->
                Text("Preview unavailable: $message", color = Color.White, modifier = Modifier.align(Alignment.Center).background(Color(0xCC7A1D1D), RoundedCornerShape(14.dp)).padding(12.dp))
            }
        }
    }

    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Editing Studio") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
        )
    }
    if (discardPrompt) AlertDialog(
        onDismissRequest = { discardPrompt = false },
        title = { Text("Discard studio edits?") },
        text = { Text("Applied edits and the visible preview have not been returned to the photo editor yet.") },
        dismissButton = { TextButton(onClick = { discardPrompt = false }) { Text("Keep editing") } },
        confirmButton = { TextButton(onClick = { discardPrompt = false; onCancel() }) { Text("Discard") } },
    )
}

/** Blends every Editing Studio result with its immediate input so preview, Apply and Done share the
 * same user-controlled strength. The input is scaled only for effects that intentionally change
 * dimensions, such as 2× upscale. */
private fun blendLabEffect(input: Bitmap, transformed: Bitmap, strength: Float): Bitmap {
    val amount = strength.coerceIn(0f, 1f)
    if (amount >= .999f) return transformed
    if (amount <= .001f) return input.copy(Bitmap.Config.ARGB_8888, false)
    val output = createEditBitmapLike(transformed)
    val canvas = Canvas(output)
    val target = Rect(0, 0, output.width, output.height)
    canvas.drawBitmap(input, null, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    canvas.drawBitmap(transformed, null, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { alpha = (amount * 255).toInt() })
    if (transformed !== input && !transformed.isRecycled) transformed.recycle()
    return output
}

@Composable
private fun LabActionCard(source: Bitmap, action: LabAction, selected: Boolean, onClick: () -> Unit) {
    val thumbnail = remember(source) { source.aiPreview(260) }
    val rendered by produceState(initialValue = PreviewResult(true), thumbnail, action.title) {
        value = withContext(Dispatchers.Default) {
            runCatching { PreviewResult(false, bitmap = action.transform(thumbnail)) }
                .getOrElse { PreviewResult(false, error = it.message ?: "Unavailable") }
        }
    }
    Card(
        onClick = onClick,
        modifier = Modifier.width(146.dp).height(112.dp).border(
            if (selected) 2.dp else 1.dp,
            if (selected) AiStudioYellow else Color(0xFF44444C),
            RoundedCornerShape(20.dp),
        ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF29292F)),
    ) {
        Box(Modifier.fillMaxSize()) {
            rendered.bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            if (rendered.loading) CircularProgressIndicator(Modifier.align(Alignment.Center).size(22.dp), color = AiStudioYellow, strokeWidth = 2.dp)
            if (rendered.error != null) Text("Unavailable", color = Color.White, modifier = Modifier.align(Alignment.Center).background(Color(0xAA7A1D1D), RoundedCornerShape(8.dp)).padding(5.dp))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEA101014)))))
            Text(
                action.engine.label,
                color = if (action.engine == LabEngine.NEURAL) Color.Black else Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.TopStart).padding(7.dp)
                    .background(if (action.engine == LabEngine.NEURAL) AiStudioYellow else Color(0xA928282E), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(9.dp)) {
                Text(action.title, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text(action.description, color = Color(0xFFD4D4DA), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

private fun Bitmap.aiPreview(maxDimension: Int): Bitmap {
    val longest = maxOf(width, height).coerceAtLeast(1)
    if (longest <= maxDimension) return this
    val scale = maxDimension.toFloat() / longest
    return Bitmap.createScaledBitmap(this, (width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1), true)
}

private fun Bitmap.mutableSoftwareBitmap(): Bitmap {
    if (config != Bitmap.Config.HARDWARE && isMutable && config == Bitmap.Config.ARGB_8888) return this
    return copy(Bitmap.Config.ARGB_8888, true)
        ?: createEditBitmapLike(this, width, height).also { android.graphics.Canvas(it).drawBitmap(this, 0f, 0f, null) }
}
