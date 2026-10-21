package com.pratyush.scribble.core.network.repository

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.ChatMessage
import com.pratyush.scribble.core.model.ChatMessageType
import com.pratyush.scribble.core.model.DefaultWordBankDataSource
import com.pratyush.scribble.core.model.DrawingReaction
import com.pratyush.scribble.core.model.DrawingStroke
import com.pratyush.scribble.core.model.DrawerSecretState
import com.pratyush.scribble.core.model.GamePhase
import com.pratyush.scribble.core.model.GameScoringEngine
import com.pratyush.scribble.core.model.GameStateSnapshot
import com.pratyush.scribble.core.model.GuessResult
import com.pratyush.scribble.core.model.MaskedWord
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.WordBankDataSource
import com.pratyush.scribble.core.model.WordChoice
import com.pratyush.scribble.core.model.WordDifficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

class FakeGameRepository(
    private val wordBankDataSource: WordBankDataSource = DefaultWordBankDataSource()
) : GameRepository {

    private val gameStateFlows = ConcurrentHashMap<String, MutableStateFlow<GameStateSnapshot?>>()
    private val strokeFlows = ConcurrentHashMap<String, MutableStateFlow<List<DrawingStroke>>>()
    private val chatFlows = ConcurrentHashMap<String, MutableStateFlow<List<ChatMessage>>>()
    private val secretFlows = ConcurrentHashMap<String, MutableStateFlow<DrawerSecretState?>>()
    private val reactionFlows = ConcurrentHashMap<String, MutableStateFlow<Map<String, DrawingReaction>>>()
    private val playerScores = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()

    fun getPlayerScore(roomId: String, playerId: String): Int {
        return playerScores[roomId]?.get(playerId) ?: 0
    }

    override fun observeGameState(roomId: String): Flow<GameStateSnapshot?> {
        return gameStateFlows.getOrPut(roomId) { MutableStateFlow(null) }.asStateFlow()
    }

    override fun observeDrawingStrokes(roomId: String, turnId: String): Flow<List<DrawingStroke>> {
        val key = "${roomId}_$turnId"
        return strokeFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    override fun observeChatMessages(roomId: String): Flow<List<ChatMessage>> {
        return chatFlows.getOrPut(roomId) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    override fun observeDrawerSecret(roomId: String, playerId: String): Flow<DrawerSecretState?> {
        val key = "${roomId}_$playerId"
        return secretFlows.getOrPut(key) { MutableStateFlow(null) }.asStateFlow()
    }

    override fun observeReactions(roomId: String, turnId: String): Flow<Map<String, DrawingReaction>> {
        val key = "${roomId}_$turnId"
        return reactionFlows.getOrPut(key) { MutableStateFlow(emptyMap()) }.asStateFlow()
    }

    override suspend fun submitReaction(
        roomId: String,
        turnId: String,
        playerId: String,
        reaction: DrawingReaction
    ): AppResult<Unit> {
        val key = "${roomId}_$turnId"
        val flow = reactionFlows.getOrPut(key) { MutableStateFlow(emptyMap()) }
        val current = flow.value
        if (current.containsKey(playerId)) {
            return AppResult.Error("Player has already submitted a reaction for this turn")
        }
        flow.value = current + (playerId to reaction)
        return AppResult.Success(Unit)
    }

    override suspend fun sendStroke(
        roomId: String,
        turnId: String,
        stroke: DrawingStroke,
        seq: Long
    ): AppResult<Unit> {
        val key = "${roomId}_$turnId"
        val flow = strokeFlows.getOrPut(key) { MutableStateFlow(emptyList()) }
        flow.value = flow.value + stroke
        return AppResult.Success(Unit)
    }

    override suspend fun deleteStrokes(
        roomId: String,
        turnId: String,
        strokeIds: List<String>
    ): AppResult<Unit> {
        val key = "${roomId}_$turnId"
        val flow = strokeFlows.getOrPut(key) { MutableStateFlow(emptyList()) }
        flow.value = flow.value.filterNot { it.id in strokeIds }
        return AppResult.Success(Unit)
    }

    override suspend fun clearDrawing(roomId: String, turnId: String): AppResult<Unit> {
        val key = "${roomId}_$turnId"
        strokeFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.value = emptyList()
        val stateFlow = gameStateFlows[roomId]
        stateFlow?.value = stateFlow.value?.copy(clearSeq = System.currentTimeMillis())
        return AppResult.Success(Unit)
    }

    override suspend fun sendChatMessage(roomId: String, message: ChatMessage): AppResult<Unit> {
        val flow = chatFlows.getOrPut(roomId) { MutableStateFlow(emptyList()) }
        if (flow.value.none { it.id == message.id }) {
            flow.value = flow.value + message
        }
        return AppResult.Success(Unit)
    }

    override suspend fun submitGuess(
        roomId: String,
        player: Player,
        guessText: String
    ): AppResult<GuessResult> {
        val trimmed = guessText.trim()
        if (trimmed.isEmpty() || trimmed.length > 100) {
            return AppResult.Error("Invalid guess length")
        }

        val stateFlow = gameStateFlows[roomId] ?: return AppResult.Error("Game not found")
        val state = stateFlow.value ?: return AppResult.Error("Game not initialized")

        if (state.phase != GamePhase.DRAWING) {
            sendChatMessage(roomId, ChatMessage("msg_${System.currentTimeMillis()}", player.id, player.name, trimmed))
            return AppResult.Success(GuessResult.wrong(player.id, player.name, trimmed))
        }

        if (state.drawerId == player.id) {
            return AppResult.Error("Drawer cannot guess")
        }

        if (state.correctGuessers.containsKey(player.id)) {
            return AppResult.Success(GuessResult.alreadyGuessed(player.id, player.name))
        }

        val guessHash = DefaultWordBankDataSource.hashWord(trimmed)
        if (guessHash == state.wordHash) {
            val order = state.correctGuessers.size + 1
            val points = GameScoringEngine.calculateGuesserPoints(order)

            val roomScores = playerScores.getOrPut(roomId) { ConcurrentHashMap() }
            val currentScore = roomScores[player.id] ?: 0
            roomScores[player.id] = currentScore + points

            val updatedCorrect = state.correctGuessers + (player.id to System.currentTimeMillis())
            stateFlow.value = state.copy(correctGuessers = updatedCorrect)

            // Post system message for correct guess without revealing word
            val msg = ChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                senderId = player.id,
                senderName = player.name,
                text = "guessed the word!",
                type = ChatMessageType.CORRECT_GUESS
            )
            sendChatMessage(roomId, msg)

            return AppResult.Success(GuessResult.correct(player.id, player.name, points, order))
        } else {
            val msg = ChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                senderId = player.id,
                senderName = player.name,
                text = trimmed,
                type = ChatMessageType.CHAT
            )
            sendChatMessage(roomId, msg)
            return AppResult.Success(GuessResult.wrong(player.id, player.name, trimmed))
        }
    }

    override suspend fun initializeGame(
        roomId: String,
        players: List<Player>,
        totalRounds: Int,
        drawTimeSeconds: Int,
        difficulty: WordDifficulty
    ): AppResult<Unit> {
        val turnOrder = players.map { it.id }.sorted()
        val firstDrawerId = turnOrder.first()
        val turnId = "turn_1"
        val choices = wordBankDataSource.getChoices(difficulty, count = 3)

        val secretKey = "${roomId}_$firstDrawerId"
        secretFlows.getOrPut(secretKey) { MutableStateFlow(null) }.value = DrawerSecretState(
            turnId = turnId,
            drawerId = firstDrawerId,
            selectedWord = null,
            wordChoices = choices
        )

        gameStateFlows.getOrPut(roomId) { MutableStateFlow(null) }.value = GameStateSnapshot(
            phase = GamePhase.COUNTDOWN,
            round = 1,
            totalRounds = totalRounds,
            turnNumber = 1,
            turnId = turnId,
            drawerId = firstDrawerId,
            turnDeadline = System.currentTimeMillis() + 5000L,
            maskedWord = "",
            wordLength = 0,
            wordHash = "",
            revealedWord = null,
            correctGuessers = emptyMap(),
            turnOrder = turnOrder,
            scores = players.associate { it.id to it.score }
        )

        val roomScores = playerScores.getOrPut(roomId) { ConcurrentHashMap() }
        players.forEach { p -> roomScores[p.id] = p.score }

        return AppResult.Success(Unit)
    }

    override suspend fun startWordSelection(roomId: String, drawTimeSeconds: Int): AppResult<Unit> {
        val stateFlow = gameStateFlows[roomId] ?: return AppResult.Error("Game not found")
        val cur = stateFlow.value ?: return AppResult.Error("State not found")
        if (cur.phase == GamePhase.MATCH_RESULTS) return AppResult.Success(Unit)
        stateFlow.value = cur.copy(
            phase = GamePhase.SELECTING_WORD,
            turnDeadline = System.currentTimeMillis() + (drawTimeSeconds * 1000L)
        )
        return AppResult.Success(Unit)
    }

    override suspend fun selectWord(
        roomId: String,
        drawerId: String,
        selectedWord: String,
        difficulty: WordDifficulty,
        drawTimeSeconds: Int
    ): AppResult<Unit> {
        val upperWord = selectedWord.trim().uppercase()
        val masked = MaskedWord.fromWord(upperWord)
        val wordHash = DefaultWordBankDataSource.hashWord(upperWord)
        val deadline = System.currentTimeMillis() + (drawTimeSeconds * 1000L)

        val secretKey = "${roomId}_$drawerId"
        val curSecret = secretFlows[secretKey]?.value
        if (curSecret != null) {
            secretFlows[secretKey]?.value = curSecret.copy(selectedWord = upperWord)
        }

        val stateFlow = gameStateFlows[roomId] ?: return AppResult.Error("Game not found")
        val state = stateFlow.value ?: return AppResult.Error("State not found")
        if (state.phase == GamePhase.MATCH_RESULTS) return AppResult.Success(Unit)

        stateFlow.value = state.copy(
            phase = GamePhase.DRAWING,
            maskedWord = masked.hint,
            wordLength = upperWord.length,
            wordHash = wordHash,
            turnDeadline = deadline,
            revealedWord = null,
            correctGuessers = emptyMap()
        )

        return AppResult.Success(Unit)
    }

    override suspend fun ensureDrawerSecret(
        roomId: String,
        turnId: String,
        drawerId: String,
        difficulty: WordDifficulty
    ): AppResult<Unit> {
        val secretKey = "${roomId}_$drawerId"
        val flow = secretFlows.getOrPut(secretKey) { MutableStateFlow(null) }
        if (flow.value == null || flow.value?.turnId != turnId || flow.value?.wordChoices.isNullOrEmpty()) {
            val choices = wordBankDataSource.getChoices(difficulty, count = 3)
            flow.value = DrawerSecretState(
                turnId = turnId,
                drawerId = drawerId,
                selectedWord = null,
                wordChoices = choices
            )
        }
        return AppResult.Success(Unit)
    }

    override suspend fun endTurnAndAdvance(roomId: String, reason: String): AppResult<Unit> {
        val stateFlow = gameStateFlows[roomId] ?: return AppResult.Error("Game not found")
        val state = stateFlow.value ?: return AppResult.Error("State not found")
        if (state.phase == GamePhase.MATCH_RESULTS) return AppResult.Success(Unit)
        if (reason == "INSUFFICIENT_PLAYERS" || state.turnOrder.size < 2) {
            stateFlow.value = state.copy(phase = GamePhase.MATCH_RESULTS, revealedWord = "GAME OVER")
            return AppResult.Success(Unit)
        }

        val currentDrawerId = state.drawerId
        val correctCount = state.correctGuessers.size
        val drawerPoints = GameScoringEngine.calculateDrawerPoints(correctCount)
        val roomScores = playerScores.getOrPut(roomId) { ConcurrentHashMap() }

        if (drawerPoints > 0 && currentDrawerId.isNotBlank()) {
            val prevScore = roomScores[currentDrawerId] ?: 0
            roomScores[currentDrawerId] = prevScore + drawerPoints
        }

        val turnOrder = state.turnOrder
        val nextTurnNumber = state.turnNumber + 1
        val isRoundOver = (state.turnNumber % turnOrder.size) == 0
        val nextRound = if (isRoundOver) state.round + 1 else state.round

        if (nextRound > state.totalRounds) {
            stateFlow.value = state.copy(
                phase = GamePhase.MATCH_RESULTS,
                revealedWord = "GAME OVER",
                scores = roomScores.toMap()
            )
            return AppResult.Success(Unit)
        }

        val nextDrawerIndex = (nextTurnNumber - 1) % turnOrder.size
        val nextDrawerId = turnOrder[nextDrawerIndex]
        val nextTurnId = "turn_$nextTurnNumber"
        val choices = wordBankDataSource.getChoices(WordDifficulty.MEDIUM, count = 3)

        val nextSecretKey = "${roomId}_$nextDrawerId"
        secretFlows.getOrPut(nextSecretKey) { MutableStateFlow(null) }.value = DrawerSecretState(
            turnId = nextTurnId,
            drawerId = nextDrawerId,
            selectedWord = null,
            wordChoices = choices
        )

        val nextPhase = if (isRoundOver) GamePhase.COUNTDOWN else GamePhase.SELECTING_WORD
        val nextDeadline = System.currentTimeMillis() + 5_000L

        stateFlow.value = state.copy(
            phase = nextPhase,
            round = nextRound,
            turnNumber = nextTurnNumber,
            turnId = nextTurnId,
            drawerId = nextDrawerId,
            turnDeadline = nextDeadline,
            maskedWord = "",
            wordLength = 0,
            wordHash = "",
            revealedWord = null,
            correctGuessers = emptyMap(),
            scores = roomScores.toMap()
        )

        return AppResult.Success(Unit)
    }

    override suspend fun handlePlayerDeparture(roomId: String, departingPlayerId: String): AppResult<Unit> {
        val stateFlow = gameStateFlows[roomId] ?: return AppResult.Error("Game not found")
        val state = stateFlow.value ?: return AppResult.Error("State not found")

        val remainingOrder = state.turnOrder.filter { it != departingPlayerId }
        if (remainingOrder.size <= 1) {
            // Default victory for the remaining player
            stateFlow.value = state.copy(
                phase = GamePhase.MATCH_RESULTS,
                turnOrder = remainingOrder
            )
            sendChatMessage(
                roomId,
                ChatMessage(
                    id = "msg_leave_${System.currentTimeMillis()}",
                    senderId = "system",
                    senderName = "System",
                    text = "Opponent left the game. You win by default!",
                    type = ChatMessageType.SYSTEM_INFO
                )
            )
            return AppResult.Success(Unit)
        }

        if (state.drawerId == departingPlayerId) {
            // Drawer left: advance turn immediately
            stateFlow.value = state.copy(turnOrder = remainingOrder)
            return endTurnAndAdvance(roomId, "DRAWER_LEFT")
        } else {
            stateFlow.value = state.copy(turnOrder = remainingOrder)
            return AppResult.Success(Unit)
        }
    }

    override suspend fun cleanupGame(roomId: String): AppResult<Unit> {
        gameStateFlows.remove(roomId)
        chatFlows.remove(roomId)
        return AppResult.Success(Unit)
    }

    override suspend fun votePlayAgain(roomId: String, player: Player, vote: Boolean): AppResult<Unit> {
        val text = if (vote) "VOTE_PLAY_AGAIN" else "VOTE_PLAY_AGAIN_CANCEL"
        val message = ChatMessage(
            id = "vote_${player.id}_${System.currentTimeMillis()}",
            senderId = player.id,
            senderName = player.name,
            text = text,
            timestamp = System.currentTimeMillis(),
            type = ChatMessageType.CHAT
        )
        return sendChatMessage(roomId, message)
    }

    override suspend fun restartGame(
        roomId: String,
        players: List<Player>,
        totalRounds: Int,
        drawTimeSeconds: Int,
        difficulty: WordDifficulty
    ): AppResult<Unit> {
        return initializeGame(roomId, players, totalRounds, drawTimeSeconds, difficulty)
    }

    private val replayVotingFlows = ConcurrentHashMap<String, MutableStateFlow<com.pratyush.scribble.core.model.ReplayVotingState?>>()

    override fun observeReplayVoting(roomId: String): Flow<com.pratyush.scribble.core.model.ReplayVotingState?> {
        return replayVotingFlows.getOrPut(roomId) { MutableStateFlow(null) }.asStateFlow()
    }

    override suspend fun submitPlayAgainVote(roomId: String, vote: Boolean): AppResult<Unit> {
        val flow = replayVotingFlows.getOrPut(roomId) { MutableStateFlow(null) }
        val current = flow.value ?: com.pratyush.scribble.core.model.ReplayVotingState()
        val votes = current.votes.toMutableMap()
        votes["local"] = vote
        val count = votes.values.count { it }
        val updated = current.copy(votes = votes, voteCount = count)
        flow.value = updated
        return AppResult.Success(Unit)
    }

    override suspend fun finalizeReplaySession(roomId: String): AppResult<Unit> {
        replayVotingFlows.remove(roomId)
        return AppResult.Success(Unit)
    }
}
