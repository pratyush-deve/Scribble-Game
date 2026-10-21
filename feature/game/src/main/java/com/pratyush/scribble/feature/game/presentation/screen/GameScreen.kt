package com.pratyush.scribble.feature.game.presentation.screen

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceElevated
import com.pratyush.scribble.core.designsystem.theme.ScribbleTheme
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantAmber
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.model.DrawingReaction
import com.pratyush.scribble.core.model.DrawingStroke
import com.pratyush.scribble.core.model.DrawingTool
import com.pratyush.scribble.core.model.GamePhase
import com.pratyush.scribble.core.model.MaskedWord
import com.pratyush.scribble.core.ui.insets.ScribbleScreenContainer
import com.pratyush.scribble.feature.game.presentation.component.ChatInputBar
import com.pratyush.scribble.feature.game.presentation.component.ChatMessageBubble
import com.pratyush.scribble.feature.game.presentation.component.DrawerStatusIndicator
import com.pratyush.scribble.feature.game.presentation.component.DrawingToolBar
import com.pratyush.scribble.feature.game.presentation.component.GameCountdownOverlay
import com.pratyush.scribble.feature.game.presentation.component.LeaveGameConfirmationDialog
import com.pratyush.scribble.feature.game.presentation.component.ScribbleDrawingCanvas
import com.pratyush.scribble.feature.game.presentation.component.WordSelectionModal
import com.pratyush.scribble.feature.game.presentation.state.DrawingCanvasState
import com.pratyush.scribble.feature.game.presentation.state.GameUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    uiState: GameUiState,
    canvasState: DrawingCanvasState,
    onStrokeCompleted: (DrawingStroke) -> Unit,
    onEraserDragSegment: (p1x: Float, p1y: Float, p2x: Float, p2y: Float, radius: Float) -> Unit,
    onEraserGestureCompleted: () -> Unit,
    onUndoClicked: () -> Unit,
    onClearClicked: () -> Unit,
    onToolSelected: (DrawingTool) -> Unit,
    onColorSelected: (Color) -> Unit,
    onStrokeWidthSelected: (Float) -> Unit,
    onWordSelected: (String) -> Unit,
    onChatInputChanged: (String) -> Unit,
    onSendGuess: () -> Unit,
    onRequestLeaveGame: () -> Unit,
    onConfirmLeaveGame: () -> Unit,
    onDismissLeaveConfirmation: () -> Unit,
    onDepartedNavigateHome: () -> Unit,
    onNavigateToResults: (roomCode: String, defaultWinReason: String?) -> Unit,
    onReactionClicked: (DrawingReaction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    val chatListState = rememberLazyListState()
    var previousMessageCount by remember { mutableIntStateOf(0) }

    // Auto-scroll on new messages or initial load
    LaunchedEffect(uiState.chatMessages.size, uiState.chatMessages.lastOrNull()?.id) {
        val messageCount = uiState.chatMessages.size
        if (messageCount > 0) {
            // Ensure scrolling occurs after the updated message list has been composed
            snapshotFlow { chatListState.layoutInfo.totalItemsCount }
                .filter { it >= messageCount }
                .first()

            val isInitialLoad = previousMessageCount == 0
            val diff = messageCount - previousMessageCount
            previousMessageCount = messageCount

            if (isInitialLoad) {
                chatListState.scrollToItem(0)
            } else if (diff <= 2 && chatListState.firstVisibleItemIndex <= 3) {
                chatListState.animateScrollToItem(0)
            } else {
                chatListState.scrollToItem(0)
            }
        } else {
            previousMessageCount = 0
        }
    }

    // Auto-scroll when keyboard appears, disappears, or available screen height adjusts
    LaunchedEffect(imeBottom) {
        if (uiState.chatMessages.isNotEmpty()) {
            chatListState.scrollToItem(0)
        }
    }

    LaunchedEffect(chatListState) {
        snapshotFlow { chatListState.layoutInfo.viewportSize.height }
            .distinctUntilChanged()
            .collect {
                if (uiState.chatMessages.isNotEmpty() && chatListState.firstVisibleItemIndex <= 1) {
                    chatListState.scrollToItem(0)
                }
            }
    }

    // Intercept hardware/system back button while game is active
    val showReactions = !uiState.isLocalDrawer &&
        uiState.phase == GamePhase.DRAWING &&
        !uiState.isRoundEnded &&
        uiState.remainingTimeSeconds > 0 &&
        !uiState.isGameFinished &&
        !uiState.isPlayerDeparted &&
        !uiState.hasVotedThisTurn

    BackHandler(enabled = !uiState.isGameFinished && !uiState.isPlayerDeparted) {
        onRequestLeaveGame()
    }

    var hasNavigatedToResults by remember { mutableStateOf(false) }
    var hasNavigatedDeparted by remember { mutableStateOf(false) }

    // When the game concludes (normal finish or opponent default win)
    LaunchedEffect(uiState.isGameFinished) {
        if (uiState.isGameFinished && !hasNavigatedToResults) {
            hasNavigatedToResults = true
            onNavigateToResults(uiState.roomCode, uiState.defaultWinReason)
        }
    }

    // When this player confirms departure, navigate back to home screen
    LaunchedEffect(uiState.isPlayerDeparted) {
        if (uiState.isPlayerDeparted && !hasNavigatedDeparted) {
            hasNavigatedDeparted = true
            onDepartedNavigateHome()
        }
    }

    ScribbleScreenContainer(
        modifier = modifier,
        applyStatusBarInset = true,
        applyNavigationBarInset = true,
        applyImeInset = true
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Game Header (Compact: Back button, Round, Word/Hint, Timer)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onRequestLeaveGame, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Leave Game",
                            tint = TextPrimary
                        )
                    }

                    // Center Word Display
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ROUND ${uiState.currentRound} OF ${uiState.totalRounds}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = uiState.displayedWord.ifEmpty {
                                if (uiState.isLocalDrawer) uiState.drawerSecretWord.orEmpty() else uiState.maskedWord.hint
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = if (uiState.isLocalDrawer || uiState.isRoundEnded) 2.sp else 3.sp,
                            color = if (uiState.isLocalDrawer) VibrantCyan else TextPrimary
                        )
                    }

                    // Timer Capsule
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (uiState.remainingTimeSeconds <= 10) VibrantAmber else DarkSurfaceElevated)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${uiState.remainingTimeSeconds}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                        }
                    }
                }

                if (isLandscape) {
                    // LANDSCAPE ADAPTIVE SPLIT LAYOUT
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // Left: Drawing Canvas and Toolbar (60% width)
                        Column(
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxHeight()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ScribbleDrawingCanvas(
                                    strokes = canvasState.strokes,
                                    isEditable = uiState.isLocalDrawer && uiState.phase == GamePhase.DRAWING && !uiState.isGameFinished,
                                    onStrokeCompleted = onStrokeCompleted,
                                    onEraserDragSegment = onEraserDragSegment,
                                    onEraserGestureCompleted = onEraserGestureCompleted,
                                    currentColor = canvasState.currentColor,
                                    currentStrokeWidth = canvasState.currentStrokeWidth,
                                    currentTool = canvasState.currentTool,
                                    showReactions = showReactions,
                                    selectedReaction = uiState.selectedReaction,
                                    likeCount = uiState.likeCount,
                                    dislikeCount = uiState.dislikeCount,
                                    reactionErrorMessage = uiState.reactionErrorMessage,
                                    onReactionClicked = onReactionClicked
                                )
                            }
                            if (uiState.isLocalDrawer && uiState.phase == GamePhase.DRAWING && !uiState.isGameFinished) {
                                Spacer(modifier = Modifier.height(4.dp))
                                DrawingToolBar(
                                    selectedTool = canvasState.currentTool,
                                    onToolSelected = onToolSelected,
                                    selectedColor = canvasState.currentColor,
                                    onColorSelected = onColorSelected,
                                    currentStrokeWidth = canvasState.currentStrokeWidth,
                                    onStrokeWidthSelected = onStrokeWidthSelected,
                                    canUndo = canvasState.canUndo,
                                    onUndo = onUndoClicked,
                                    onClear = onClearClicked
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Right: Dedicated Chat Column (40% width)
                        Column(
                            modifier = Modifier
                                .weight(0.4f)
                                .fillMaxHeight()
                        ) {
                            if (!uiState.isLocalDrawer && uiState.phase == GamePhase.DRAWING && !uiState.isGameFinished && uiState.drawerPlayer != null) {
                                DrawerStatusIndicator(
                                    drawer = uiState.drawerPlayer,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            LazyColumn(
                                state = chatListState,
                                modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface)
                                .padding(8.dp),
                                reverseLayout = true
                            ) {
                                items(uiState.chatMessages.reversed(), key = { it.id }) { msg ->
                                    ChatMessageBubble(
                                        message = msg,
                                        senderPlayer = uiState.players.firstOrNull { it.id == msg.senderId }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            ChatInputBar(
                                value = uiState.chatInput,
                                onValueChange = onChatInputChanged,
                                onSend = onSendGuess,
                                enabled = !uiState.hasGuessedCorrectly && !uiState.isGameFinished
                            )
                        }
                    }
                } else {
                    // PORTRAIT VERTICALLY ARRANGED LAYOUT
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        // Drawing Canvas Area
                        Box(
                            modifier = Modifier
                                .weight(if (uiState.isLocalDrawer && uiState.phase == GamePhase.DRAWING) 0.50f else 0.48f)
                                .fillMaxWidth()
                        ) {
                            ScribbleDrawingCanvas(
                                strokes = canvasState.strokes,
                                isEditable = uiState.isLocalDrawer && uiState.phase == GamePhase.DRAWING && !uiState.isGameFinished,
                                onStrokeCompleted = onStrokeCompleted,
                                onEraserDragSegment = onEraserDragSegment,
                                onEraserGestureCompleted = onEraserGestureCompleted,
                                currentColor = canvasState.currentColor,
                                currentStrokeWidth = canvasState.currentStrokeWidth,
                                currentTool = canvasState.currentTool,
                                showReactions = showReactions,
                                selectedReaction = uiState.selectedReaction,
                                likeCount = uiState.likeCount,
                                dislikeCount = uiState.dislikeCount,
                                reactionErrorMessage = uiState.reactionErrorMessage,
                                onReactionClicked = onReactionClicked
                            )
                        }

                        // Drawing Toolbar (Visible when current player is drawer during DRAWING phase)
                        if (uiState.isLocalDrawer && uiState.phase == GamePhase.DRAWING && !uiState.isGameFinished) {
                            Spacer(modifier = Modifier.height(6.dp))
                            DrawingToolBar(
                                selectedTool = canvasState.currentTool,
                                onToolSelected = onToolSelected,
                                selectedColor = canvasState.currentColor,
                                onColorSelected = onColorSelected,
                                currentStrokeWidth = canvasState.currentStrokeWidth,
                                onStrokeWidthSelected = onStrokeWidthSelected,
                                canUndo = canvasState.canUndo,
                                onUndo = onUndoClicked,
                                onClear = onClearClicked
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Dedicated Portrait Chat Area (Visible for both drawer and guessers)
                        Column(
                            modifier = Modifier
                                .weight(if (uiState.isLocalDrawer && uiState.phase == GamePhase.DRAWING) 0.35f else 0.45f)
                                .fillMaxWidth()
                        ) {
                            if (!uiState.isLocalDrawer && uiState.phase == GamePhase.DRAWING && !uiState.isGameFinished && uiState.drawerPlayer != null) {
                                DrawerStatusIndicator(
                                    drawer = uiState.drawerPlayer,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            LazyColumn(
                                state = chatListState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurface)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                reverseLayout = true
                            ) {
                                items(uiState.chatMessages.reversed(), key = { it.id }) { msg ->
                                    ChatMessageBubble(
                                        message = msg,
                                        senderPlayer = uiState.players.firstOrNull { it.id == msg.senderId }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            ChatInputBar(
                                value = uiState.chatInput,
                                onValueChange = onChatInputChanged,
                                onSend = onSendGuess,
                                enabled = !uiState.hasGuessedCorrectly && !uiState.isGameFinished
                            )
                        }
                    }
                }
            }

            // Shared Authoritative Countdown Overlay (Animated 5..1 center screen countdown)
            if (uiState.phase == GamePhase.COUNTDOWN && !uiState.isGameFinished) {
                GameCountdownOverlay(
                    remainingSeconds = uiState.remainingTimeSeconds,
                    round = uiState.currentRound,
                    totalRounds = uiState.totalRounds,
                    drawerName = uiState.drawerPlayer?.name
                )
            }

            // Word Selection Overlay (Presented when choosing word)
            if (uiState.phase == GamePhase.SELECTING_WORD && !uiState.isGameFinished) {
                WordSelectionModal(
                    choices = uiState.wordChoices,
                    isDrawer = uiState.isLocalDrawer,
                    onWordSelected = onWordSelected,
                    drawerName = uiState.drawerPlayer?.name,
                    remainingSeconds = uiState.remainingTimeSeconds
                )
            }

            // Exit Game Confirmation Dialog
            if (uiState.showLeaveConfirmationDialog) {
                LeaveGameConfirmationDialog(
                    onConfirmLeave = onConfirmLeaveGame,
                    onDismiss = onDismissLeaveConfirmation
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
fun GameScreenDrawerPreview() {
    ScribbleTheme {
        GameScreen(
            uiState = GameUiState(
                isLocalDrawer = true,
                phase = GamePhase.DRAWING,
                displayedWord = "PENGUIN",
                drawerSecretWord = "PENGUIN"
            ),
            canvasState = DrawingCanvasState(),
            onStrokeCompleted = {},
            onEraserDragSegment = { _, _, _, _, _ -> },
            onEraserGestureCompleted = {},
            onUndoClicked = {},
            onClearClicked = {},
            onToolSelected = {},
            onColorSelected = {},
            onStrokeWidthSelected = {},
            onWordSelected = {},
            onChatInputChanged = {},
            onSendGuess = {},
            onRequestLeaveGame = {},
            onConfirmLeaveGame = {},
            onDismissLeaveConfirmation = {},
            onDepartedNavigateHome = {},
            onNavigateToResults = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
fun GameScreenGuesserPreview() {
    val masked = MaskedWord.fromWord("PENGUIN")
    ScribbleTheme {
        GameScreen(
            uiState = GameUiState(
                isLocalDrawer = false,
                phase = GamePhase.DRAWING,
                displayedWord = masked.hint,
                drawerSecretWord = null,
                maskedWord = masked
            ),
            canvasState = DrawingCanvasState(),
            onStrokeCompleted = {},
            onEraserDragSegment = { _, _, _, _, _ -> },
            onEraserGestureCompleted = {},
            onUndoClicked = {},
            onClearClicked = {},
            onToolSelected = {},
            onColorSelected = {},
            onStrokeWidthSelected = {},
            onWordSelected = {},
            onChatInputChanged = {},
            onSendGuess = {},
            onRequestLeaveGame = {},
            onConfirmLeaveGame = {},
            onDismissLeaveConfirmation = {},
            onDepartedNavigateHome = {},
            onNavigateToResults = { _, _ -> }
        )
    }
}
