package com.pratyush.scribble.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceElevated

@Composable
fun AvatarBadge(
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fontSizeRatio: Float = 0.55f
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(DarkSurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = (size.value * fontSizeRatio).sp
        )
    }
}
