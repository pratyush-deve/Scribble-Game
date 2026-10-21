package com.pratyush.scribble.feature.results.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratyush.scribble.core.designsystem.component.AvatarBadge
import com.pratyush.scribble.core.designsystem.component.ScribblePrimaryButton
import com.pratyush.scribble.core.designsystem.component.ScribbleSecondaryButton
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.ScribbleTheme
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCoral
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantMint
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.PlayerScore
import com.pratyush.scribble.core.ui.insets.ScribbleScreenContainer
import com.pratyush.scribble.feature.results.presentation.component.WinnerPodium
import com.pratyush.scribble.feature.results.presentation.state.ResultsUiState

@Composable
fun ResultsScreen(
    uiState: ResultsUiState,
    onPlayAgain: () -> Unit,
    onReturnToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScribbleScreenContainer(
        modifier = modifier,
        applyStatusBarInset = true,
        applyNavigationBarInset = true
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MATCH RESULTS",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = VibrantCyan
                )

                if (uiState.winReason != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(VibrantMint.copy(alpha = 0.2f))
                            .border(1.5.dp, VibrantMint, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🏆 " + uiState.winReason,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VibrantMint
                        )
                    }
                } else {
                    Text(
                        text = "Room ${uiState.roomCode} • All ${uiState.totalRounds} rounds completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Winner Podium (Top 3)
                WinnerPodium(
                    rankings = uiState.rankings,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Full Leaderboard list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.rankings, key = { it.player.id }) { score ->
                        val isLocal = score.player.id == uiState.localPlayerId
                        val isPlayerDeparted = score.isDeparted || score.player.isDeparted
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface)
                                .border(
                                    width = if (isLocal) 2.dp else 1.dp,
                                    color = if (isLocal) VibrantViolet else DarkSurfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${score.rank}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.size(32.dp)
                                )

                                AvatarBadge(emoji = score.player.avatarId.emoji, size = 36.dp)

                                Spacer(modifier = Modifier.size(12.dp))

                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isLocal) "${score.player.name} (You)" else score.player.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isLocal) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isPlayerDeparted) TextSecondary else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (isPlayerDeparted) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = com.pratyush.scribble.core.designsystem.theme.ErrorRed.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, com.pratyush.scribble.core.designsystem.theme.ErrorRed.copy(alpha = 0.6f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                                    contentDescription = "Left game",
                                                    tint = com.pratyush.scribble.core.designsystem.theme.ErrorRed,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "LEFT",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = com.pratyush.scribble.core.designsystem.theme.ErrorRed
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "${score.score} pts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPlayerDeparted) TextSecondary else VibrantMint
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Voting Status & Countdown (Only shown for normal completed matches where replay voting is active)
                if (!uiState.isInsufficientPlayersDeparture) {
                    if (uiState.votingTimeRemainingSeconds > 0) {
                        Text(
                            text = "Voting ends in ${uiState.votingTimeRemainingSeconds}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibrantCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Voting closed",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    // Voted Players Avatars & Names
                    if (uiState.votedPlayers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Voted: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            uiState.votedPlayers.forEach { player ->
                                Spacer(modifier = Modifier.width(4.dp))
                                AvatarBadge(emoji = player.avatarId.emoji, size = 20.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (player.id == uiState.localPlayerId) "You" else player.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VibrantMint,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Action Buttons
                if (uiState.isInsufficientPlayersDeparture) {
                    // Departure / Insufficient players: Only Back to Home is available
                    ScribblePrimaryButton(
                        text = "Back to Home",
                        onClick = onReturnToHome,
                        containerColor = VibrantViolet
                    )
                } else {
                    val voteText = if (uiState.requiredMajority > 1) {
                        "Play Again (${uiState.playAgainVotes}/${uiState.requiredMajority})"
                    } else {
                        "Play Again"
                    }

                    ScribblePrimaryButton(
                        text = voteText,
                        onClick = onPlayAgain,
                        containerColor = if (uiState.hasVotedPlayAgain) VibrantMint else VibrantViolet
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ScribbleSecondaryButton(
                        text = "Back to Home",
                        onClick = onReturnToHome
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Failure transition overlay (Suppressed for active match departure)
            if (uiState.isVoteFailed && !uiState.isInsufficientPlayersDeparture) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.82f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .border(1.5.dp, VibrantViolet, RoundedCornerShape(16.dp))
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "⏳",
                                fontSize = 36.sp
                            )
                            Text(
                                text = uiState.failureMessage,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = uiState.failureSupportingText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = VibrantCyan,
                                trackColor = DarkSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
fun ResultsScreenPreview() {
    ScribbleTheme {
        ResultsScreen(
            uiState = ResultsUiState(
                roomCode = "SCR1B",
                rankings = listOf(
                    PlayerScore(Player("1", "Winner", AvatarId.FOX), 1, 450),
                    PlayerScore(Player("2", "RunnerUp", AvatarId.CAT), 2, 380),
                    PlayerScore(Player("3", "Third", AvatarId.PANDA), 3, 210)
                )
            ),
            onPlayAgain = {},
            onReturnToHome = {}
        )
    }
}
