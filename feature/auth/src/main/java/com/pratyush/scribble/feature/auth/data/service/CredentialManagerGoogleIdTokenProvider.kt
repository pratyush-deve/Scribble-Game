package com.pratyush.scribble.feature.auth.data.service

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.feature.auth.domain.service.GoogleIdTokenProvider
import kotlinx.coroutines.CancellationException

/**
 * Implementation of [GoogleIdTokenProvider] using AndroidX Credential Manager and Google Identity.
 */
class CredentialManagerGoogleIdTokenProvider(
    private val serverClientId: String
) : GoogleIdTokenProvider {

    override suspend fun getGoogleIdToken(context: Context): AppResult<String> {
        val trimmedClientId = serverClientId.trim()
        if (trimmedClientId.isEmpty()) {
            logE("Google Sign-In failed: serverClientId (default_web_client_id) is blank")
            return AppResult.Error("Google Sign-In is not configured properly (missing web client ID).")
        }

        val activity = context.findActivity()
        if (activity == null) {
            logE("Google Sign-In failed: Context (${context::class.java.simpleName}) is not an Activity Context")
            return AppResult.Error("Google Sign-In requires an active screen context. Please try again.")
        }

        if (activity.isFinishing || activity.isDestroyed) {
            logW("Google Sign-In aborted: Activity (${activity::class.java.simpleName}) is finishing or destroyed")
            return AppResult.Error("Google Sign-In could not be started because the screen is closing.")
        }

        return try {
            val credentialManager = CredentialManager.create(activity)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(trimmedClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            when (val credential = result.credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        if (idToken.isBlank()) {
                            logE("Google Sign-In failed: received blank Google ID token")
                            AppResult.Error("Received an empty Google Sign-In token. Please try again.")
                        } else {
                            AppResult.Success(idToken)
                        }
                    } else {
                        logW("Unsupported credential type returned: ${credential.type}")
                        AppResult.Error("Unsupported credential type selected.")
                    }
                }
                else -> {
                    logW("Unexpected credential class returned: ${credential::class.java.simpleName}")
                    AppResult.Error("Unexpected credential format received.")
                }
            }
        } catch (e: GetCredentialCancellationException) {
            logI("Google Sign-In cancelled by user (${e::class.java.simpleName})")
            AppResult.Error(ERROR_SIGN_IN_CANCELLED, e)
        } catch (e: NoCredentialException) {
            logW("No Google account available on device (${e::class.java.simpleName}: ${e.type})")
            AppResult.Error("No Google account found on device", e)
        } catch (e: GetCredentialProviderConfigurationException) {
            logE("Credential provider configuration error (${e::class.java.simpleName}: ${e.type})")
            AppResult.Error("Google Play Services credential provider is unavailable or needs an update.", e)
        } catch (e: GetCredentialUnsupportedException) {
            logE("Credential Manager unsupported (${e::class.java.simpleName}: ${e.type})")
            AppResult.Error("Google Sign-In is not supported on this device.", e)
        } catch (e: GetCredentialInterruptedException) {
            logW("Credential retrieval interrupted (${e::class.java.simpleName}: ${e.type})")
            AppResult.Error("Google Sign-In was interrupted. Please try again.", e)
        } catch (e: GetCredentialException) {
            logE("Credential retrieval failed (${e::class.java.simpleName}: ${e.type})")
            AppResult.Error("Google Sign-In failed. Please check your connection and try again.", e)
        } catch (e: GoogleIdTokenParsingException) {
            logE("Failed to parse GoogleIdTokenCredential (${e::class.java.simpleName})")
            AppResult.Error("Failed to read Google Sign-In credential. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logE("Unexpected exception during Google Sign-In (${e::class.java.simpleName})")
            AppResult.Error("An unexpected error occurred during Google Sign-In", e)
        }
    }

    companion object {
        private const val TAG = "GoogleIdTokenProvider"
        const val ERROR_SIGN_IN_CANCELLED = "Sign-in cancelled"

        private fun logI(message: String) {
            runCatching { Log.i(TAG, message) }
        }

        private fun logW(message: String) {
            runCatching { Log.w(TAG, message) }
        }

        private fun logE(message: String) {
            runCatching { Log.e(TAG, message) }
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext?.findActivity()
    else -> null
}

