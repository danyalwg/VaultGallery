package com.danyal.vaultgallery

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal enum class ViewerSwipeAction { PREVIOUS, NEXT, INFO, CANCEL }

/** A vertical-only companion for HorizontalPager. It deliberately ignores horizontal movement so
 * the pager receives the complete gesture stream and can keep both media pages attached to the
 * finger from touch-down through cancellation or settling. */
internal fun Modifier.viewerInfoGesture(
    key: Any?,
    enabled: Boolean = true,
    onDrag: (vertical: Float) -> Unit,
    onRelease: (showInfo: Boolean) -> Unit,
): Modifier = pointerInput(key, enabled) {
    val threshold = 64.dp.toPx()
    val directionSlop = 7.dp.toPx()
    val topControls = 92.dp.toPx()
    val bottomControls = 220.dp.toPx()
    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val eligible = enabled && first.position.y >= topControls && first.position.y <= size.height - bottomControls
        var horizontal = 0f
        var vertical = 0f
        var verticalAxis = false
        var multiTouch = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } > 1) multiTouch = true
            val change = event.changes.firstOrNull { it.id == first.id } ?: break
            val movement = change.positionChange()
            horizontal += movement.x
            vertical += movement.y
            if (eligible && !multiTouch && !verticalAxis && maxOf(abs(horizontal), abs(vertical)) >= directionSlop) {
                verticalAxis = abs(vertical) > abs(horizontal) * 1.12f
            }
            if (verticalAxis) {
                change.consume()
                onDrag(vertical.coerceAtMost(0f))
            }
            if (!change.pressed) break
        }
        onRelease(eligible && !multiTouch && verticalAxis && vertical < -threshold)
    }
}

/**
 * Observes viewer swipes during the initial pointer pass and reports continuous, axis-locked
 * displacement. Child image/video controls may still handle taps and pinches.
 */
internal fun Modifier.viewerSwipeGestures(
    key: Any?,
    enabled: Boolean = true,
    onDrag: (horizontal: Float, vertical: Float) -> Unit,
    onRelease: (action: ViewerSwipeAction, horizontalVelocity: Float) -> Unit,
): Modifier = pointerInput(key, enabled) {
    // Samsung commits short intentional swipes and fast flicks, while a slow accidental nudge
    // springs back. Projecting release velocity avoids requiring a large physical finger travel.
    // A page should feel attached to the finger, then commit from either a short deliberate drag
    // or a quick flick. The previous 14%/48dp gate made users travel conspicuously farther than
    // Samsung Gallery before anything happened.
    val horizontalThreshold = minOf(36.dp.toPx(), size.width * .10f)
    val horizontalFlingVelocity = 410.dp.toPx()
    val verticalThreshold = 64.dp.toPx()
    val directionSlop = 5.dp.toPx()
    val topControls = 92.dp.toPx()
    // The viewer reserves this lower region for the video timeline, filmstrip, and action bar.
    // Keeping it out of page detection prevents a seek drag from becoming a next/previous swipe.
    val bottomControls = 220.dp.toPx()
    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val eligible = enabled && first.position.y >= topControls && first.position.y <= size.height - bottomControls
        var horizontal = 0f
        var vertical = 0f
        var multiTouch = false
        var horizontalAxis: Boolean? = null
        val velocityTracker = VelocityTracker().apply {
            addPosition(first.uptimeMillis, first.position)
        }
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } > 1) {
                multiTouch = true
                onDrag(0f, 0f)
            }
            val change = event.changes.firstOrNull { it.id == first.id } ?: break
            velocityTracker.addPosition(change.uptimeMillis, change.position)
            val movement = change.positionChange()
            horizontal += movement.x
            vertical += movement.y
            if (eligible && !multiTouch) {
                if (horizontalAxis == null && maxOf(abs(horizontal), abs(vertical)) >= directionSlop) {
                    horizontalAxis = when {
                        abs(horizontal) > abs(vertical) * 1.06f -> true
                        abs(vertical) > abs(horizontal) * 1.06f -> false
                        else -> null
                    }
                }
                when (horizontalAxis) {
                    true -> { change.consume(); onDrag(horizontal, 0f) }
                    false -> { change.consume(); onDrag(0f, vertical.coerceAtMost(0f)) }
                    null -> Unit
                }
            }
            if (!change.pressed) break
        }
        val horizontalVelocity = if (multiTouch || horizontalAxis != true) 0f else {
            runCatching { velocityTracker.calculateVelocity().x }.getOrDefault(0f)
        }
        val projectedHorizontal = horizontal + horizontalVelocity * .16f
        val action = if (!eligible || multiTouch) ViewerSwipeAction.CANCEL else when {
            horizontalAxis == false && vertical < -verticalThreshold -> ViewerSwipeAction.INFO
            horizontalAxis == true && (
                projectedHorizontal < -horizontalThreshold ||
                    (horizontal < 0f && horizontalVelocity < -horizontalFlingVelocity)
            ) -> ViewerSwipeAction.NEXT
            horizontalAxis == true && (
                projectedHorizontal > horizontalThreshold ||
                    (horizontal > 0f && horizontalVelocity > horizontalFlingVelocity)
            ) -> ViewerSwipeAction.PREVIOUS
            else -> ViewerSwipeAction.CANCEL
        }
        onRelease(action, horizontalVelocity)
    }
}
