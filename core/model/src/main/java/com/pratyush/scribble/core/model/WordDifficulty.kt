package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class WordDifficulty(val displayName: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}
