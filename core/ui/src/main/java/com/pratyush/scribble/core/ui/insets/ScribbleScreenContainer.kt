package com.pratyush.scribble.core.ui.insets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.pratyush.scribble.core.designsystem.theme.DarkBackground

/**
 * ScribbleScreenContainer ensures that system bars (status bar, navigation bar)
 * and IME keyboard insets are handled centrally without double-padding or arbitrary hardcoded spacers.
 */
@Composable
fun ScribbleScreenContainer(
    modifier: Modifier = Modifier,
    applyStatusBarInset: Boolean = true,
    applyNavigationBarInset: Boolean = true,
    applyImeInset: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    val statusBarPadding: Dp = if (applyStatusBarInset) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }

    val navBarPadding: Dp = if (applyNavigationBarInset) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }

    val imeBottomPadding: Dp = if (applyImeInset) {
        WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }

    // When IME is open, imeBottomPadding is larger than navBarPadding,
    // elevating bottom content cleanly above the virtual keyboard.
    val bottomPadding: Dp = max(navBarPadding, imeBottomPadding)

    val paddingValues = PaddingValues(
        top = statusBarPadding,
        bottom = bottomPadding
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(paddingValues)
    ) {
        content(paddingValues)
    }
}
