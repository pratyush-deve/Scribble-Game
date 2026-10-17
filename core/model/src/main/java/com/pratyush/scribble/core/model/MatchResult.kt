package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class MatchResult(
    val roomId: String,
    val rankings: List<PlayerScore>,
    val totalRounds: Int,
    val winner: Player,
    val winReason: String? = null
)
