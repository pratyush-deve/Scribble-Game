package com.pratyush.scribble.core.network.repository

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AccountType
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.MatchRecord
import com.pratyush.scribble.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun getProfile(uid: String): AppResult<UserProfile?>
    suspend fun getOrCreateProfile(
        uid: String,
        defaultDisplayName: String,
        defaultAvatarId: AvatarId = AvatarId.CAT,
        email: String? = null,
        accountType: AccountType = AccountType.PERMANENT
    ): AppResult<UserProfile>
    fun observeProfile(uid: String): Flow<UserProfile?>
    suspend fun updateDisplayName(uid: String, displayName: String): AppResult<Unit>
    suspend fun updateAvatar(uid: String, avatarId: AvatarId): AppResult<Unit>
    suspend fun updateProfile(uid: String, displayName: String, avatarId: AvatarId): AppResult<Unit>
    suspend fun updateLastSeen(uid: String): AppResult<Unit>
    suspend fun getMatchHistory(uid: String): AppResult<List<MatchRecord>>
    suspend fun upgradeGuestToPermanent(uid: String, email: String?): AppResult<Unit>
    suspend fun checkGuestMatchEligibility(uid: String): AppResult<Boolean>
}
