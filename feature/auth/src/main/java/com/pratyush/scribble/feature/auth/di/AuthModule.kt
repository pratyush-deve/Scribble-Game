package com.pratyush.scribble.feature.auth.di

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.network.repository.ProfileRepository
import com.pratyush.scribble.feature.auth.data.repository.FirebaseAuthRepository
import com.pratyush.scribble.feature.auth.data.service.CredentialManagerGoogleIdTokenProvider
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository

/**
 * Factory providing production instances of [AuthRepository] from within `:feature:auth`.
 * This preserves clean architectural boundaries so `:app` doesn't need direct Firebase SDK dependencies.
 */
object AuthModule {
    private const val TAG = "AuthModule"

    @Volatile
    private var instance: AuthRepository? = null

    @Volatile
    private var resolvedClientId: String = ""

    fun provideAuthRepository(
        context: Context,
        profileRepository: ProfileRepository? = null,
        webClientId: String? = null
    ): AuthRepository {
        val candidateClientId = webClientId?.trim().orEmpty()
        val existing = instance
        if (existing != null && (resolvedClientId.isNotEmpty() || candidateClientId.isEmpty())) {
            return existing
        }
        return synchronized(this) {
            val current = instance
            if (current != null && (resolvedClientId.isNotEmpty() || candidateClientId.isEmpty())) {
                current
            } else {
                val appContext = context.applicationContext ?: context
                val finalWebClientId = resolveWebClientId(
                    explicitWebClientId = candidateClientId,
                    resourceLookup = { lookupWebClientIdFromResources(appContext) }
                )
                createAuthRepository(
                    profileRepository = profileRepository,
                    webClientId = finalWebClientId
                ).also {
                    resolvedClientId = finalWebClientId
                    instance = it
                }
            }
        }
    }

    internal fun resolveWebClientId(
        explicitWebClientId: String?,
        resourceLookup: () -> String
    ): String {
        val trimmedExplicit = explicitWebClientId?.trim().orEmpty()
        if (trimmedExplicit.isNotEmpty()) {
            return trimmedExplicit
        }
        return resourceLookup().trim()
    }

    private fun lookupWebClientIdFromResources(appContext: Context): String {
        val resId = appContext.resources.getIdentifier(
            "default_web_client_id",
            "string",
            appContext.packageName
        )
        return if (resId != 0) {
            appContext.getString(resId).trim()
        } else {
            runCatching {
                Log.e(TAG, "default_web_client_id resource not found in package ${appContext.packageName}")
            }
            ""
        }
    }

    private fun createAuthRepository(
        profileRepository: ProfileRepository?,
        webClientId: String
    ): AuthRepository {
        val googleIdTokenProvider = CredentialManagerGoogleIdTokenProvider(webClientId)
        return FirebaseAuthRepository(
            firebaseAuth = FirebaseAuth.getInstance(),
            googleIdTokenSource = { activityContext ->
                if (activityContext == null) {
                    runCatching {
                        Log.e(TAG, "Google Sign-In requested without an Activity Context")
                    }
                    AppResult.Error("Google Sign-In requires an active screen context. Please try again.")
                } else {
                    googleIdTokenProvider.getGoogleIdToken(activityContext)
                }
            },
            profileRepository = profileRepository
        )
    }

    fun setAuthRepositoryForTesting(repository: AuthRepository?) {
        instance = repository
        resolvedClientId = if (repository != null) "test" else ""
    }
}

