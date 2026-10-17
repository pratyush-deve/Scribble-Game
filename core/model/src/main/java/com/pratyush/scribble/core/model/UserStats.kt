package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserStats(
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val totalScore: Int = 0,
    val bestScore: Int = 0,
    val winStreak: Int = 0
) {
    val winRate: Float
        get() = if (gamesPlayed > 0) (wins.toFloat() / gamesPlayed) * 100f else 0f
}
