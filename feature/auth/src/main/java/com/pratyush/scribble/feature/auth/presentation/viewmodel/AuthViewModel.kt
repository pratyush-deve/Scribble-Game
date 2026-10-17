package com.pratyush.scribble.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository
import com.pratyush.scribble.feature.auth.presentation.state.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(authenticatedUser = authRepository.currentUser.value)
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(authenticatedUser = user) }
            }
        }
    }

    fun onGoogleSignInClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.signInWithGoogle()) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, authenticatedUser = result.data) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun onGuestSignInClicked(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.signInAsGuest(name)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, authenticatedUser = result.data) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
