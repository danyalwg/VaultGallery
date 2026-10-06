package com.danyal.vaultgallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

/** Returns the current app window height, including live resize and multi-window changes. */
@Composable
internal fun currentWindowHeightDp(): Dp {
    val heightPx = LocalWindowInfo.current.containerSize.height
    return with(LocalDensity.current) { heightPx.toDp() }
}
