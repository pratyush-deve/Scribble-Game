package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class RoomSettings(
    val maxPlayers: Int = 8,
    val rounds: Int = 3,
    val drawTimeSeconds: Int = 60,
    val wordDifficulty: WordDifficulty = WordDifficulty.MEDIUM
) {
    init {
        require(maxPlayers in 2..8) { "Players must be between 2 and 8" }
        require(rounds in 2..6) { "Rounds must be between 2 and 6" }
        require(drawTimeSeconds in 30..120) { "Draw time must be between 30 and 120 seconds" }
    }
}
