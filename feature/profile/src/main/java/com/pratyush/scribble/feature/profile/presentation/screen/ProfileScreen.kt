package com.pratyush.scribble.feature.profile.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pratyush.scribble.core.designsystem.component.AvatarBadge
import com.pratyush.scribble.core.designsystem.component.ScribbleCard
import com.pratyush.scribble.core.designsystem.component.ScribblePrimaryButton
import com.pratyush.scribble.core.designsystem.component.ScribbleSecondaryButton
import com.pratyush.scribble.core.designsystem.theme.DarkBackground
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBarsPadding
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.ScribbleTheme
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantMint
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.ui.avatar.AvatarGridPicker
import com.pratyush.scribble.feature.profile.presentation.state.ProfileUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onAvatarSelected: (AvatarId) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("Profile & Customization", color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
            windowInsets = WindowInsets(0.dp)
        )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Avatar Display
                AvatarBadge(
                    emoji = uiState.selectedAvatar.emoji,
                    size = 96.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = uiState.selectedAvatar.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = VibrantCyan
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Display Name Input
                Text(
                    text = "DISPLAY NAME",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.editedDisplayName,
                    onValueChange = onDisplayNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.errorMessage != null,
                    supportingText = uiState.errorMessage?.let { errorText ->
                        { Text(text = errorText, color = MaterialTheme.colorScheme.error) }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = VibrantViolet,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Avatar Picker Grid
                Text(
                    text = "CHOOSE AVATAR",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                AvatarGridPicker(
                    selectedAvatar = uiState.selectedAvatar,
                    onAvatarSelected = onAvatarSelected,
                    modifier = Modifier.height(160.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Lifetime Statistics Section
                Text(
                    text = "LIFETIME STATISTICS",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                ScribbleCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Row 1: Played, Wins, Losses, Draws
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.userProfile.stats.gamesPlayed}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Played",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.userProfile.stats.wins}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = VibrantMint
                                )
                                Text(
                                    text = "Wins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.userProfile.stats.losses}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Losses",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.userProfile.stats.draws}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = VibrantViolet
                                )
                                Text(
                                    text = "Draws",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        androidx.compose.material3.HorizontalDivider(
                            color = DarkSurfaceVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        // Row 2: Win Rate, Streak, Total Score, Best Score
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val winRateStr = java.lang.String.format(java.util.Locale.US, "%.1f%%", uiState.userProfile.stats.winRate)
                                Text(
                                    text = winRateStr,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = VibrantCyan
                                )
                                Text(
                                    text = "Win Rate",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.userProfile.stats.winStreak}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = VibrantMint
                                )
                                Text(
                                    text = "Streak",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.userProfile.stats.totalScore}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = VibrantCyan
                                )
                                Text(
                                    text = "Total Pts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.userProfile.stats.bestScore}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = VibrantViolet
                                )
                                Text(
                                    text = "Best Pts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                ScribblePrimaryButton(
                    text = if (uiState.isSaving) "Saving..." else "Save Changes",
                    onClick = onSaveProfile
                )

                Spacer(modifier = Modifier.height(12.dp))

                var showSignOutDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                ScribbleSecondaryButton(
                    text = "Sign Out",
                    onClick = { showSignOutDialog = true }
                )

                if (showSignOutDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showSignOutDialog = false },
                        title = {
                            Text(
                                text = "Sign out?",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        },
                        text = {
                            Text(
                                text = "Are you sure you want to sign out? You will need to sign in again to access your profile and join games.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showSignOutDialog = false
                                    onSignOut()
                                }
                            ) {
                                Text("Sign out", color = com.pratyush.scribble.core.designsystem.theme.ErrorRed, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { showSignOutDialog = false }
                            ) {
                                Text("Cancel", color = TextSecondary)
                            }
                        },
                        containerColor = DarkSurface,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
fun ProfileScreenPreview() {
    ScribbleTheme {
        ProfileScreen(
            uiState = ProfileUiState(),
            onAvatarSelected = {},
            onDisplayNameChanged = {},
            onSaveProfile = {},
            onSignOut = {},
            onNavigateBack = {}
        )
    }
}
