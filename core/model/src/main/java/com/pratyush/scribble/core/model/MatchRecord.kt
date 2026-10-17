package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class MatchRecord(
    val matchId: String,
    val completedAt: Long = 0L,
    val playerUids: List<String> = emptyList(),
    val scores: Map<String, Int> = emptyMap(),
    val winnerUid: String? = null,
    val isDraw: Boolean = false,
    val rounds: Int = 3,
    val drawTimeSeconds: Int = 60
)
