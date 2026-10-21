package com.pratyush.scribble.feature.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.ScribbleTheme
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantAmber
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.UserProfile
import com.pratyush.scribble.core.network.di.NetworkModule
import com.pratyush.scribble.core.network.repository.AvatarAnimationRepository

/**
 * Redesigned hero showcase profile card:
 * Displays a large player avatar animation that is seamlessly integrated into the card background
 * (without enclosing circle borders). A subtle black gradient at the bottom ensures the profile name
 * is clearly visible in white text. A faded, translucent edit icon at the top right signals editability.
 */
@Composable
fun ProfileShowcaseCard(
    userProfile: UserProfile,
    onProfileClicked: () -> Unit,
    modifier: Modifier = Modifier,
    avatarAnimationRepository: AvatarAnimationRepository = remember { NetworkModule.provideAvatarAnimationRepository() }
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E2333),
                        Color(0xFF151824)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = DarkSurfaceVariant.copy(alpha = 0.85f),
                shape = RoundedCornerShape(26.dp)
            )
            .clickable(onClick = onProfileClicked),
        contentAlignment = Alignment.Center
    ) {
        // 1. Black gradient overlay towards the bottom of the card for high contrast behind the name
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.40f to Color.Transparent,
                        0.72f to Color.Black.copy(alpha = 0.40f),
                        1.0f to Color.Black.copy(alpha = 0.78f)
                    )
                )
        )

        // 2. Main content: Hero avatar animation + profile name at the bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 20.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val animationUrl = remember(userProfile.avatarId) {
                avatarAnimationRepository.getAnimationUrl(userProfile.avatarId)
            }

            val compositionResult = rememberLottieComposition(
                spec = LottieCompositionSpec.Url(animationUrl)
            )

            val progress by animateLottieCompositionAsState(
                composition = compositionResult.value,
                iterations = LottieConstants.IterateForever,
                isPlaying = true
            )

            // Large animation container without enclosing circular border - part of the card background
            val animationScale = if (userProfile.avatarId == AvatarId.LION) 1.45f else 1.0f

            Box(
                modifier = Modifier.size(170.dp),
                contentAlignment = Alignment.Center
            ) {
                if (compositionResult.value != null) {
                    LottieAnimation(
                        composition = compositionResult.value,
                        progress = { progress },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(animationScale)
                    )
                } else {
                    // Polished static emoji fallback while downloading, offline, or on failure
                    Text(
                        text = userProfile.avatarId.emoji,
                        fontSize = 76.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player Display Name at the bottom of the card in crisp white text
            Text(
                text = userProfile.displayName.ifBlank { "Player" },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Optional guest limitation pill
            if (userProfile.isGuest) {
                Spacer(modifier = Modifier.height(8.dp))
                val limitReached = userProfile.isGuestLimitReached
                val pillText = if (limitReached) {
                    "Guest · 2 of 2 free games used (Limit Reached)"
                } else {
                    "Guest · ${userProfile.guestMatchesCompleted} of 2 free games used"
                }
                val pillColor = if (limitReached) VibrantAmber else VibrantCyan

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(DarkSurface.copy(alpha = 0.90f))
                        .border(1.dp, pillColor.copy(alpha = 0.5f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = if (limitReached) "⚠️" else "🎮", fontSize = 12.sp)
                    Text(
                        text = pillText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = pillColor
                    )
                }
            }
        }

        // 3. Faded, very translucent edit icon at the top right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 14.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Profile",
                tint = Color.White.copy(alpha = 0.42f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Placeholder card displayed while user profile is being fetched.
 */
@Composable
fun ProfileShowcasePlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E2333),
                        Color(0xFF151824)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = DarkSurfaceVariant.copy(alpha = 0.85f),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(170.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎨",
                    fontSize = 76.sp,
                    color = TextSecondary.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Loading Profile...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
fun ProfileShowcaseCardPreview() {
    ScribbleTheme {
        ProfileShowcaseCard(
            userProfile = UserProfile(
                id = "preview_1",
                displayName = "TigerMaster",
                avatarId = AvatarId.TIGER,
                totalWins = 14,
                totalScore = 4850,
                totalGamesPlayed = 22
            ),
            onProfileClicked = {}
        )
    }
}
