package com.pratyush.scribble.feature.auth.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.pratyush.scribble.core.network.repository.ProfileRepository
import com.pratyush.scribble.feature.auth.data.repository.FirebaseAuthRepository
import com.pratyush.scribble.feature.auth.data.service.CredentialManagerGoogleIdTokenProvider
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository

/**
 * Factory providing production instances of [AuthRepository] from within `:feature:auth`.
 * This preserves clean architectural boundaries so `:app` doesn't need direct Firebase SDK dependencies.
 */
object AuthModule {
    @Volatile
    private var instance: AuthRepository? = null

    fun provideAuthRepository(
        context: Context,
        profileRepository: ProfileRepository? = null
    ): AuthRepository {
        return instance ?: synchronized(this) {
            instance ?: createAuthRepository(context.applicationContext, profileRepository).also {
                instance = it
            }
        }
    }

    private fun createAuthRepository(
        context: Context,
        profileRepository: ProfileRepository?
    ): AuthRepository {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val webClientId = if (resId != 0) {
            context.getString(resId)
        } else {
            ""
        }

        val googleIdTokenProvider = CredentialManagerGoogleIdTokenProvider(webClientId)
        return FirebaseAuthRepository(
            firebaseAuth = FirebaseAuth.getInstance(),
            googleIdTokenSource = {
                googleIdTokenProvider.getGoogleIdToken(context)
            },
            profileRepository = profileRepository
        )
    }

    fun setAuthRepositoryForTesting(repository: AuthRepository?) {
        instance = repository
    }
}
