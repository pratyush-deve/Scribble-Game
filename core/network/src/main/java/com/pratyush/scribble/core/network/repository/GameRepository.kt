package com.pratyush.scribble.core.network.repository

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.ChatMessage
import com.pratyush.scribble.core.model.DrawingReaction
import com.pratyush.scribble.core.model.DrawingStroke
import com.pratyush.scribble.core.model.DrawerSecretState
import com.pratyush.scribble.core.model.GameStateSnapshot
import com.pratyush.scribble.core.model.GuessResult
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.WordDifficulty
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observeGameState(roomId: String): Flow<GameStateSnapshot?>
    fun observeDrawingStrokes(roomId: String, turnId: String): Flow<List<DrawingStroke>>
    fun observeChatMessages(roomId: String): Flow<List<ChatMessage>>
    fun observeDrawerSecret(roomId: String, playerId: String): Flow<DrawerSecretState?>
    fun observeReactions(roomId: String, turnId: String): Flow<Map<String, DrawingReaction>> = kotlinx.coroutines.flow.emptyFlow()
    suspend fun submitReaction(roomId: String, turnId: String, playerId: String, reaction: DrawingReaction): AppResult<Unit> = AppResult.Success(Unit)

    suspend fun sendStroke(roomId: String, turnId: String, stroke: DrawingStroke, seq: Long): AppResult<Unit>
    suspend fun deleteStrokes(roomId: String, turnId: String, strokeIds: List<String>): AppResult<Unit>
    suspend fun clearDrawing(roomId: String, turnId: String): AppResult<Unit>

    suspend fun sendChatMessage(roomId: String, message: ChatMessage): AppResult<Unit>
    suspend fun submitGuess(roomId: String, player: Player, guessText: String): AppResult<GuessResult>

    suspend fun initializeGame(
        roomId: String,
        players: List<Player>,
        totalRounds: Int,
        drawTimeSeconds: Int,
        difficulty: WordDifficulty
    ): AppResult<Unit>

    suspend fun selectWord(
        roomId: String,
        drawerId: String,
        selectedWord: String,
        difficulty: WordDifficulty,
        drawTimeSeconds: Int
    ): AppResult<Unit>

    suspend fun startWordSelection(
        roomId: String,
        drawTimeSeconds: Int = 10
    ): AppResult<Unit>

    suspend fun ensureDrawerSecret(
        roomId: String,
        turnId: String,
        drawerId: String,
        difficulty: WordDifficulty
    ): AppResult<Unit>

    suspend fun endTurnAndAdvance(roomId: String, reason: String = "TIME_EXPIRED"): AppResult<Unit>
    suspend fun handlePlayerDeparture(roomId: String, departingPlayerId: String): AppResult<Unit>
    suspend fun cleanupGame(roomId: String): AppResult<Unit>
    suspend fun votePlayAgain(roomId: String, player: Player, vote: Boolean): AppResult<Unit>
    suspend fun restartGame(
        roomId: String,
        players: List<Player>,
        totalRounds: Int,
        drawTimeSeconds: Int,
        difficulty: WordDifficulty
    ): AppResult<Unit>

    fun observeReplayVoting(roomId: String): Flow<com.pratyush.scribble.core.model.ReplayVotingState?>
    suspend fun submitPlayAgainVote(roomId: String, vote: Boolean): AppResult<Unit>
    suspend fun finalizeReplaySession(roomId: String): AppResult<Unit>
    suspend fun finalizeMatch(roomId: String): AppResult<Unit> = AppResult.Success(Unit)
}
