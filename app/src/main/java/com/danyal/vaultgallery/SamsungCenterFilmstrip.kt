package com.danyal.vaultgallery

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Samsung Gallery's bottom control is one centre-anchored mixed-media coordinate system.
 * Images are discrete segments. A video becomes a duration strip while the user drags through
 * it, and the fixed screen centre maps directly to a timestamp. This composable deliberately
 * owns both navigation and video seeking so they cannot drift into competing state machines.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun <T> SamsungCenterFilmstrip(
    items: List<T>,
    currentKey: Any,
    key: (T) -> Any,
    isVideo: (T) -> Boolean,
    durationMs: (T) -> Long,
    mediaWidth: (T) -> Int,
    mediaHeight: (T) -> Int,
    videoPositionMs: (T) -> Long,
    videoPlaying: (T) -> Boolean = { false },
    videoFrameSource: (T) -> Uri?,
    videoFrames: (@Composable (item: T, modifier: Modifier) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onCurrentChanged: (T) -> Unit,
    onVideoTapped: (T) -> Unit,
    onVideoScrub: (item: T, positionMs: Long, final: Boolean) -> Unit,
    thumbnail: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    if (items.isEmpty()) return
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()
    val normalFling = ScrollableDefaults.flingBehavior()
    val directFrameScrub = remember {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float = 0f
        }
    }
    var widthPx by remember { mutableFloatStateOf(1f) }
    var pointerDown by remember { mutableStateOf(false) }
    var stripGesture by remember { mutableStateOf(false) }
    var scrubKey by remember { mutableStateOf<Any?>(null) }
    var reportedKey by remember { mutableStateOf(currentKey) }
    var wasMoving by remember { mutableStateOf(false) }
    var liveVideoPositionMs by remember(currentKey) { mutableLongStateOf(0L) }
    var liveVideoPlaying by remember(currentKey) { mutableStateOf(false) }
    val halfScreen = with(density) { (widthPx / 2f).toDp() }

    fun compactWidth(item: T, selected: Boolean): Dp {
        // Preserve enough of the source aspect ratio to distinguish portrait, square and
        // landscape media, but enforce Samsung-sized minimums so tall photos never collapse
        // into the minuscule slivers that made the earlier strip hard to use.
        return filmstripCompactWidthDp(mediaWidth(item), mediaHeight(item), selected).dp
    }

    fun frameCount(item: T): Int =
        (ceil(durationMs(item).coerceAtLeast(1L) / 1_000.0).toInt() + 1).coerceIn(8, 20)

    fun itemWidth(item: T): Dp = if (scrubKey == key(item) && isVideo(item)) {
        (frameCount(item) * 59).dp
    } else compactWidth(item, key(item) == currentKey)

    fun centreObservation(): Pair<T, LazyListItemInfo>? {
        val layout = state.layoutInfo
        if (layout.visibleItemsInfo.isEmpty()) return null
        val centre = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
        val info = layout.visibleItemsInfo.firstOrNull {
            centre >= it.offset && centre <= it.offset + it.size
        } ?: layout.visibleItemsInfo.minByOrNull {
            abs((it.offset + it.size / 2f) - centre)
        } ?: return null
        return items.getOrNull(info.index)?.let { it to info }
    }

    // Playback and the filmstrip are two projections of the same timeline. Compact playback
    // moves the in-thumbnail indexer; expanded playback moves the frame row beneath the fixed
    // centre playhead. Pause freezes both, and a finger gesture immediately takes ownership.
    LaunchedEffect(currentKey, items, scrubKey, pointerDown, stripGesture) {
        while (true) {
            val current = items.firstOrNull { key(it) == currentKey }
            liveVideoPositionMs = if (current != null && isVideo(current)) {
                videoPositionMs(current).coerceAtLeast(0L)
            } else 0L
            liveVideoPlaying = current != null && isVideo(current) && videoPlaying(current)
            if (
                current != null && scrubKey == currentKey && liveVideoPlaying &&
                !pointerDown && !stripGesture && !state.isScrollInProgress
            ) {
                val currentIndex = items.indexOfFirst { key(it) == currentKey }
                val info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex }
                if (info != null) {
                    val layout = state.layoutInfo
                    val centre = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
                    val fraction = (liveVideoPositionMs.toFloat() / durationMs(current).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    val delta = info.offset + info.size * fraction - centre
                    if (abs(delta) >= 0.5f) state.scrollBy(delta)
                }
            }
            delay(32)
        }
    }

    suspend fun centreItem(index: Int) {
        val candidate = items.getOrNull(index) ?: return
        val offset = with(density) { (itemWidth(candidate).toPx() / 2f).roundToInt() }
        state.animateScrollToItem(index, offset)
    }

    // External navigation (main-viewport swipe, next/previous, or a direct thumbnail tap) must
    // converge on the same strip offset. Never recenter while the strip itself owns the gesture.
    LaunchedEffect(currentKey, items.size, widthPx, scrubKey, stripGesture) {
        reportedKey = currentKey
        if (!stripGesture && scrubKey != currentKey) {
            val index = items.indexOfFirst { key(it) == currentKey }
            if (index >= 0) {
                delay(24)
                centreItem(index)
            }
        }
    }

    // Resolve the media and (for video) timestamp beneath the physical screen centre every frame.
    // Do not key this observer to scrubKey: the observer itself unfolds/collapses videos. A
    // restart in the middle of that mutation can cancel the frame-boundary anchor correction.
    LaunchedEffect(state, items, currentKey, stripGesture, pointerDown, widthPx) {
        while (true) {
            val moving = pointerDown || state.isScrollInProgress
            val observation = centreObservation()
            if (stripGesture && observation != null) {
                val (candidate, info) = observation
                val candidateKey = key(candidate)
                if (reportedKey != candidateKey) {
                    reportedKey = candidateKey
                    onCurrentChanged(candidate)
                }
                if (isVideo(candidate)) {
                    if (scrubKey != candidateKey) {
                        val layout = state.layoutInfo
                        val centre = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
                        val spatialFraction = ((centre - info.offset) / info.size.toFloat()).coerceIn(0f, 1f)
                        // When the already-current video unfolds, preserve its live playback
                        // frame. When the drag crosses into another video, spatial penetration
                        // from that video's leading/trailing edge determines the timestamp.
                        val entryFraction = if (candidateKey == currentKey) {
                            (videoPositionMs(candidate).toFloat() / durationMs(candidate).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                        } else spatialFraction
                        scrubKey = candidateKey
                        // Preserve the exact temporal point under the fixed centre while the
                        // compact video unfolds. Direction-specific compensation caused the row
                        // to jump when entering a video from its leading edge.
                        withFrameNanos { }
                        state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == info.index }?.let { expanded ->
                            val expandedPoint = expanded.offset + expanded.size * entryFraction
                            state.scrollBy(expandedPoint - centre)
                        }
                    } else {
                        val layout = state.layoutInfo
                        val centre = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
                        val fraction = ((centre - info.offset) / info.size.toFloat()).coerceIn(0f, 1f)
                        onVideoScrub(candidate, (durationMs(candidate) * fraction).toLong(), false)
                    }
                }
            }

            if (wasMoving && !moving && stripGesture && observation != null) {
                val (candidate, info) = observation
                if (isVideo(candidate) && scrubKey == key(candidate)) {
                    val layout = state.layoutInfo
                    val centre = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
                    val fraction = ((centre - info.offset) / info.size.toFloat()).coerceIn(0f, 1f)
                    onVideoScrub(candidate, (durationMs(candidate) * fraction).toLong(), true)
                } else {
                    // Collapse any old video only after crossing and settling on an image. A frame
                    // later, recenter to absorb the width change without a visible teleport.
                    scrubKey = null
                    withFrameNanos { }
                    centreItem(items.indexOfFirst { key(it) == key(candidate) }.coerceAtLeast(0))
                }
                stripGesture = false
            }
            wasMoving = moving
            delay(16)
        }
    }

    Box(
        modifier
            .semantics {
                liveRegion = LiveRegionMode.Polite
                val currentIndex = items.indexOfFirst { key(it) == currentKey }.coerceAtLeast(0)
                val current = items.getOrNull(currentIndex)
                stateDescription = buildString {
                    append("Item ${currentIndex + 1} of ${items.size}")
                    if (current != null && isVideo(current)) append(", video at ${samsungVideoTimeLabel(liveVideoPositionMs)}")
                }
            }
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) },
        contentAlignment = Alignment.Center,
    ) {
        LazyRow(
            state = state,
            // Frame-level seeking must stop under the finger. The default LazyRow decay could
            // turn a small release velocity into many seconds of unintended travel. Compact
            // media navigation retains normal fling behavior.
            flingBehavior = if (scrubKey != null) directFrameScrub else normalFling,
            modifier = Modifier.fillMaxSize().pointerInput(items.map(key)) {
                val slop = 10.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pointerDown = true
                    var totalX = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        totalX += change.positionChange().x
                        if (!stripGesture && abs(totalX) >= slop) stripGesture = true
                        if (!change.pressed) break
                    }
                    pointerDown = false
                }
            },
            contentPadding = PaddingValues(horizontal = halfScreen),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(items, key = { _, candidate -> key(candidate) }) { index, candidate ->
                val candidateKey = key(candidate)
                val selected = candidateKey == currentKey
                val scrubbing = candidateKey == scrubKey && isVideo(candidate)
                Box(
                    Modifier.width(itemWidth(candidate))
                        .height(if (selected || scrubbing) 44.dp else 36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .clickable {
                            if (isVideo(candidate)) {
                                if (candidateKey != currentKey) {
                                    // Samsung keeps the strip compact when another video is
                                    // tapped. It navigates and autoplays that item; only a second
                                    // tap on the now-current video opens frame-level control.
                                    scrubKey = null
                                    scope.launch { centreItem(index) }
                                    onCurrentChanged(candidate)
                                } else {
                                    val duration = durationMs(candidate).coerceAtLeast(1L)
                                    val fraction = (videoPositionMs(candidate).toFloat() / duration).coerceIn(0f, 1f)
                                    scrubKey = candidateKey
                                    onVideoTapped(candidate)
                                    scope.launch {
                                        // Expand around the exact frame already being viewed.
                                        withFrameNanos { }
                                        val layout = state.layoutInfo
                                        val centre = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
                                        layout.visibleItemsInfo.firstOrNull { it.index == index }?.let { expanded ->
                                            state.scrollBy(expanded.offset + expanded.size * fraction - centre)
                                        }
                                    }
                                }
                            } else {
                                scope.launch { centreItem(index) }
                                onCurrentChanged(candidate)
                            }
                        },
                ) {
                    thumbnail(candidate, Modifier.fillMaxSize())
                    if (scrubbing) {
                        if (videoFrames != null) {
                            videoFrames(candidate, Modifier.fillMaxSize())
                        } else videoFrameSource(candidate)?.let { source ->
                            SamsungVideoFrames(
                                source = source,
                                mediaKey = candidateKey,
                                durationHintMs = durationMs(candidate),
                                frameCount = frameCount(candidate),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    } else if (isVideo(candidate)) {
                        Box(
                            Modifier.align(Alignment.BottomStart).padding(3.dp).size(18.dp)
                                .clip(CircleShape).background(Color(0xCC202024)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.PlayArrow, "Video", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        if (selected) {
                            val duration = durationMs(candidate).coerceAtLeast(1L)
                            val fraction = (liveVideoPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                            Box(
                                Modifier.align(Alignment.CenterStart)
                                    .offset { IntOffset(((63.dp.toPx() - 4.dp.toPx()) * fraction).roundToInt(), 0) }
                                    .fillMaxHeight().width(4.dp)
                                    .background(Color.White),
                            )
                        }
                    }
                }
            }
        }
        if (scrubKey == currentKey) {
            Box(
                Modifier.fillMaxHeight().width(2.dp)
                    .clip(RoundedCornerShape(2.dp)).background(Color.White),
            )
        }
    }
}

internal fun filmstripCompactWidthDp(width: Int, height: Int, selected: Boolean): Int {
    val aspect = width.toFloat().coerceAtLeast(1f) / height.toFloat().coerceAtLeast(1f)
    val cellHeight = if (selected) 44f else 36f
    val minimum = if (selected) 63f else 52f
    val maximum = if (selected) 88f else 72f
    return (cellHeight * aspect).coerceIn(minimum, maximum).roundToInt()
}
