package com.pratyush.scribble.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.network.repository.ProfileRepository
import com.pratyush.scribble.core.network.repository.RoomRepository
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository
import com.pratyush.scribble.feature.profile.presentation.state.ProfileUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository? = null,
    private val profileRepository: ProfileRepository? = null,
    private val roomRepository: RoomRepository? = null,
    private val activeRoomId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var profileJob: Job? = null

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val auth = authRepository
        if (auth == null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            auth.currentUser.collect { user ->
                if (user != null) {
                    _uiState.update { current ->
                        current.copy(
                            userProfile = user,
                            selectedAvatar = if (current.editedDisplayName.isEmpty()) user.avatarId else current.selectedAvatar,
                            editedDisplayName = if (current.editedDisplayName.isEmpty()) user.displayName else current.editedDisplayName,
                            isLoading = false
                        )
                    }

                    // Observe Firestore profile directly if available
                    if (profileRepository != null && profileJob == null) {
                        profileJob = viewModelScope.launch {
                            profileRepository.observeProfile(user.id).collect { firestoreProfile ->
                                if (firestoreProfile != null) {
                                    _uiState.update { current ->
                                        current.copy(
                                            userProfile = firestoreProfile,
                                            isLoading = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onAvatarSelected(avatar: AvatarId) {
        _uiState.update { it.copy(selectedAvatar = avatar) }
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(editedDisplayName = name, errorMessage = null) }
    }

    fun onSaveProfile(onSuccess: (() -> Unit)? = null) {
        val state = _uiState.value
        val trimmedName = state.editedDisplayName.trim()

        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Display name cannot be blank") }
            return
        }
        if (trimmedName.length > 25) {
            _uiState.update { it.copy(errorMessage = "Display name must be 25 characters or less") }
            return
        }

        val uid = state.userProfile.id
        if (uid.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Not signed in") }
            return
        }

        val avatar = state.selectedAvatar

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            var saveError: String? = null

            if (profileRepository != null) {
                when (val result = profileRepository.updateProfile(uid, trimmedName, avatar)) {
                    is AppResult.Success -> Unit
                    is AppResult.Error -> saveError = result.message
                    is AppResult.Loading -> Unit
                }
            }

            if (saveError != null) {
                _uiState.update { it.copy(isSaving = false, errorMessage = saveError) }
                return@launch
            }

            // If active in a room, propagate name and avatar into RTDB room player record
            val roomId = activeRoomId
            if (roomId != null && roomRepository != null) {
                roomRepository.updatePlayerProfile(roomId, uid, trimmedName, avatar)
            }

            _uiState.update { current ->
                current.copy(
                    userProfile = current.userProfile.copy(
                        displayName = trimmedName,
                        avatarId = avatar
                    ),
                    isSaving = false,
                    isSaved = true
                )
            }

            onSuccess?.invoke()
        }
    }

    fun toggleAvatarPicker(visible: Boolean) {
        _uiState.update { it.copy(isAvatarPickerVisible = visible) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
