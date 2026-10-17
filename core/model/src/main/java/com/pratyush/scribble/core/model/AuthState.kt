package com.pratyush.scribble.core.model

/**
 * Represents the authentication state of the application.
 */
sealed interface AuthState {
    /**
     * Firebase authentication state has not yet been resolved on launch.
     */
    data object Loading : AuthState

    /**
     * A valid signed-in user exists.
     */
    data class Authenticated(val user: UserProfile) : AuthState

    /**
     * Confirmed that no signed-in user exists.
     */
    data object Unauthenticated : AuthState
}
