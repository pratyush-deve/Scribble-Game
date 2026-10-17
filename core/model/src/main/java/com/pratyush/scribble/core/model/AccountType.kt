package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

/**
 * Distinguishes between guest accounts and permanent authenticated accounts.
 */
@Serializable
enum class AccountType {
    /**
     * Temporary identity backed by Firebase Anonymous Authentication.
     * Allowed a maximum of 2 completed matches before requiring Google Sign-In.
     */
    GUEST,

    /**
     * Permanent identity backed by Google Sign-In with unlimited match access.
     */
    PERMANENT
}
