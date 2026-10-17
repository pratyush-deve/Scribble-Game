package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class GameStateSnapshot(
    val phase: GamePhase = GamePhase.SELECTING_WORD,
    val round: Int = 1,
    val totalRounds: Int = 3,
    val turnNumber: Int = 1,
    val turnId: String = "turn_1",
    val drawerId: String = "",
    val turnDeadline: Long = 0L,
    val maskedWord: String = "",
    val wordLength: Int = 0,
    val wordHash: String = "",
    val revealedWord: String? = null,
    val correctGuessers: Map<String, Long> = emptyMap(),
    val turnOrder: List<String> = emptyList(),
    val clearSeq: Long = 0L,
    val scores: Map<String, Int> = emptyMap()
)

@Serializable
data class DrawerSecretState(
    val turnId: String,
    val drawerId: String,
    val selectedWord: String?,
    val wordChoices: List<WordChoice>
)
