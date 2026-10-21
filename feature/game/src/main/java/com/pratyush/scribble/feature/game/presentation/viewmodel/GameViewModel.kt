package com.pratyush.scribble.feature.game.presentation.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.designsystem.theme.StrokeBlack
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.ChatMessage
import com.pratyush.scribble.core.model.ChatMessageType
import com.pratyush.scribble.core.model.DrawingReaction
import com.pratyush.scribble.core.model.DrawingStroke
import com.pratyush.scribble.core.model.DrawingTool
import com.pratyush.scribble.core.model.GamePhase
import com.pratyush.scribble.core.model.GameStateSnapshot
import com.pratyush.scribble.core.model.MaskedWord
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.StrokeGeometry
import com.pratyush.scribble.core.model.WordChoice
import com.pratyush.scribble.core.model.WordDifficulty
import com.pratyush.scribble.core.network.repository.GameRepository
import com.pratyush.scribble.core.network.repository.RoomRepository
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository
import com.pratyush.scribble.feature.game.presentation.state.DrawingCanvasState
import com.pratyush.scribble.feature.game.presentation.state.GameUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

class GameViewModel(
    val roomCode: String,
    initialIsDrawer: Boolean = false,
    initialPlayers: List<Player>? = null,
    private val authRepository: AuthRepository? = null,
    private val roomRepository: RoomRepository? = null,
    private val gameRepository: GameRepository? = null
) : ViewModel() {

    private var activeRoomId: String? = null
    private var authoritativeSecretWord: String = if (gameRepository == null) "ELEPHANT" else ""
    private var isDepartureProcessed: Boolean = false
    private val strokeSequence = AtomicLong(1L)
    private var currentTurnId: String = ""
    private var currentTurnDeadline: Long = 0L

    private val hasInitializedGame = java.util.concurrent.atomic.AtomicBoolean(false)
    private val hasAdvancedToWordSelection = java.util.concurrent.atomic.AtomicBoolean(false)
    private val isAdvancingTurn = java.util.concurrent.atomic.AtomicBoolean(false)
    private var lastRevealedTurnId: String = ""
    private var roomDifficulty: WordDifficulty = WordDifficulty.MEDIUM

    val localPlayerId: String = authRepository?.currentUser?.value?.id ?: "p1"

    private val _gameUiState = MutableStateFlow(
        createInitialState(roomCode, initialIsDrawer, initialPlayers)
    )
    val gameUiState: StateFlow<GameUiState> = _gameUiState.asStateFlow()

    private val _canvasState = MutableStateFlow(DrawingCanvasState())
    val canvasState: StateFlow<DrawingCanvasState> = _canvasState.asStateFlow()

    // Undo history stack storing snapshots of the stroke list
    private val undoHistory = mutableListOf<List<DrawingStroke>>()
    private var preEraserGestureSnapshot: List<DrawingStroke>? = null

    private var timerJob: Job? = null
    private var strokesObserveJob: Job? = null
    private var reactionsObserveJob: Job? = null
    private var isSubmittingReaction: Boolean = false

    init {
        if (gameRepository != null && roomRepository != null) {
            connectMultiplayerGame()
        } else {
            startCountdownTimer()
        }
    }

    private fun createInitialState(
        roomCode: String,
        isDrawer: Boolean,
        customPlayers: List<Player>?
    ): GameUiState {
        if (gameRepository != null) {
            val defaultPlayers = customPlayers ?: emptyList()
            val initialDrawer = if (isDrawer) {
                defaultPlayers.firstOrNull { it.id == localPlayerId }
            } else null

            return GameUiState(
                roomCode = roomCode,
                phase = GamePhase.COUNTDOWN,
                isLocalDrawer = isDrawer,
                currentRound = 1,
                totalRounds = 3,
                remainingTimeSeconds = 5,
                displayedWord = "",
                drawerSecretWord = null,
                maskedWord = MaskedWord(length = 0, hint = ""),
                wordChoices = emptyList(),
                drawerPlayer = initialDrawer,
                players = defaultPlayers,
                chatMessages = emptyList()
            )
        } else {
            val defaultPlayers = customPlayers ?: listOf(
                Player(id = "p1", name = "Player 1", avatarId = AvatarId.CAT, isHost = true),
                Player(id = "p2", name = "Player 2", avatarId = AvatarId.DOG, isHost = false)
            )
            val initialDrawer = if (isDrawer) defaultPlayers[0] else defaultPlayers[1]
            val masked = MaskedWord.fromWord(authoritativeSecretWord)

            return GameUiState(
                roomCode = roomCode,
                phase = if (isDrawer) GamePhase.SELECTING_WORD else GamePhase.DRAWING,
                isLocalDrawer = isDrawer,
                currentRound = 1,
                totalRounds = 3,
                remainingTimeSeconds = 60,
                displayedWord = if (isDrawer) authoritativeSecretWord else masked.hint,
                drawerSecretWord = if (isDrawer) authoritativeSecretWord else null,
                maskedWord = masked,
                wordChoices = if (isDrawer) listOf(
                    WordChoice("1", "ELEPHANT", WordDifficulty.EASY),
                    WordChoice("2", "GUITAR", WordDifficulty.MEDIUM),
                    WordChoice("3", "SPACESHIP", WordDifficulty.HARD)
                ) else emptyList(),
                drawerPlayer = initialDrawer,
                players = defaultPlayers,
                chatMessages = emptyList()
            )
        }
    }

    private fun connectMultiplayerGame() {
        val gameRepo = gameRepository ?: return
        val roomRepo = roomRepository ?: return

        viewModelScope.launch {
            val resolvedId = roomRepo.resolveRoomId(roomCode) ?: "room_$roomCode"
            activeRoomId = resolvedId

            // 1. Observe Room membership, presence, scores
            launch {
                roomRepo.observeRoom(resolvedId).collectLatest { room ->
                    if (room != null) {
                        val activePlayers = room.players
                        val isHost = room.hostId == localPlayerId
                        roomDifficulty = room.settings.wordDifficulty

                        _gameUiState.update { current ->
                            current.copy(
                                players = activePlayers,
                                totalRounds = room.settings.rounds,
                                drawTimeSeconds = room.settings.drawTimeSeconds
                            )
                        }

                        // If host and gameState not yet initialized, host initializes the game
                        if (isHost && !hasInitializedGame.get() && activePlayers.size >= 2) {
                            if (hasInitializedGame.compareAndSet(false, true)) {
                                gameRepo.initializeGame(
                                    roomId = resolvedId,
                                    players = activePlayers,
                                    totalRounds = room.settings.rounds,
                                    drawTimeSeconds = room.settings.drawTimeSeconds,
                                    difficulty = room.settings.wordDifficulty
                                )
                            }
                        }
                    } else {
                        // Room was closed
                        _gameUiState.update { it.copy(isGameFinished = true) }
                    }
                }
            }

            // 2. Observe Authoritative Game State
            launch {
                gameRepo.observeGameState(resolvedId).collectLatest { snapshot ->
                    if (snapshot != null) {
                        hasInitializedGame.set(true)
                        handleGameStateSnapshot(snapshot, resolvedId)
                    }
                }
            }

            // 3. Observe Chat Messages
            launch {
                gameRepo.observeChatMessages(resolvedId).collectLatest { messages ->
                    _gameUiState.update { current ->
                        current.copy(chatMessages = messages)
                    }
                }
            }

            // 4. Observe Drawer Secrets (Private to drawer)
            launch {
                gameRepo.observeDrawerSecret(resolvedId, localPlayerId).collectLatest { secret ->
                    if (secret != null && (currentTurnId.isEmpty() || secret.turnId == currentTurnId)) {
                        val selWord = secret.selectedWord
                        if (selWord != null) {
                            authoritativeSecretWord = selWord
                        }
                        _gameUiState.update { current ->
                            if (current.isLocalDrawer) {
                                current.copy(
                                    wordChoices = secret.wordChoices,
                                    drawerSecretWord = selWord,
                                    displayedWord = selWord ?: current.displayedWord
                                )
                            } else current
                        }
                    }
                }
            }
        }

        // Start authoritative deadline ticker
        startDeadlineTimer()
    }

    private fun handleGameStateSnapshot(snapshot: GameStateSnapshot, roomId: String) {
        val isDrawer = snapshot.drawerId == localPlayerId
        val isTurnChanged = currentTurnId != snapshot.turnId

        // Eligible guessers: all players except active drawer
        val eligibleGuesserIds = if (_gameUiState.value.players.isNotEmpty()) {
            _gameUiState.value.players.filter { it.id != snapshot.drawerId }.map { it.id }
        } else {
            snapshot.turnOrder.filter { it != snapshot.drawerId }
        }
        val allGuessed = eligibleGuesserIds.isNotEmpty() && eligibleGuesserIds.all { snapshot.correctGuessers.containsKey(it) }

        if (isTurnChanged) {
            currentTurnId = snapshot.turnId
            currentTurnDeadline = snapshot.turnDeadline
            hasAdvancedToWordSelection.set(false)
            isAdvancingTurn.set(false)
            // Turn changed -> re-listen to strokes for this turn
            observeStrokesForTurn(roomId, snapshot.turnId, isDrawer)
            observeReactionsForTurn(roomId, snapshot.turnId)
            _gameUiState.update {
                it.copy(
                    hasVotedThisTurn = false,
                    selectedReaction = null,
                    likeCount = 0,
                    dislikeCount = 0,
                    reactionErrorMessage = null
                )
            }
            _canvasState.update { it.copy(strokes = emptyList(), canUndo = false) }
            undoHistory.clear()
            preEraserGestureSnapshot = null
            authoritativeSecretWord = ""
        } else {
            // Only update deadline if not frozen due to allGuessed
            if (snapshot.phase != GamePhase.DRAWING || !allGuessed) {
                currentTurnDeadline = snapshot.turnDeadline
            }
        }

        val drawerPlayer = _gameUiState.value.players.firstOrNull { it.id == snapshot.drawerId }
        val masked = MaskedWord(
            hint = snapshot.maskedWord.ifEmpty { "_ ".repeat(max(1, snapshot.wordLength)).trim() },
            length = snapshot.wordLength
        )

        val hasGuessed = snapshot.correctGuessers.containsKey(localPlayerId)
        val isRoundOver = snapshot.phase == GamePhase.ROUND_RESULTS || snapshot.revealedWord != null
        val isGameOver = snapshot.phase == GamePhase.MATCH_RESULTS

        val displayed = when {
            isGameOver || isRoundOver -> snapshot.revealedWord ?: authoritativeSecretWord
            isDrawer -> _gameUiState.value.drawerSecretWord ?: authoritativeSecretWord
            hasGuessed -> authoritativeSecretWord
            snapshot.phase == GamePhase.DRAWING -> masked.hint
            snapshot.phase == GamePhase.COUNTDOWN -> "GET READY!"
            else -> "CHOOSING WORD..."
        }

        _gameUiState.update { current ->
            current.copy(
                phase = snapshot.phase,
                isLocalDrawer = isDrawer,
                currentRound = snapshot.round,
                totalRounds = snapshot.totalRounds,
                drawerPlayer = drawerPlayer,
                maskedWord = masked,
                displayedWord = displayed,
                hasGuessedCorrectly = hasGuessed,
                isRoundEnded = isRoundOver,
                isGameFinished = isGameOver,
                wordChoices = if (isDrawer) (if (isTurnChanged) emptyList() else current.wordChoices) else emptyList(),
                drawerSecretWord = if (isDrawer) (if (isTurnChanged) null else current.drawerSecretWord) else null
            )
        }

        if (isDrawer && snapshot.phase == GamePhase.SELECTING_WORD && _gameUiState.value.wordChoices.isEmpty()) {
            viewModelScope.launch {
                gameRepository?.ensureDrawerSecret(
                    roomId = roomId,
                    turnId = snapshot.turnId,
                    drawerId = localPlayerId,
                    difficulty = roomDifficulty
                )
            }
        }

        // Freeze drawing timer and advance turn immediately when all eligible guessers have guessed correctly
        if (snapshot.phase == GamePhase.DRAWING && allGuessed) {
            currentTurnDeadline = 0L
            revealWordOnce(roomId)
            val isHost = _gameUiState.value.players.firstOrNull { it.id == localPlayerId }?.isHost == true
            if (isHost && !isAdvancingTurn.get()) {
                if (isAdvancingTurn.compareAndSet(false, true)) {
                    viewModelScope.launch {
                        gameRepository?.endTurnAndAdvance(roomId, "ALL_GUESSED")
                    }
                }
            }
        }
    }

    private fun revealWordOnce(roomId: String) {
        if (_gameUiState.value.isLocalDrawer && lastRevealedTurnId != currentTurnId && authoritativeSecretWord.isNotBlank()) {
            lastRevealedTurnId = currentTurnId
            val localPlayer = _gameUiState.value.players.firstOrNull { it.id == localPlayerId }
                ?: Player(localPlayerId, "Drawer", AvatarId.FOX)
            val msg = ChatMessage(
                id = "reveal_${currentTurnId}_${System.currentTimeMillis()}",
                senderId = localPlayerId,
                senderName = localPlayer.name,
                text = "The word was: ${authoritativeSecretWord.uppercase()}",
                type = ChatMessageType.CORRECT_GUESS
            )
            viewModelScope.launch {
                gameRepository?.sendChatMessage(roomId, msg)
            }
        }
    }

    private fun observeStrokesForTurn(roomId: String, turnId: String, isDrawer: Boolean) {
        val gameRepo = gameRepository ?: return
        if (turnId.isBlank()) return
        strokesObserveJob?.cancel()
        strokesObserveJob = viewModelScope.launch {
            gameRepo.observeDrawingStrokes(roomId, turnId).collectLatest { remoteStrokes ->
                if (!_gameUiState.value.isLocalDrawer || _canvasState.value.strokes.isEmpty()) {
                    _canvasState.update { it.copy(strokes = remoteStrokes) }
                }
            }
        }
    }

    private fun observeReactionsForTurn(roomId: String, turnId: String) {
        val gameRepo = gameRepository ?: return
        if (turnId.isBlank()) return
        reactionsObserveJob?.cancel()
        reactionsObserveJob = viewModelScope.launch {
            gameRepo.observeReactions(roomId, turnId).collectLatest { reactionsMap ->
                val myReaction = reactionsMap[localPlayerId]
                val likes = reactionsMap.values.count { it == DrawingReaction.LIKE }
                val dislikes = reactionsMap.values.count { it == DrawingReaction.DISLIKE }
                _gameUiState.update { current ->
                    current.copy(
                        selectedReaction = myReaction ?: current.selectedReaction,
                        hasVotedThisTurn = current.hasVotedThisTurn || (myReaction != null),
                        likeCount = likes,
                        dislikeCount = dislikes
                    )
                }
            }
        }
    }

    fun submitReaction(reaction: DrawingReaction) {
        val current = _gameUiState.value
        if (current.isLocalDrawer) return
        if (current.phase != GamePhase.DRAWING) return
        if (current.isRoundEnded || current.remainingTimeSeconds <= 0 || current.isGameFinished || current.isPlayerDeparted) return
        if (current.hasVotedThisTurn) return
        if (isSubmittingReaction) return

        val roomId = activeRoomId
        val targetTurnId = currentTurnId
        if (targetTurnId.isBlank()) return

        // 1. Immediate synchronous optimistic UI update: buttons disappear immediately
        isSubmittingReaction = true
        _gameUiState.update { state ->
            val likes = if (reaction == DrawingReaction.LIKE) state.likeCount + 1 else state.likeCount
            val dislikes = if (reaction == DrawingReaction.DISLIKE) state.dislikeCount + 1 else state.dislikeCount
            state.copy(
                hasVotedThisTurn = true,
                selectedReaction = reaction,
                likeCount = likes,
                dislikeCount = dislikes,
                reactionErrorMessage = null
            )
        }

        if (roomId == null || gameRepository == null) {
            isSubmittingReaction = false
            return
        }

        // 2. Asynchronous write to Firebase
        viewModelScope.launch {
            try {
                val result = gameRepository.submitReaction(roomId, targetTurnId, localPlayerId, reaction)
                if (result is AppResult.Error) {
                    handleVoteFailure(targetTurnId, result.message ?: "Failed to submit reaction")
                } else if (result is AppResult.Success) {
                    try {
                        val localPlayer = _gameUiState.value.players.firstOrNull { it.id == localPlayerId }
                        val displayName = localPlayer?.name?.trim()?.ifEmpty { null }
                            ?: authRepository?.currentUser?.value?.displayName?.trim()?.ifEmpty { null }
                            ?: "Player"
                        val announcementText = if (reaction == DrawingReaction.LIKE) {
                            "🔥 $displayName liked the drawing!"
                        } else {
                            "👎 $displayName disliked the drawing!"
                        }
                        val announcementMsg = ChatMessage(
                            id = "reaction_${targetTurnId}_${localPlayerId}",
                            senderId = localPlayerId,
                            senderName = displayName,
                            text = announcementText,
                            timestamp = System.currentTimeMillis(),
                            type = ChatMessageType.CHAT
                        )
                        gameRepository.sendChatMessage(roomId, announcementMsg)
                    } catch (chatError: Exception) {
                        android.util.Log.w("GameViewModel", "Failed to send reaction chat announcement", chatError)
                    }
                }
            } catch (e: Exception) {
                handleVoteFailure(targetTurnId, e.message ?: "Network error submitting reaction")
            } finally {
                isSubmittingReaction = false
            }
        }
    }

    private fun handleVoteFailure(targetTurnId: String, errorMsg: String) {
        val current = _gameUiState.value
        // Restore buttons only if the player is still eligible and the turn is still active
        if (currentTurnId == targetTurnId &&
            current.phase == GamePhase.DRAWING &&
            !current.isRoundEnded &&
            current.remainingTimeSeconds > 0 &&
            !current.isGameFinished &&
            !current.isPlayerDeparted
        ) {
            _gameUiState.update { state ->
                val prev = state.selectedReaction
                val likes = if (prev == DrawingReaction.LIKE) max(0, state.likeCount - 1) else state.likeCount
                val dislikes = if (prev == DrawingReaction.DISLIKE) max(0, state.dislikeCount - 1) else state.dislikeCount
                state.copy(
                    hasVotedThisTurn = false,
                    selectedReaction = null,
                    likeCount = likes,
                    dislikeCount = dislikes,
                    reactionErrorMessage = "Reaction failed to send. Tap to retry."
                )
            }
        } else {
            // Turn already finished or advanced; avoid restoring buttons
            _gameUiState.update { state ->
                state.copy(reactionErrorMessage = null)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        strokesObserveJob?.cancel()
        reactionsObserveJob?.cancel()
    }

    private fun startDeadlineTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(250)
                val capturedRoomId = activeRoomId ?: continue
                val capturedTurnId = currentTurnId
                val capturedPhase = _gameUiState.value.phase
                val deadline = currentTurnDeadline

                if (deadline <= 0L || capturedTurnId.isBlank()) {
                    continue
                }

                val now = System.currentTimeMillis()
                val remaining = max(0, ((deadline - now) / 1000).toInt())

                if (capturedTurnId == currentTurnId && capturedPhase == _gameUiState.value.phase) {
                    _gameUiState.update { it.copy(remainingTimeSeconds = remaining) }
                }

                if (now >= deadline) {
                    if (capturedRoomId != activeRoomId || capturedTurnId != currentTurnId || capturedPhase != _gameUiState.value.phase || deadline != currentTurnDeadline) {
                        continue
                    }

                    // Invalidate deadline immediately
                    currentTurnDeadline = 0L

                    val isHost = _gameUiState.value.players.firstOrNull { it.id == localPlayerId }?.isHost == true

                    when (capturedPhase) {
                        GamePhase.DRAWING -> {
                            revealWordOnce(capturedRoomId)
                            if (isHost && isAdvancingTurn.compareAndSet(false, true)) {
                                gameRepository?.endTurnAndAdvance(capturedRoomId, "TIME_EXPIRED")
                            }
                        }
                        GamePhase.SELECTING_WORD -> {
                            if (_gameUiState.value.isLocalDrawer) {
                                val fallback = _gameUiState.value.wordChoices.firstOrNull()?.word ?: "APPLE"
                                onWordSelected(fallback)
                            } else if (isHost && isAdvancingTurn.compareAndSet(false, true)) {
                                gameRepository?.endTurnAndAdvance(capturedRoomId, "SELECTION_EXPIRED")
                            }
                        }
                        GamePhase.COUNTDOWN -> {
                            if (isHost && !hasAdvancedToWordSelection.get()) {
                                hasAdvancedToWordSelection.set(true)
                                gameRepository?.startWordSelection(capturedRoomId, 10)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_gameUiState.value.remainingTimeSeconds > 0) {
                delay(1000)
                _gameUiState.update { current ->
                    if (current.isGameFinished) {
                        current
                    } else {
                        val newTime = current.remainingTimeSeconds - 1
                        if (newTime == 0) {
                            current.copy(
                                remainingTimeSeconds = 0,
                                isRoundEnded = true,
                                displayedWord = authoritativeSecretWord,
                                isGameFinished = true
                            )
                        } else {
                            current.copy(remainingTimeSeconds = newTime)
                        }
                    }
                }
            }
        }
    }

    // --- DEPARTURE HANDLING ---

    fun onRequestLeaveGame() {
        if (_gameUiState.value.isGameFinished || _gameUiState.value.isPlayerDeparted) {
            return
        }
        _gameUiState.update { it.copy(showLeaveConfirmationDialog = true) }
    }

    fun onDismissLeaveConfirmation() {
        _gameUiState.update { it.copy(showLeaveConfirmationDialog = false) }
    }

    fun onConfirmLeaveGame() {
        if (isDepartureProcessed) return
        isDepartureProcessed = true

        timerJob?.cancel()
        _gameUiState.update { current ->
            current.copy(
                showLeaveConfirmationDialog = false,
                isPlayerDeparted = true,
                remainingTimeSeconds = 0
            )
        }

        val roomId = activeRoomId
        if (roomId != null && gameRepository != null && roomRepository != null) {
            viewModelScope.launch {
                gameRepository.handlePlayerDeparture(roomId, localPlayerId)
                roomRepository.leaveRoom(roomId, localPlayerId)
            }
        }
    }

    fun handlePlayerDeparture(departingPlayerId: String) {
        val currentState = _gameUiState.value
        if (currentState.isGameFinished) return

        val activePlayers = currentState.players.filter { it.isConnected }
        val departingPlayer = activePlayers.firstOrNull { it.id == departingPlayerId } ?: return

        // 1. Room with exactly two active players: remaining player wins by default!
        if (activePlayers.size == 2) {
            timerJob?.cancel()
            val remainingPlayer = activePlayers.first { it.id != departingPlayerId }
            val winMessage = "Your opponent left the game. You win by default!"

            val systemMsg = ChatMessage(
                id = "sys_leave_${System.currentTimeMillis()}",
                senderId = "system",
                senderName = "System",
                text = "${departingPlayer.name} left the game. ${remainingPlayer.name} wins by default!",
                type = ChatMessageType.SYSTEM_INFO
            )

            val updatedPlayers = currentState.players.filter { it.id != departingPlayerId }

            _gameUiState.update { current ->
                current.copy(
                    players = updatedPlayers,
                    isGameFinished = true,
                    isRoundEnded = true,
                    remainingTimeSeconds = 0,
                    defaultWinReason = winMessage,
                    displayedWord = authoritativeSecretWord,
                    chatMessages = current.chatMessages + systemMsg
                )
            }
            return
        }

        // 2. Room with more than two active players: continue game and post notice
        val remainingPlayers = activePlayers.filter { it.id != departingPlayerId }
        val systemNotice = ChatMessage(
            id = "sys_leave_${System.currentTimeMillis()}",
            senderId = "system",
            senderName = "System",
            text = "'${departingPlayer.name}' left the game.",
            type = ChatMessageType.SYSTEM_INFO
        )

        val wasDrawer = currentState.drawerPlayer?.id == departingPlayerId

        if (wasDrawer) {
            _canvasState.update { it.copy(strokes = emptyList(), canUndo = false) }
            val nextDrawer = remainingPlayers.first()
            val newIsLocalDrawer = nextDrawer.id == localPlayerId

            authoritativeSecretWord = "GUITAR"
            val masked = MaskedWord.fromWord(authoritativeSecretWord)

            _gameUiState.update { current ->
                current.copy(
                    players = remainingPlayers,
                    drawerPlayer = nextDrawer,
                    isLocalDrawer = newIsLocalDrawer,
                    phase = GamePhase.SELECTING_WORD,
                    remainingTimeSeconds = 60,
                    drawerSecretWord = if (newIsLocalDrawer) authoritativeSecretWord else null,
                    displayedWord = if (newIsLocalDrawer) authoritativeSecretWord else masked.hint,
                    maskedWord = masked,
                    chatMessages = current.chatMessages + systemNotice,
                    wordChoices = if (newIsLocalDrawer) {
                        listOf(
                            WordChoice("1", "GUITAR", WordDifficulty.EASY),
                            WordChoice("2", "SUNFLOWER", WordDifficulty.MEDIUM),
                            WordChoice("3", "TELESCOPE", WordDifficulty.HARD)
                        )
                    } else emptyList()
                )
            }
            startCountdownTimer()
        } else {
            _gameUiState.update { current ->
                current.copy(
                    players = remainingPlayers,
                    chatMessages = current.chatMessages + systemNotice
                )
            }
        }
    }

    // --- DRAWING & CANVAS ---

    fun onStrokeCompleted(stroke: DrawingStroke) {
        val currentStrokes = _canvasState.value.strokes
        undoHistory.add(currentStrokes)
        val updated = currentStrokes + stroke
        _canvasState.update { it.copy(strokes = updated, canUndo = true) }

        val roomId = activeRoomId
        if (roomId != null && gameRepository != null) {
            viewModelScope.launch {
                gameRepository.sendStroke(roomId, currentTurnId, stroke, strokeSequence.getAndIncrement())
            }
        }
    }

    fun onEraserDragSegment(p1x: Float, p1y: Float, p2x: Float, p2y: Float, radius: Float) {
        val currentStrokes = _canvasState.value.strokes
        if (preEraserGestureSnapshot == null) {
            preEraserGestureSnapshot = currentStrokes
        }

        val erasedIds = StrokeGeometry.findIntersectedStrokeIds(
            existingStrokes = currentStrokes,
            p1x = p1x,
            p1y = p1y,
            p2x = p2x,
            p2y = p2y,
            eraserRadius = radius
        )

        if (erasedIds.isNotEmpty()) {
            val surviving = currentStrokes.filterNot { it.id in erasedIds }
            _canvasState.update { it.copy(strokes = surviving, canUndo = true) }

            val roomId = activeRoomId
            if (roomId != null && gameRepository != null) {
                viewModelScope.launch {
                    gameRepository.deleteStrokes(roomId, currentTurnId, erasedIds)
                }
            }
        }
    }

    fun onEraserGestureCompleted() {
        preEraserGestureSnapshot?.let { snapshot ->
            if (snapshot != _canvasState.value.strokes) {
                undoHistory.add(snapshot)
            }
        }
        preEraserGestureSnapshot = null
        _canvasState.update { it.copy(canUndo = undoHistory.isNotEmpty() || it.strokes.isNotEmpty()) }
    }

    fun onUndoClicked() {
        val currentStrokes = _canvasState.value.strokes
        if (undoHistory.isNotEmpty()) {
            val previousStrokes = undoHistory.removeAt(undoHistory.lastIndex)
            val removedStrokes = currentStrokes.filterNot { prev -> previousStrokes.any { it.id == prev.id } }
            _canvasState.update {
                it.copy(
                    strokes = previousStrokes,
                    canUndo = undoHistory.isNotEmpty() || previousStrokes.isNotEmpty()
                )
            }
            val roomId = activeRoomId
            if (roomId != null && gameRepository != null && removedStrokes.isNotEmpty()) {
                viewModelScope.launch {
                    gameRepository.deleteStrokes(roomId, currentTurnId, removedStrokes.map { it.id })
                }
            }
        } else if (currentStrokes.isNotEmpty()) {
            val lastStroke = currentStrokes.last()
            val previousStrokes = currentStrokes.dropLast(1)
            _canvasState.update {
                it.copy(
                    strokes = previousStrokes,
                    canUndo = previousStrokes.isNotEmpty()
                )
            }
            val roomId = activeRoomId
            if (roomId != null && gameRepository != null) {
                viewModelScope.launch {
                    gameRepository.deleteStrokes(roomId, currentTurnId, listOf(lastStroke.id))
                }
            }
        }
    }

    fun onClearClicked() {
        if (_canvasState.value.strokes.isNotEmpty()) {
            undoHistory.add(_canvasState.value.strokes)
        }
        _canvasState.update { current ->
            current.copy(strokes = emptyList(), canUndo = undoHistory.isNotEmpty())
        }
        val roomId = activeRoomId
        if (roomId != null && gameRepository != null) {
            viewModelScope.launch {
                gameRepository.clearDrawing(roomId, currentTurnId)
            }
        }
    }

    fun onToolSelected(tool: DrawingTool) {
        _canvasState.update { it.copy(currentTool = tool) }
    }

    fun onColorSelected(color: Color) {
        _canvasState.update { it.copy(currentColor = color) }
    }

    fun onStrokeWidthSelected(width: Float) {
        _canvasState.update { it.copy(currentStrokeWidth = width) }
    }

    fun onWordSelected(word: String) {
        authoritativeSecretWord = word
        val masked = MaskedWord.fromWord(word)
        val drawTime = _gameUiState.value.drawTimeSeconds

        _gameUiState.update { current ->
            current.copy(
                phase = GamePhase.DRAWING,
                drawerSecretWord = if (current.isLocalDrawer) word else null,
                displayedWord = if (current.isLocalDrawer) word else masked.hint,
                maskedWord = masked,
                remainingTimeSeconds = drawTime
            )
        }

        val roomId = activeRoomId
        if (roomId != null && gameRepository != null) {
            viewModelScope.launch {
                gameRepository.selectWord(
                    roomId = roomId,
                    drawerId = localPlayerId,
                    selectedWord = word,
                    difficulty = roomDifficulty,
                    drawTimeSeconds = drawTime
                )
            }
        }
    }

    fun onChatInputChanged(text: String) {
        _gameUiState.update { it.copy(chatInput = text) }
    }

    fun onSendGuess() {
        val guess = _gameUiState.value.chatInput.trim()
        if (guess.isEmpty()) return

        val roomId = activeRoomId
        if (roomId != null && gameRepository != null) {
            val localPlayer = _gameUiState.value.players.firstOrNull { it.id == localPlayerId }
                ?: Player(localPlayerId, "You", AvatarId.FOX)
            viewModelScope.launch {
                _gameUiState.update { it.copy(chatInput = "") }
                val result = gameRepository.submitGuess(roomId, localPlayer, guess)
                if (result is AppResult.Success && result.data.isCorrect) {
                    authoritativeSecretWord = guess.uppercase()
                    _gameUiState.update { current ->
                        current.copy(
                            hasGuessedCorrectly = true,
                            displayedWord = guess.uppercase()
                        )
                    }
                }
            }
            return
        }

        // Offline / test fallback
        val isCorrect = guess.equals(authoritativeSecretWord, ignoreCase = true)
        val msg = if (isCorrect) {
            ChatMessage(
                id = "chat_${System.currentTimeMillis()}",
                senderId = localPlayerId,
                senderName = "You",
                text = "guessed the word!",
                type = ChatMessageType.CORRECT_GUESS
            )
        } else {
            ChatMessage(
                id = "chat_${System.currentTimeMillis()}",
                senderId = localPlayerId,
                senderName = "You",
                text = guess,
                type = ChatMessageType.CHAT
            )
        }

        _gameUiState.update { current ->
            current.copy(
                chatMessages = current.chatMessages + msg,
                chatInput = "",
                hasGuessedCorrectly = current.hasGuessedCorrectly || isCorrect
            )
        }
    }

    fun toggleRoleForPreview() {
        _gameUiState.update { current ->
            val newIsDrawer = !current.isLocalDrawer
            val masked = MaskedWord.fromWord(authoritativeSecretWord)
            current.copy(
                isLocalDrawer = newIsDrawer,
                drawerSecretWord = if (newIsDrawer) authoritativeSecretWord else null,
                displayedWord = if (newIsDrawer || current.isRoundEnded) authoritativeSecretWord else masked.hint,
                wordChoices = if (newIsDrawer) {
                    listOf(
                        WordChoice("1", "ELEPHANT", WordDifficulty.EASY),
                        WordChoice("2", "GUITAR", WordDifficulty.MEDIUM),
                        WordChoice("3", "SPACESHIP", WordDifficulty.HARD)
                    )
                } else emptyList()
            )
        }
    }

    fun finishGameNow() {
        _gameUiState.update {
            it.copy(
                remainingTimeSeconds = 0,
                isRoundEnded = true,
                displayedWord = authoritativeSecretWord,
                isGameFinished = true
            )
        }
    }
}
