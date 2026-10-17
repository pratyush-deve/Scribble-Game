package com.pratyush.scribble.feature.lobby.presentation.screen

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratyush.scribble.core.designsystem.component.ScribblePrimaryButton
import com.pratyush.scribble.core.designsystem.component.ScribbleSecondaryButton
import com.pratyush.scribble.core.designsystem.theme.DarkBackground
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceElevated
import com.pratyush.scribble.core.designsystem.theme.ScribbleTheme
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.Room
import com.pratyush.scribble.core.model.RoomCode
import com.pratyush.scribble.core.model.WordDifficulty
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBarsPadding
import com.pratyush.scribble.feature.lobby.presentation.component.PlayerLobbyCard
import com.pratyush.scribble.feature.lobby.presentation.component.RoomSettingsSection
import com.pratyush.scribble.feature.lobby.presentation.state.LobbyUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    uiState: LobbyUiState,
    onRoundsChanged: (Int) -> Unit,
    onDrawTimeChanged: (Int) -> Unit,
    onDifficultyChanged: (WordDifficulty) -> Unit,
    onAvatarSelected: (AvatarId) -> Unit = {},
    onStartGame: () -> Unit,
    onLeaveRoom: () -> Unit,
    onNavigateToGame: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    androidx.activity.compose.BackHandler {
        onLeaveRoom()
    }

    LaunchedEffect(uiState.isGameStarted) {
        if (uiState.isGameStarted && uiState.room != null) {
            onNavigateToGame(uiState.room.code.value)
        }
    }

    if (uiState.isRoomClosedByHost) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onLeaveRoom,
            containerColor = com.pratyush.scribble.core.designsystem.theme.DarkSurface,
            title = {
                Text(
                    text = "Room Closed",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "The host closed this room.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                ScribblePrimaryButton(
                    text = "Return to Home",
                    onClick = onLeaveRoom,
                    containerColor = VibrantViolet
                )
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .systemBarsPadding()
    ) {
        TopAppBar(
            title = { Text("Game Lobby", color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onLeaveRoom) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Leave Room",
                        tint = TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
            windowInsets = WindowInsets(0.dp)
        )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Room Code Card
                item {
                    val roomCode = uiState.displayRoomCode
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceElevated)
                            .clickable {
                                clipboardManager.setText(AnnotatedString(roomCode))
                                Toast.makeText(context, "Room Code Copied!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ROOM CODE",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Spacer(modifier = Modifier.size(48.dp))
                                Text(
                                    text = roomCode,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 4.sp,
                                    color = VibrantCyan
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(roomCode))
                                        Toast.makeText(context, "Room Code Copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy room code",
                                        tint = VibrantCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Players Count & List
                item {
                    val currentCount = uiState.room?.players?.size ?: 0
                    val maxCount = uiState.room?.settings?.maxPlayers ?: 8
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PLAYERS",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "$currentCount / $maxCount Players",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibrantViolet,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(color = VibrantViolet)
                        }
                    }
                } else if (uiState.errorMessage != null && uiState.room == null && !uiState.isRoomClosedByHost) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceElevated)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                color = com.pratyush.scribble.core.designsystem.theme.ErrorRed,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(uiState.room?.players.orEmpty(), key = { it.id }) { player ->
                        PlayerLobbyCard(
                            player = player,
                            isLocalPlayer = player.id == uiState.localPlayerId
                        )
                    }
                }


                // Settings
                uiState.room?.let { r ->
                    item {
                        RoomSettingsSection(
                            settings = r.settings,
                            isHost = uiState.isHost,
                            onRoundsChanged = onRoundsChanged,
                            onDrawTimeChanged = onDrawTimeChanged,
                            onDifficultyChanged = onDifficultyChanged
                        )
                    }
                }
            }

            // Bottom Start / Waiting Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                if (uiState.isHost) {
                    ScribblePrimaryButton(
                        text = "Start Game",
                        onClick = onStartGame,
                        enabled = uiState.canStartGame,
                        containerColor = VibrantViolet
                    )
                } else {
                    ScribbleSecondaryButton(
                        text = "Waiting for host to start...",
                        onClick = {},
                        enabled = false
                    )
                }
            }
        }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
fun LobbyScreenPreview() {
    ScribbleTheme {
        LobbyScreen(
            uiState = LobbyUiState(
                isHost = true,
                room = Room(
                    id = "room_1",
                    code = RoomCode("SCR1B"),
                    hostId = "p1",
                    players = listOf(
                        Player("p1", "Alpha", AvatarId.CAT, isHost = true),
                        Player("p2", "Beta", AvatarId.DOG, isHost = false)
                    )
                )
            ),
            onRoundsChanged = {},
            onDrawTimeChanged = {},
            onDifficultyChanged = {},
            onStartGame = {},
            onLeaveRoom = {},
            onNavigateToGame = {}
        )
    }
}
