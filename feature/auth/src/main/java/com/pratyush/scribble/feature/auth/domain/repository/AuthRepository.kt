package com.pratyush.scribble.feature.auth.domain.repository

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AuthState
import com.pratyush.scribble.core.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
        get() = MutableStateFlow(
            currentUser.value?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated
        )
    val currentUser: StateFlow<UserProfile?>
    suspend fun signInWithGoogle(): AppResult<UserProfile>
    suspend fun signInAsGuest(displayName: String): AppResult<UserProfile>
    suspend fun signOut(): AppResult<Unit>
    suspend fun getIdToken(forceRefresh: Boolean = false): AppResult<String>
    suspend fun linkGuestWithGoogle(): AppResult<UserProfile> = signInWithGoogle()
    suspend fun checkGuestEligibility(): AppResult<Boolean> = AppResult.Success(true)
}

