package com.pratyush.scribble.feature.results.presentation.state

import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.PlayerScore

data class ResultsUiState(
    val roomCode: String = "",
    val rankings: List<PlayerScore> = emptyList(),
    val totalRounds: Int = 3,
    val localPlayerId: String = "",
    val winReason: String? = null,
    val isHost: Boolean = false,
    val hasVotedPlayAgain: Boolean = false,
    val playAgainVotes: Int = 0,
    val requiredMajority: Int = 1,
    val votedPlayers: List<Player> = emptyList(),
    val votingTimeRemainingSeconds: Int = 15,
    val isSessionClosed: Boolean = false,
    val isVoteFailed: Boolean = false,
    val failureMessage: String = "Not enough players to play again!",
    val failureSupportingText: String = "The session has ended. Taking you back home...",
    val shouldNavigateHome: Boolean = false,
    val isGameRestarted: Boolean = false
) {
    val isInsufficientPlayersDeparture: Boolean
        get() = winReason != null || (rankings.isNotEmpty() && rankings.count { !it.isDeparted && !it.player.isDeparted } < 2 && rankings.any { it.isDeparted || it.player.isDeparted })
}
