package com.pratyush.scribble.feature.home.presentation.state

import com.pratyush.scribble.core.model.UserProfile

sealed interface JoinRoomState {
    data object Idle : JoinRoomState
    data object ResolvingRoom : JoinRoomState
    data object Joining : JoinRoomState
    data class Joined(val roomCode: String) : JoinRoomState
    data object RoomNotFound : JoinRoomState
    data object RoomClosed : JoinRoomState
    data object RoomFull : JoinRoomState
    data class NetworkError(val message: String) : JoinRoomState
}

data class HomeUiState(
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val isJoinRoomDialogOpen: Boolean = false,
    val isHostRoomDialogOpen: Boolean = false,
    val isHowToPlayDialogOpen: Boolean = false,
    val isGuestLimitDialogOpen: Boolean = false,
    val isLinkingAccount: Boolean = false,
    val roomCodeInput: String = "",
    val joinRoomState: JoinRoomState = JoinRoomState.Idle,
    val errorMessage: String? = null
) {
    val isJoiningOrResolving: Boolean
        get() = joinRoomState is JoinRoomState.ResolvingRoom || joinRoomState is JoinRoomState.Joining
}
