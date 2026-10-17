package com.pratyush.scribble.feature.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceElevated
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantAmber
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantMint
import com.pratyush.scribble.core.model.UserProfile

@Composable
fun UserProfileHeader(
    userProfile: UserProfile,
    onProfileClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceElevated)
            .border(
                width = 1.dp,
                color = DarkSurfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onProfileClicked)
            .padding(vertical = 20.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large centered avatar cleanly against the card background (no cyan border/ring)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userProfile.avatarId.emoji,
                    fontSize = 42.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player Display Name
            Text(
                text = userProfile.displayName.ifBlank { "Player" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (userProfile.isGuest) {
                val limitReached = userProfile.isGuestLimitReached
                val pillText = if (limitReached) {
                    "Guest · 2 of 2 free games used (Limit Reached)"
                } else {
                    "Guest · ${userProfile.guestMatchesCompleted} of 2 free games used"
                }
                val pillColor = if (limitReached) VibrantAmber else VibrantCyan

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface.copy(alpha = 0.85f))
                        .border(1.dp, pillColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = if (limitReached) "⚠️" else "🎮", fontSize = 13.sp)
                    Text(
                        text = pillText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = pillColor
                    )
                }
            } else {
                // Player statistics in one horizontal row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Wins Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface.copy(alpha = 0.75f))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🏆", fontSize = 12.sp)
                        Text(
                            text = "Wins: ${userProfile.totalWins}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = VibrantMint
                        )
                    }

                    Spacer(modifier = Modifier.size(6.dp))

                    // 2. Score Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface.copy(alpha = 0.75f))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "⭐", fontSize = 12.sp)
                        Text(
                            text = "Score: ${userProfile.totalScore}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = VibrantAmber
                        )
                    }

                    Spacer(modifier = Modifier.size(6.dp))

                    // 3. Games Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface.copy(alpha = 0.75f))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🎮", fontSize = 12.sp)
                        Text(
                            text = "Games: ${userProfile.totalGamesPlayed}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
fun UserProfileHeaderPreview() {
    com.pratyush.scribble.core.designsystem.theme.ScribbleTheme {
        UserProfileHeader(
            userProfile = UserProfile(
                id = "preview_1",
                displayName = "TigerMaster",
                avatarId = com.pratyush.scribble.core.model.AvatarId.TIGER,
                totalWins = 14,
                totalScore = 4850,
                totalGamesPlayed = 22
            ),
            onProfileClicked = {}
        )
    }
}
