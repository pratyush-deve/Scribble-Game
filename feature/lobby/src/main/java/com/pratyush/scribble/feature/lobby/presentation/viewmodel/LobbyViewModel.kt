package com.pratyush.scribble.feature.lobby.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.GamePhase
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.Room
import com.pratyush.scribble.core.model.RoomCode
import com.pratyush.scribble.core.model.RoomSettings
import com.pratyush.scribble.core.model.WordDifficulty
import com.pratyush.scribble.core.network.repository.ProfileRepository
import com.pratyush.scribble.core.network.repository.RoomRepository
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository
import com.pratyush.scribble.feature.lobby.presentation.state.LobbyUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LobbyViewModel(
    private val initialRoomCode: String,
    private val authRepository: AuthRepository,
    private val roomRepository: RoomRepository,
    private val profileRepository: ProfileRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LobbyUiState(
            initialRoomCode = initialRoomCode,
            localPlayerId = authRepository.currentUser.value?.id ?: "",
            isHost = false,
            isLoading = true
        )
    )
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    private var activeRoomId: String? = null
    private var isLeaving = false
    private var observeJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            val user = authRepository.currentUser.value
                ?: authRepository.currentUser.firstOrNull { it != null }

            val currentUserId = user?.id ?: ""
            _uiState.update { it.copy(localPlayerId = currentUserId) }

            // 1. Resolve roomCode to roomId
            val roomId = roomRepository.resolveRoomId(initialRoomCode)
            if (roomId != null) {
                startObserving(roomId, currentUserId)
            } else {
                // If resolving directly by code returned null, attempt joinRoom as fallback
                val player = Player(
                    id = currentUserId,
                    name = user?.displayName ?: "Player",
                    avatarId = user?.avatarId ?: AvatarId.CAT,
                    isHost = false
                )
                when (val joinResult = roomRepository.joinRoom(initialRoomCode, player)) {
                    is AppResult.Success -> {
                        startObserving(joinResult.data.id, currentUserId)
                    }
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Room does not exist or has expired"
                            )
                        }
                    }
                    is AppResult.Loading -> Unit
                }
            }
        }
    }

    private fun startObserving(roomId: String, currentUserId: String) {
        activeRoomId = roomId
        roomRepository.registerPresence(roomId, currentUserId)

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            roomRepository.observeRoom(roomId).collect { updatedRoom ->
                if (isLeaving) return@collect

                if (updatedRoom != null) {
                    val hostStatus = updatedRoom.hostId == currentUserId
                    val isStarted = updatedRoom.phase != GamePhase.WAITING_FOR_PLAYERS
                    _uiState.update {
                        it.copy(
                            room = updatedRoom,
                            isHost = hostStatus,
                            isLoading = false,
                            errorMessage = null,
                            isGameStarted = isStarted
                        )
                    }
                } else {
                    // Room was closed by host or deleted
                    _uiState.update {
                        it.copy(
                            room = null,
                            isLoading = false,
                            isRoomClosedByHost = true
                        )
                    }
                }
            }
        }
    }

    fun onRoundsChanged(rounds: Int) {
        val roomId = activeRoomId ?: return
        val currentSettings = _uiState.value.room?.settings ?: return
        viewModelScope.launch {
            roomRepository.updateSettings(
                roomId = roomId,
                requesterId = _uiState.value.localPlayerId,
                settings = currentSettings.copy(rounds = rounds)
            )
        }
    }

    fun onDrawTimeChanged(drawTimeSeconds: Int) {
        val roomId = activeRoomId ?: return
        val currentSettings = _uiState.value.room?.settings ?: return
        viewModelScope.launch {
            roomRepository.updateSettings(
                roomId = roomId,
                requesterId = _uiState.value.localPlayerId,
                settings = currentSettings.copy(drawTimeSeconds = drawTimeSeconds)
            )
        }
    }

    fun onDifficultyChanged(difficulty: WordDifficulty) {
        val roomId = activeRoomId ?: return
        val currentSettings = _uiState.value.room?.settings ?: return
        viewModelScope.launch {
            roomRepository.updateSettings(
                roomId = roomId,
                requesterId = _uiState.value.localPlayerId,
                settings = currentSettings.copy(wordDifficulty = difficulty)
            )
        }
    }

    fun onAvatarSelected(avatarId: AvatarId) {
        val roomId = activeRoomId ?: return
        val playerId = _uiState.value.localPlayerId
        viewModelScope.launch {
            roomRepository.updatePlayerAvatar(roomId, playerId, avatarId)
            if (profileRepository != null && playerId.isNotBlank()) {
                profileRepository.updateAvatar(playerId, avatarId)
            }
        }
    }

    fun onLeaveRoom(onComplete: () -> Unit) {
        if (isLeaving) return
        isLeaving = true
        observeJob?.cancel()
        val roomId = activeRoomId
        val playerId = _uiState.value.localPlayerId
        _uiState.update { it.copy(room = null, isRoomClosedByHost = false) }

        if (roomId != null && playerId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    roomRepository.leaveRoom(roomId, playerId)
                } finally {
                    onComplete()
                }
            }
        } else {
            onComplete()
        }
    }

    fun onStartGameClicked() {
        val roomId = activeRoomId ?: return
        val hostId = _uiState.value.localPlayerId
        viewModelScope.launch {
            when (val result = roomRepository.startGame(roomId, hostId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isGameStarted = true) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                is AppResult.Loading -> Unit
            }
        }
    }
}
