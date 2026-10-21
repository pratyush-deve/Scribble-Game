package com.pratyush.scribble.feature.game.presentation.state

import com.pratyush.scribble.core.model.ChatMessage
import com.pratyush.scribble.core.model.DrawingReaction
import com.pratyush.scribble.core.model.GamePhase
import com.pratyush.scribble.core.model.MaskedWord
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.WordChoice

/**
 * Immutable UI State for the Game Screen.
 *
 * Security & Role Isolation:
 * - [displayedWord]: The word string safe to render in the player's UI.
 *   For the drawer, this contains the full secret word (e.g. "ELEPHANT").
 *   For guessing players, this contains ONLY the masked hint (e.g. "_ _ _ _ _ _ _ _"),
 *   or the revealed word only after the round has officially ended [isRoundEnded].
 * - [drawerSecretWord]: Accessible ONLY if [isLocalDrawer] is true; null otherwise.
 *   Guessers NEVER receive the secret word string in their state.
 *
 * Departure Handling:
 * - [showLeaveConfirmationDialog]: Controls visibility of the "Leave game?" modal.
 * - [isPlayerDeparted]: Signals the local player confirmed departure and must navigate away.
 * - [defaultWinReason]: When opponent leaves in a 2-player game, stores default victory reason.
 */
data class GameUiState(
    val roomCode: String = "",
    val phase: GamePhase = GamePhase.SELECTING_WORD,
    val isLocalDrawer: Boolean = true,
    val currentRound: Int = 1,
    val totalRounds: Int = 3,
    val remainingTimeSeconds: Int = 60,
    val drawTimeSeconds: Int = 60,
    val displayedWord: String = "",
    val drawerSecretWord: String? = null,
    val maskedWord: MaskedWord = MaskedWord.fromWord("ELEPHANT"),
    val wordChoices: List<WordChoice> = emptyList(),
    val drawerPlayer: Player? = null,
    val players: List<Player> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val chatInput: String = "",
    val hasGuessedCorrectly: Boolean = false,
    val isRoundEnded: Boolean = false,
    val isGameFinished: Boolean = false,
    val showLeaveConfirmationDialog: Boolean = false,
    val isPlayerDeparted: Boolean = false,
    val defaultWinReason: String? = null,
    val hasVotedThisTurn: Boolean = false,
    val selectedReaction: DrawingReaction? = null,
    val likeCount: Int = 0,
    val dislikeCount: Int = 0,
    val reactionErrorMessage: String? = null
)
