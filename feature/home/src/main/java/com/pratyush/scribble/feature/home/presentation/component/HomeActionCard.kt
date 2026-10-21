package com.pratyush.scribble.feature.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary

@Composable
fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    accentLightColor: Color,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animationAssetName: String? = null,
    animationContentScale: ContentScale = ContentScale.Crop,
    animationAlignment: Alignment = Alignment.Center,
    animationModifier: Modifier = Modifier.fillMaxSize(),
    animationAlpha: Float = 0.30f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 155.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(colors = gradientColors))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(26.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        // Lottie background animation
        if (!animationAssetName.isNullOrBlank()) {
            val composition by rememberLottieComposition(
                spec = LottieCompositionSpec.Asset(animationAssetName)
            )
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                isPlaying = true
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(26.dp)),
                contentAlignment = animationAlignment
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    contentScale = animationContentScale,
                    modifier = animationModifier
                        .alpha(animationAlpha)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large rounded-square container with icon
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(accentColor.copy(alpha = 0.25f))
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentLightColor,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            // Title and Subtitle with enhanced typography
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Large circular arrow indicator on the far right
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurface.copy(alpha = 0.75f))
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.35f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accentLightColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
