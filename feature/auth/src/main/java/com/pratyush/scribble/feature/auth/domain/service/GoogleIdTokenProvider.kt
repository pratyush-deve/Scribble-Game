package com.pratyush.scribble.feature.auth.domain.service

import android.content.Context
import com.pratyush.scribble.core.common.result.AppResult

/**
 * Contract for obtaining a Google ID Token using Android's Credential Manager.
 * Separating credential retrieval from Firebase Auth ensures pure, unit-testable repository logic.
 */
interface GoogleIdTokenProvider {
    suspend fun getGoogleIdToken(context: Context): AppResult<String>
}
