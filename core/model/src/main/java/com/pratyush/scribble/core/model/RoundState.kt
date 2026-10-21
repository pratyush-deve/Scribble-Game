package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class RoundState(
    val currentRound: Int,
    val totalRounds: Int,
    val drawer: Player,
    val word: String? = null,
    val maskedWord: MaskedWord? = null,
    val remainingTimeSeconds: Int = 60,
    val totalTimeSeconds: Int = 60,
    val wordChoices: List<WordChoice> = emptyList(),
    val phase: GamePhase = GamePhase.WAITING_FOR_PLAYERS,
    val reactions: Map<String, DrawingReaction> = emptyMap()
)
