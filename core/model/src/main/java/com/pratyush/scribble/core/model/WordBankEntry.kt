package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class WordBankEntry(
    val id: String,
    val word: String,
    val difficulty: WordDifficulty,
    val category: String,
    val packVersion: Int = 1
) {
    fun toWordChoice(): WordChoice {
        return WordChoice(
            id = id,
            word = word.uppercase(),
            difficulty = difficulty
        )
    }
}
