package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: String,
    val code: RoomCode,
    val hostId: String,
    val players: List<Player> = emptyList(),
    val settings: RoomSettings = RoomSettings(),
    val phase: GamePhase = GamePhase.WAITING_FOR_PLAYERS,
    val currentRound: Int = 1,
    val currentDrawerId: String? = null
) {
    val playerCount: Int get() = players.size
    val canStart: Boolean get() = players.size >= 2
}
