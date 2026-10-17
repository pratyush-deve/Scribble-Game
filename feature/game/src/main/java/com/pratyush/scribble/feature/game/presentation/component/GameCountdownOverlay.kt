package com.pratyush.scribble.feature.game.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratyush.scribble.core.designsystem.theme.DarkBackground
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceElevated
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import kotlin.math.max

@Composable
fun GameCountdownOverlay(
    remainingSeconds: Int,
    round: Int,
    totalRounds: Int,
    drawerName: String?,
    modifier: Modifier = Modifier
) {
    val displayNum = max(1, remainingSeconds)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Round Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "ROUND $round OF $totalRounds",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = VibrantCyan,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (round > 1) "ROUND $round STARTING IN" else "GAME STARTING IN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Large Animated Countdown Number in Center
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                VibrantViolet.copy(alpha = 0.35f),
                                DarkSurface.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .border(3.dp, VibrantCyan.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = displayNum,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.5f, animationSpec = tween(300, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(durationMillis = 250, easing = LinearEasing)) +
                                        scaleOut(targetScale = 1.3f, animationSpec = tween(250, easing = LinearEasing))
                            )
                    },
                    label = "CountdownNumberAnimation"
                ) { targetNumber ->
                    Text(
                        text = "$targetNumber",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!drawerName.isNullOrBlank()) {
                Text(
                    text = if (round > 1) "$drawerName is drawing next!" else "$drawerName will draw first!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = if (round > 1) "Round $round is about to begin!" else "Get ready to draw and guess!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
