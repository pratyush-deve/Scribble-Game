package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val room: Room,
    val roundState: RoundState? = null,
    val currentStrokes: List<DrawingStroke> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val isLocalPlayerDrawer: Boolean = false,
    val localPlayer: Player? = null
)
