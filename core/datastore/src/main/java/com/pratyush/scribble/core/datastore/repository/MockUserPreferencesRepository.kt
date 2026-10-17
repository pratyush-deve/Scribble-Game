package com.pratyush.scribble.core.datastore.repository

import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockUserPreferencesRepository : UserPreferencesRepository {
    private val _userProfile = MutableStateFlow(
        UserProfile(
            id = "mock_user_1",
            displayName = "Skribbler",
            email = "player@scribble.app",
            avatarId = AvatarId.CAT,
            totalGamesPlayed = 12,
            totalWins = 4,
            totalScore = 2450
        )
    )

    override val userProfile: Flow<UserProfile> = _userProfile.asStateFlow()

    override suspend fun updateDisplayName(name: String) {
        _userProfile.update { it.copy(displayName = name) }
    }

    override suspend fun updateAvatar(avatarId: AvatarId) {
        _userProfile.update { it.copy(avatarId = avatarId) }
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        _userProfile.value = profile
    }

    override suspend fun clearProfile() {
        _userProfile.value = UserProfile(id = "guest", displayName = "Guest")
    }
}
