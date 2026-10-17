package com.pratyush.scribble.feature.profile.presentation.state

import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.UserProfile

data class ProfileUiState(
    val userProfile: UserProfile = UserProfile(
        id = "",
        displayName = "Player",
        email = null,
        avatarId = AvatarId.CAT
    ),
    val selectedAvatar: AvatarId = AvatarId.CAT,
    val editedDisplayName: String = "",
    val isAvatarPickerVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
)
