package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class WordChoice(
    val id: String,
    val word: String,
    val difficulty: WordDifficulty = WordDifficulty.MEDIUM
)
