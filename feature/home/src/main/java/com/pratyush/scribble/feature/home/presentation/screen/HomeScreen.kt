package com.pratyush.scribble.feature.home.presentation.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceElevated
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.ScribbleTheme
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.UserProfile
import com.pratyush.scribble.core.ui.insets.ScribbleScreenContainer
import com.pratyush.scribble.feature.home.presentation.component.GuestLimitDialog
import com.pratyush.scribble.feature.home.presentation.component.HostRoomDialog
import com.pratyush.scribble.feature.home.presentation.component.HowToPlayDialog
import com.pratyush.scribble.feature.home.presentation.component.JoinRoomDialog
import com.pratyush.scribble.feature.home.presentation.component.UserProfileHeader
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

            // B. Player identity card
            if (uiState.userProfile != null) {
                UserProfileHeader(
                    userProfile = uiState.userProfile,
                    onProfileClicked = onProfileClicked
                )
            } else {
                // Shimmer / loading placeholder card (matching contact-card layout)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkSurfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                        )
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

            Spacer(modifier = Modifier.height(24.dp))

            // C. Main Play Area Header
            Text(
                text = "PLAY MULTIPLAYER",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Two-column row with equal-width Host and Join cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Host a Room Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(DarkSurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = VibrantViolet.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable(onClick = onHostRoomClicked)
                        .padding(vertical = 20.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(VibrantViolet.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Host Icon",
                                tint = VibrantViolet,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Host a Room",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Create a game",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                // Join a Room Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(DarkSurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = DarkSurfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable(onClick = onJoinRoomClicked)
                        .padding(vertical = 20.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(VibrantCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = "Join Icon",
                                tint = VibrantCyan,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Join a Room",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Enter a code",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // D. Dedicated "How to Play" Action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceElevated.copy(alpha = 0.7f))
                    .border(
                        width = 1.dp,
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(onClick = onHowToPlayClicked)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VibrantCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "How to Play",
                            tint = VibrantCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "How to Play Scribble",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Learn the 7 simple rules of the game",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View Guide",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Host Dialog
        if (uiState.isHostRoomDialogOpen) {
            HostRoomDialog(
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
                onDismiss = onDismissGuestLimitDialog
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
                    totalScore = 6500
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
