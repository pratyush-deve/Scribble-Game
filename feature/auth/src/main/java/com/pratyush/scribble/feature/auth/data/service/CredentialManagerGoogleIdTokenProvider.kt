package com.pratyush.scribble.feature.auth.data.service

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.feature.auth.domain.service.GoogleIdTokenProvider

/**
 * Implementation of [GoogleIdTokenProvider] using AndroidX Credential Manager and Google Identity.
 */
class CredentialManagerGoogleIdTokenProvider(
    private val serverClientId: String
) : GoogleIdTokenProvider {

    override suspend fun getGoogleIdToken(context: Context): AppResult<String> {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )
            when (val credential = result.credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        try {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            AppResult.Success(googleIdTokenCredential.idToken)
                        } catch (e: Exception) {
                            AppResult.Error("Failed to parse Google ID token: ${e.localizedMessage ?: "Unknown error"}")
                        }
                    } else {
                        AppResult.Error("Unsupported credential type: ${credential.type}")
                    }
                }
                else -> AppResult.Error("Unexpected credential format received.")
            }
        } catch (e: GetCredentialCancellationException) {
            AppResult.Error("Sign-in cancelled")
        } catch (e: NoCredentialException) {
            AppResult.Error("No Google account found on device")
        } catch (e: GetCredentialException) {
            AppResult.Error(e.message ?: "Credential retrieval failed")
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "An unexpected error occurred during Google Sign-In")
        }
    }
}
