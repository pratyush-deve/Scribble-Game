package com.pratyush.scribble.feature.lobby.presentation.state

import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.Room
import com.pratyush.scribble.core.model.RoomSettings

data class LobbyUiState(
    val initialRoomCode: String = "",
    val room: Room? = null,
    val localPlayerId: String = "local_player_1",
    val isHost: Boolean = true,
    val isLoading: Boolean = false,
    val isRoomClosedByHost: Boolean = false,
    val errorMessage: String? = null,
    val isGameStarted: Boolean = false
) {
    val displayRoomCode: String get() = room?.code?.value ?: initialRoomCode
    val canStartGame: Boolean get() = isHost && (room?.canStart == true)
}

