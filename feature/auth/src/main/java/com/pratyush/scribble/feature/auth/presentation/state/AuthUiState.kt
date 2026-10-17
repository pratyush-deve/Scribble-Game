package com.pratyush.scribble.feature.auth.presentation.state

import com.pratyush.scribble.core.model.UserProfile

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authenticatedUser: UserProfile? = null
)
