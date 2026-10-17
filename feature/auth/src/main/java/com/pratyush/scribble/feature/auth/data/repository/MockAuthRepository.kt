package com.pratyush.scribble.feature.auth.data.repository

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AccountType
import com.pratyush.scribble.core.model.AuthState
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.UserProfile
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockAuthRepository(
    initialUser: UserProfile? = null
) : AuthRepository {
    private val _currentUser = MutableStateFlow<UserProfile?>(initialUser)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(
        if (initialUser != null) AuthState.Authenticated(initialUser) else AuthState.Unauthenticated
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun signInWithGoogle(): AppResult<UserProfile> {
        delay(600) // Simulate auth flow
        val user = UserProfile(
            id = "google_user_101",
            displayName = "Alex Scribbler",
            email = "alex.scribbler@gmail.com",
            avatarId = AvatarId.FOX,
            totalGamesPlayed = 8,
            totalWins = 3,
            totalScore = 1800
        )
        _currentUser.value = user
        _authState.value = AuthState.Authenticated(user)
        return AppResult.Success(user)
    }

    override suspend fun signInAsGuest(displayName: String): AppResult<UserProfile> {
        delay(300)
        val name = displayName.ifBlank { "Guest_${(100..999).random()}" }
        val user = UserProfile(
            id = "guest_${System.currentTimeMillis()}",
            displayName = name,
            email = null,
            avatarId = AvatarId.CAT,
            accountType = AccountType.GUEST,
            guestMatchesCompleted = 0,
            guestAccessStatus = "ACTIVE"
        )
        _currentUser.value = user
        _authState.value = AuthState.Authenticated(user)
        return AppResult.Success(user)
    }

    override suspend fun signOut(): AppResult<Unit> {
        delay(200)
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
        return AppResult.Success(Unit)
    }

    override suspend fun getIdToken(forceRefresh: Boolean): AppResult<String> {
        delay(100)
        val user = _currentUser.value
        return if (user != null) {
            AppResult.Success("mock_firebase_id_token_${user.id}")
        } else {
            AppResult.Error("No user signed in")
        }
    }

    override suspend fun linkGuestWithGoogle(): AppResult<UserProfile> {
        delay(600)
        val current = _currentUser.value ?: return signInWithGoogle()
        val upgradedUser = current.copy(
            email = "alex.scribbler@gmail.com",
            accountType = AccountType.PERMANENT
        )
        _currentUser.value = upgradedUser
        _authState.value = AuthState.Authenticated(upgradedUser)
        return AppResult.Success(upgradedUser)
    }

    override suspend fun checkGuestEligibility(): AppResult<Boolean> {
        val user = _currentUser.value ?: return AppResult.Success(false)
        if (user.accountType == AccountType.PERMANENT) return AppResult.Success(true)
        return AppResult.Success(!user.isGuestLimitReached)
    }
}


