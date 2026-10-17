package com.pratyush.scribble.core.datastore.repository

import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userProfile: Flow<UserProfile>
    suspend fun updateDisplayName(name: String)
    suspend fun updateAvatar(avatarId: AvatarId)
    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun clearProfile()
}
