package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    val avatarId: AvatarId,
    val isHost: Boolean = false,
    val score: Int = 0,
    val hasGuessedWord: Boolean = false,
    val isConnected: Boolean = true,
    val isDeparted: Boolean = false
)
