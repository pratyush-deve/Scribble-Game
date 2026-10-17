package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val soundEnabled: Boolean = true
)
