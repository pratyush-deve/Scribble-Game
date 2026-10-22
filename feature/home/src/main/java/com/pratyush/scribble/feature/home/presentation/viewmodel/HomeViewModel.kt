package com.pratyush.scribble.feature.home.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.RoomCode
import com.pratyush.scribble.core.model.RoomSettings
import com.pratyush.scribble.core.model.UserProfile
import com.pratyush.scribble.core.network.repository.ProfileRepository
import com.pratyush.scribble.core.network.repository.RoomRepository
import com.pratyush.scribble.core.network.util.RoomCodeGenerator
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository
import com.pratyush.scribble.feature.home.presentation.state.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.pratyush.scribble.feature.home.presentation.state.JoinRoomState

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val roomRepository: RoomRepository,
    private val profileRepository: ProfileRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var profileObserveJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(userProfile = user) }
                profileObserveJob?.cancel()
                if (user != null && profileRepository != null) {
                    profileObserveJob = launch {
                        profileRepository.observeProfile(user.id).collect { updatedProfile ->
                            if (updatedProfile != null) {
                                _uiState.update { it.copy(userProfile = updatedProfile) }
                            }
                        }
                    }
                } else {
                    profileObserveJob = null
                }
            }
        }
    }

    fun onHostRoomClicked() {
        if (_uiState.value.userProfile?.isGuestLimitReached == true) {
            _uiState.update { it.copy(isGuestLimitDialogOpen = true, errorMessage = null) }
            return
        }
        _uiState.update { it.copy(isHostRoomDialogOpen = true, errorMessage = null) }
    }

    fun onJoinRoomClicked() {
        if (_uiState.value.userProfile?.isGuestLimitReached == true) {
            _uiState.update { it.copy(isGuestLimitDialogOpen = true, errorMessage = null) }
            return
        }
        _uiState.update {
            it.copy(
                isJoinRoomDialogOpen = true,
                roomCodeInput = "",
                joinRoomState = JoinRoomState.Idle,
                errorMessage = null
            )
        }
    }

    fun onHowToPlayClicked() {
        _uiState.update { it.copy(isHowToPlayDialogOpen = true) }
    }

    fun onDismissHowToPlayDialog() {
        _uiState.update { it.copy(isHowToPlayDialogOpen = false) }
    }

    fun onDismissGuestLimitDialog() {
        _uiState.update { it.copy(isGuestLimitDialogOpen = false, errorMessage = null) }
    }

    fun onDismissDialogs() {
        if (_uiState.value.isJoiningOrResolving || _uiState.value.isLinkingAccount) return
        _uiState.update {
            it.copy(
                isHostRoomDialogOpen = false,
                isJoinRoomDialogOpen = false,
                isHowToPlayDialogOpen = false,
                isGuestLimitDialogOpen = false,
                joinRoomState = JoinRoomState.Idle,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun onUpgradeToGoogleClicked(
        activityContext: Context? = null,
        onSuccess: () -> Unit = {}
    ) {
        if (_uiState.value.isLinkingAccount) return
        _uiState.update { it.copy(isLinkingAccount = true, errorMessage = null) }
        viewModelScope.launch {
            val result = if (activityContext != null) {
                authRepository.linkGuestWithGoogle(activityContext)
            } else {
                authRepository.linkGuestWithGoogle()
            }
            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLinkingAccount = false,
                            isGuestLimitDialogOpen = false,
                            userProfile = result.data,
                            errorMessage = null
                        )
                    }
                    onSuccess()
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLinkingAccount = false,
                            errorMessage = result.message
                        )
                    }
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun onRoomCodeChanged(code: String) {
        if (_uiState.value.isJoiningOrResolving) return
        val normalized = RoomCodeGenerator.normalize(code).take(8)
        _uiState.update {
            it.copy(
                roomCodeInput = normalized,
                joinRoomState = JoinRoomState.Idle,
                errorMessage = null
            )
        }
    }

    fun validateAndJoinRoom(onSuccess: (String) -> Unit) {
        if (_uiState.value.isJoiningOrResolving) return

        val rawCode = _uiState.value.roomCodeInput
        val normalized = RoomCodeGenerator.normalize(rawCode)
        if (!RoomCodeGenerator.isValid(normalized)) {
            _uiState.update {
                it.copy(
                    errorMessage = "Room code must be 4 to 8 alphanumeric characters",
                    joinRoomState = JoinRoomState.Idle
                )
            }
            return
        }

        val user = _uiState.value.userProfile
        if (user == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "You must be signed in to join a room",
                    joinRoomState = JoinRoomState.Idle
                )
            }
            return
        }

        if (user.isGuestLimitReached) {
            _uiState.update {
                it.copy(
                    isGuestLimitDialogOpen = true,
                    isJoinRoomDialogOpen = false,
                    isLoading = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    joinRoomState = JoinRoomState.ResolvingRoom,
                    errorMessage = null
                )
            }

            val player = Player(
                id = user.id,
                name = user.displayName,
                avatarId = user.avatarId,
                isHost = false
            )

            _uiState.update { it.copy(joinRoomState = JoinRoomState.Joining) }

            when (val result = roomRepository.joinRoom(normalized, player)) {
                is AppResult.Success -> {
                    val code = result.data.code.value
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            joinRoomState = JoinRoomState.Joined(code),
                            isJoinRoomDialogOpen = false
                        )
                    }
                    onSuccess(code)
                }
                is AppResult.Error -> {
                    val rawMsg = result.message
                    val state: JoinRoomState = when {
                        rawMsg.contains("ROOM_NOT_FOUND", ignoreCase = true) || rawMsg.contains("does not exist", ignoreCase = true) -> {
                            JoinRoomState.RoomNotFound
                        }
                        rawMsg.contains("ROOM_CLOSED", ignoreCase = true) || rawMsg.contains("in progress", ignoreCase = true) -> {
                            JoinRoomState.RoomClosed
                        }
                        rawMsg.contains("ROOM_FULL", ignoreCase = true) || rawMsg.contains("full", ignoreCase = true) -> {
                            JoinRoomState.RoomFull
                        }
                        else -> {
                            JoinRoomState.NetworkError(rawMsg)
                        }
                    }

                    val userMessage = when (state) {
                        is JoinRoomState.RoomNotFound -> "Room not found. Check the code and try again."
                        is JoinRoomState.RoomClosed -> "This room has ended or is already in progress."
                        is JoinRoomState.RoomFull -> "Room is full. Please try another room."
                        is JoinRoomState.NetworkError -> "Connection error. Please check your network and try again."
                        else -> rawMsg
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            joinRoomState = state,
                            errorMessage = userMessage
                        )
                    }
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun createHostRoom(onSuccess: (String) -> Unit) {
        if (_uiState.value.isLoading) return

        val user = _uiState.value.userProfile
        if (user == null) {
            _uiState.update { it.copy(errorMessage = "You must be signed in to create a room") }
            return
        }

        if (user.isGuestLimitReached) {
            _uiState.update {
                it.copy(
                    isGuestLimitDialogOpen = true,
                    isHostRoomDialogOpen = false,
                    isLoading = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val hostPlayer = Player(
                id = user.id,
                name = user.displayName,
                avatarId = user.avatarId,
                isHost = true
            )
            when (val result = roomRepository.createRoom(hostPlayer, RoomSettings())) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isHostRoomDialogOpen = false) }
                    onSuccess(result.data.code.value)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is AppResult.Loading -> Unit
            }
        }
    }
}
