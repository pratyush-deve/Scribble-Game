package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerScore(
    val player: Player,
    val rank: Int,
    val score: Int,
    val roundScore: Int = 0
)
