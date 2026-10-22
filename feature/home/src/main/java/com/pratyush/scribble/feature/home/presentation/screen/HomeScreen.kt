package com.pratyush.scribble.feature.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceElevated
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.ScribbleTheme
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantAmber
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantCyanLight
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.designsystem.theme.VibrantVioletLight
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.UserProfile
import com.pratyush.scribble.core.ui.insets.ScribbleScreenContainer
import com.pratyush.scribble.feature.home.presentation.component.GuestLimitDialog
import com.pratyush.scribble.feature.home.presentation.component.HomeActionCard
import com.pratyush.scribble.feature.home.presentation.component.HostRoomDialog
import com.pratyush.scribble.feature.home.presentation.component.HowToPlayDialog
import com.pratyush.scribble.feature.home.presentation.component.JoinRoomDialog
import com.pratyush.scribble.feature.home.presentation.component.PlayerStatsRow
import com.pratyush.scribble.feature.home.presentation.component.ProfileShowcaseCard
import com.pratyush.scribble.feature.home.presentation.component.ProfileShowcasePlaceholder
import com.pratyush.scribble.feature.home.presentation.state.HomeUiState

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onHostRoomClicked: () -> Unit,
    onJoinRoomClicked: () -> Unit,
    onProfileClicked: () -> Unit,
    onRoomCodeChanged: (String) -> Unit,
    onConfirmJoinRoom: () -> Unit,
    onConfirmHostRoom: () -> Unit,
    onDismissDialogs: () -> Unit,
    modifier: Modifier = Modifier,
    onHowToPlayClicked: () -> Unit = {},
    onDismissHowToPlay: () -> Unit = onDismissDialogs,
    onUpgradeToGoogleClicked: () -> Unit = {},
    onDismissGuestLimitDialog: () -> Unit = onDismissDialogs
) {
    ScribbleScreenContainer(
        modifier = modifier,
        applyStatusBarInset = true,
        applyNavigationBarInset = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // A. Compact top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(VibrantViolet, VibrantCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = "Logo",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "SCRIBBLE",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Draw. Guess. Outsmart your friends.",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibrantCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // B. Player identity showcase card & independent statistics
            if (uiState.userProfile != null) {
                ProfileShowcaseCard(
                    userProfile = uiState.userProfile,
                    onProfileClicked = onProfileClicked
                )

                Spacer(modifier = Modifier.height(14.dp))

                PlayerStatsRow(
                    totalWins = uiState.userProfile.totalWins,
                    totalScore = uiState.userProfile.totalScore,
                    totalGamesPlayed = uiState.userProfile.totalGamesPlayed
                )
            } else {
                ProfileShowcasePlaceholder()
            }

            Spacer(modifier = Modifier.height(24.dp))

            // C. Main Play Area Header
            Text(
                text = "PLAY MULTIPLAYER",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Host a Room Card (Large full-width, Purple theme)
            HomeActionCard(
                title = "Host a Room",
                subtitle = "Create a game and invite your friends to play",
                icon = Icons.Default.Add,
                accentColor = VibrantViolet,
                accentLightColor = VibrantVioletLight,
                gradientColors = listOf(Color(0xFF261A45), Color(0xFF161829)),
                animationAssetName = "draw.json",
                animationContentScale = androidx.compose.ui.layout.ContentScale.Fit,
                animationAlignment = Alignment.Center,
                animationModifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 10.dp),
                animationAlpha = 0.50f,
                onClick = onHostRoomClicked
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Join a Room Card (Large full-width, Cyan theme)
            HomeActionCard(
                title = "Join a Room",
                subtitle = "Enter a code to join your friends' game",
                icon = Icons.AutoMirrored.Filled.Login,
                accentColor = VibrantCyan,
                accentLightColor = VibrantCyanLight,
                gradientColors = listOf(Color(0xFF102B37), Color(0xFF141A29)),
                animationAssetName = "join.json",
                onClick = onJoinRoomClicked
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. How to Play Scribble Card (Large full-width, Gold/Amber theme)
            HomeActionCard(
                title = "How to Play Scribble",
                subtitle = "Learn the 7 simple rules of the game",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                accentColor = VibrantAmber,
                accentLightColor = Color(0xFFFCD34D),
                gradientColors = listOf(Color(0xFF2E2310), Color(0xFF191826)),
                animationAssetName = "how_to_play.json",
                animationContentScale = androidx.compose.ui.layout.ContentScale.Fit,
                animationAlignment = Alignment.CenterEnd,
                animationModifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 10.dp)
                    .padding(end = 20.dp),
                animationAlpha = 0.50f,
                onClick = onHowToPlayClicked
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Host Dialog
        if (uiState.isHostRoomDialogOpen) {
            HostRoomDialog(
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onConfirm = onConfirmHostRoom,
                onDismiss = onDismissDialogs
            )
        }

        // Join Dialog
        if (uiState.isJoinRoomDialogOpen) {
            JoinRoomDialog(
                roomCode = uiState.roomCodeInput,
                onRoomCodeChanged = onRoomCodeChanged,
                joinRoomState = uiState.joinRoomState,
                errorMessage = uiState.errorMessage,
                onConfirm = onConfirmJoinRoom,
                onDismiss = onDismissDialogs
            )
        }

        // How to Play Dialog
        if (uiState.isHowToPlayDialogOpen) {
            HowToPlayDialog(
                onDismiss = onDismissHowToPlay
            )
        }

        // Guest Limit Dialog
        if (uiState.isGuestLimitDialogOpen) {
            GuestLimitDialog(
                isLoading = uiState.isLinkingAccount,
                onUpgradeClicked = onUpgradeToGoogleClicked,
                onDismiss = onDismissGuestLimitDialog,
                errorMessage = uiState.errorMessage
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
fun HomeScreenPreview() {
    ScribbleTheme {
        HomeScreen(
            uiState = HomeUiState(
                userProfile = UserProfile(
                    id = "preview_1",
                    displayName = "ArtistPro",
                    avatarId = AvatarId.FOX,
                    totalWins = 12,
                    totalScore = 6500,
                    totalGamesPlayed = 25
                )
            ),
            onHostRoomClicked = {},
            onJoinRoomClicked = {},
            onProfileClicked = {},
            onRoomCodeChanged = {},
            onConfirmJoinRoom = {},
            onConfirmHostRoom = {},
            onDismissDialogs = {}
        )
    }
}
