package com.pratyush.scribble.feature.auth.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AccountType
import com.pratyush.scribble.core.model.AuthState
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.UserProfile
import com.pratyush.scribble.core.network.repository.ProfileRepository
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

internal data class AuthenticatedFirebaseUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val isAnonymous: Boolean
)

internal interface FirebaseAuthDataSource {
    val currentUser: AuthenticatedFirebaseUser?
    fun addAuthStateListener(listener: (AuthenticatedFirebaseUser?) -> Unit)
    suspend fun signInWithGoogleIdToken(idToken: String): AuthenticatedFirebaseUser?
    suspend fun signInAnonymously(): AuthenticatedFirebaseUser?
    suspend fun linkCurrentUserWithGoogleIdToken(idToken: String): AuthenticatedFirebaseUser?
    fun signOut()
    suspend fun getCurrentUserIdToken(forceRefresh: Boolean): String?
}

private class RealFirebaseAuthDataSource(
    private val firebaseAuth: FirebaseAuth
) : FirebaseAuthDataSource {
    override val currentUser: AuthenticatedFirebaseUser?
        get() = firebaseAuth.currentUser?.toAuthenticatedFirebaseUser()

    override fun addAuthStateListener(listener: (AuthenticatedFirebaseUser?) -> Unit) {
        firebaseAuth.addAuthStateListener { auth ->
            listener(auth.currentUser?.toAuthenticatedFirebaseUser())
        }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): AuthenticatedFirebaseUser? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return firebaseAuth.signInWithCredential(credential).await().user?.toAuthenticatedFirebaseUser()
    }

    override suspend fun signInAnonymously(): AuthenticatedFirebaseUser? {
        return firebaseAuth.signInAnonymously().await().user?.toAuthenticatedFirebaseUser()
    }

    override suspend fun linkCurrentUserWithGoogleIdToken(idToken: String): AuthenticatedFirebaseUser? {
        val user = firebaseAuth.currentUser ?: return null
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val linked = user.linkWithCredential(credential).await().user ?: firebaseAuth.currentUser ?: user
        return linked.toAuthenticatedFirebaseUser()
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun getCurrentUserIdToken(forceRefresh: Boolean): String? {
        val user = firebaseAuth.currentUser ?: return null
        return user.getIdToken(forceRefresh).await().token
    }

    private fun FirebaseUser.toAuthenticatedFirebaseUser(): AuthenticatedFirebaseUser =
        AuthenticatedFirebaseUser(
            uid = uid,
            displayName = displayName,
            email = email,
            isAnonymous = isAnonymous
        )
}

/**
 * Production implementation of [AuthRepository] backed by Firebase Authentication and Firestore profiles.
 */
class FirebaseAuthRepository internal constructor(
    private val authDataSource: FirebaseAuthDataSource,
    private val googleIdTokenSource: suspend (Context?) -> AppResult<String>,
    private val profileRepository: ProfileRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : AuthRepository {

    constructor(
        firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
        googleIdTokenSource: suspend (Context?) -> AppResult<String>,
        profileRepository: ProfileRepository? = null,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    ) : this(
        authDataSource = RealFirebaseAuthDataSource(firebaseAuth),
        googleIdTokenSource = googleIdTokenSource,
        profileRepository = profileRepository,
        scope = scope
    )

    constructor(
        firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
        googleIdTokenSource: suspend () -> AppResult<String>,
        profileRepository: ProfileRepository? = null,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    ) : this(
        authDataSource = RealFirebaseAuthDataSource(firebaseAuth),
        googleIdTokenSource = { _ -> googleIdTokenSource() },
        profileRepository = profileRepository,
        scope = scope
    )

    private val profileLock = Any()
    private val workContext = scope.coroutineContext[kotlin.coroutines.ContinuationInterceptor] ?: Dispatchers.IO
    private val initialUser: AuthenticatedFirebaseUser? = authDataSource.currentUser
    private val initialProfile: UserProfile? = if (profileRepository == null) {
        initialUser?.toUserProfile()
    } else {
        null
    }

    private val _currentUser = MutableStateFlow<UserProfile?>(initialProfile)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(
        if (initialProfile != null) AuthState.Authenticated(initialProfile) else AuthState.Loading
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var profileInitDeferred: Deferred<UserProfile>? = null
    private var profileInitUid: String? = null
    private var profileObserveJob: Job? = null
    private var activeObserveUid: String? = null

    @Volatile
    private var isExplicitAuthInProgress: Boolean = false

    init {
        authDataSource.addAuthStateListener { user ->
            if (user == null) {
                if (!isExplicitAuthInProgress) {
                    clearProfileJobs()
                    _currentUser.value = null
                    _authState.value = AuthState.Unauthenticated
                }
            } else if (!isExplicitAuthInProgress) {
                val alreadyActiveOrInitializing = synchronized(profileLock) {
                    (activeObserveUid == user.uid && (profileObserveJob?.isActive == true || profileRepository == null)) ||
                        (profileInitUid == user.uid && profileInitDeferred?.isActive == true)
                }
                if (!alreadyActiveOrInitializing) {
                    initializeAndObserveProfile(user)
                }
            }
        }
    }

    private fun clearProfileJobs() = synchronized(profileLock) {
        profileInitDeferred?.cancel()
        profileInitDeferred = null
        profileInitUid = null
        profileObserveJob?.cancel()
        profileObserveJob = null
        activeObserveUid = null
    }

    private fun initializeAndObserveProfile(
        user: AuthenticatedFirebaseUser,
        preferredDisplayName: String? = null,
        preferredAccountType: AccountType? = null
    ): Deferred<UserProfile> = synchronized(profileLock) {
        val existingDeferred = profileInitDeferred
        if (profileInitUid == user.uid && existingDeferred != null && existingDeferred.isActive) {
            return existingDeferred
        }

        if (profileInitUid != null && profileInitUid != user.uid) {
            profileInitDeferred?.cancel()
        }
        if (activeObserveUid != null && activeObserveUid != user.uid) {
            profileObserveJob?.cancel()
            profileObserveJob = null
            activeObserveUid = null
        }

        val resolvedAccountType = preferredAccountType
            ?: if (user.isAnonymous) AccountType.GUEST else AccountType.PERMANENT
        val resolvedDefaultName = preferredDisplayName?.takeIf { it.isNotBlank() }
            ?: user.displayName?.takeIf { it.isNotBlank() }
            ?: "Player_${user.uid.take(4)}"

        profileInitUid = user.uid
        val deferred = scope.async {
            val fallbackProfile = UserProfile(
                id = user.uid,
                displayName = resolvedDefaultName,
                email = user.email,
                avatarId = AvatarId.CAT,
                accountType = resolvedAccountType
            )
            val resolvedProfile = if (profileRepository != null) {
                when (val res = profileRepository.getOrCreateProfile(
                    uid = user.uid,
                    defaultDisplayName = resolvedDefaultName,
                    defaultAvatarId = AvatarId.CAT,
                    email = user.email,
                    accountType = resolvedAccountType
                )) {
                    is AppResult.Success -> res.data
                    else -> fallbackProfile
                }
            } else {
                fallbackProfile
            }

            _currentUser.value = resolvedProfile
            _authState.value = AuthState.Authenticated(resolvedProfile)
            startObservingProfile(user.uid)
            resolvedProfile
        }
        profileInitDeferred = deferred
        deferred
    }

    private fun startObservingProfile(uid: String) = synchronized(profileLock) {
        if (profileRepository == null) {
            activeObserveUid = uid
            return
        }
        if (activeObserveUid == uid && profileObserveJob?.isActive == true) {
            return
        }
        profileObserveJob?.cancel()
        activeObserveUid = uid
        profileObserveJob = scope.launch {
            profileRepository.observeProfile(uid).collect { updatedProfile ->
                if (updatedProfile != null) {
                    _currentUser.value = updatedProfile
                    _authState.value = AuthState.Authenticated(updatedProfile)
                }
            }
        }
    }

    override suspend fun signInWithGoogle(): AppResult<UserProfile> = signInWithGoogleInternal(null)

    override suspend fun signInWithGoogle(activityContext: Context): AppResult<UserProfile> =
        signInWithGoogleInternal(activityContext)

    private suspend fun signInWithGoogleInternal(activityContext: Context?): AppResult<UserProfile> = withContext(workContext) {
        val currentFirebaseUser = authDataSource.currentUser
        if (currentFirebaseUser != null && currentFirebaseUser.isAnonymous) {
            // Already logged in as an anonymous guest: link credentials to preserve guest UID!
            return@withContext linkGuestWithGoogleInternal(activityContext)
        }

        val tokenResult = googleIdTokenSource(activityContext)
        if (tokenResult !is AppResult.Success) {
            return@withContext AppResult.Error((tokenResult as? AppResult.Error)?.message ?: "Failed to retrieve Google ID token")
        }

        val idToken = tokenResult.data.trim()
        if (idToken.isEmpty()) {
            logE("Firebase Google sign-in aborted: idToken is blank")
            return@withContext AppResult.Error("Received an empty Google Sign-In token. Please try again.")
        }

        isExplicitAuthInProgress = true
        try {
            val user = authDataSource.signInWithGoogleIdToken(idToken)
            if (user != null) {
                val profile = initializeAndObserveProfile(
                    user = user,
                    preferredAccountType = AccountType.PERMANENT
                ).await()
                AppResult.Success(profile)
            } else {
                logE("Firebase signInWithCredential succeeded but user was null")
                AppResult.Error("Firebase sign-in completed but user profile was null")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logE("Firebase Google sign-in failed (${e::class.java.simpleName})")
            AppResult.Error(e.localizedMessage ?: "Firebase Google sign-in failed", e)
        } finally {
            isExplicitAuthInProgress = false
        }
    }

    override suspend fun signInAsGuest(displayName: String): AppResult<UserProfile> = withContext(workContext) {
        val resolvedName = displayName.trim().ifBlank { "Guest_${(100..999).random()}" }
        isExplicitAuthInProgress = true
        try {
            val user = authDataSource.signInAnonymously()
            if (user != null) {
                val profile = initializeAndObserveProfile(
                    user = user,
                    preferredDisplayName = resolvedName,
                    preferredAccountType = AccountType.GUEST
                ).await()
                AppResult.Success(profile)
            } else {
                AppResult.Error("Anonymous sign-in completed but user was null")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logE("Guest sign-in failed (${e::class.java.simpleName})")
            AppResult.Error(e.localizedMessage ?: "Guest sign-in failed", e)
        } finally {
            isExplicitAuthInProgress = false
        }
    }

    override suspend fun linkGuestWithGoogle(): AppResult<UserProfile> = linkGuestWithGoogleInternal(null)

    override suspend fun linkGuestWithGoogle(activityContext: Context): AppResult<UserProfile> =
        linkGuestWithGoogleInternal(activityContext)

    private suspend fun linkGuestWithGoogleInternal(activityContext: Context?): AppResult<UserProfile> = withContext(workContext) {
        val currentUser = authDataSource.currentUser
            ?: return@withContext AppResult.Error("No user currently signed in")

        if (!currentUser.isAnonymous) {
            return@withContext signInWithGoogleInternal(activityContext)
        }

        val tokenResult = googleIdTokenSource(activityContext)
        if (tokenResult !is AppResult.Success) {
            return@withContext AppResult.Error((tokenResult as? AppResult.Error)?.message ?: "Failed to retrieve Google ID token")
        }

        val idToken = tokenResult.data.trim()
        if (idToken.isEmpty()) {
            logE("Guest-to-Google linking aborted: idToken is blank")
            return@withContext AppResult.Error("Received an empty Google Sign-In token. Please try again.")
        }

        isExplicitAuthInProgress = true
        try {
            val linkedUser = authDataSource.linkCurrentUserWithGoogleIdToken(idToken) ?: currentUser
            val uid = linkedUser.uid

            val updatedProfile = scope.async {
                if (profileRepository != null) {
                    profileRepository.upgradeGuestToPermanent(uid, linkedUser.email)
                }

                val profile = if (profileRepository != null) {
                    when (val res = profileRepository.getProfile(uid)) {
                        is AppResult.Success -> res.data ?: linkedUser.toUserProfile().copy(accountType = AccountType.PERMANENT)
                        else -> linkedUser.toUserProfile().copy(accountType = AccountType.PERMANENT)
                    }
                } else {
                    linkedUser.toUserProfile().copy(accountType = AccountType.PERMANENT)
                }

                _currentUser.value = profile
                _authState.value = AuthState.Authenticated(profile)
                startObservingProfile(uid)
                profile
            }.await()

            AppResult.Success(updatedProfile)
        } catch (collisionEx: FirebaseAuthUserCollisionException) {
            // Account collision: Google account is already linked to another Firebase user.
            // Requirement 6: Handle collision gracefully, do not delete or overwrite either account, sign into the existing Google account.
            logW("Account collision during guest upgrade (${collisionEx::class.java.simpleName}); signing into existing Google account")
            try {
                val existingUser = authDataSource.signInWithGoogleIdToken(idToken)
                if (existingUser != null) {
                    val profile = initializeAndObserveProfile(
                        user = existingUser,
                        preferredAccountType = AccountType.PERMANENT
                    ).await()
                    AppResult.Success(profile)
                } else {
                    AppResult.Error("Failed to sign into existing account")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logE("Failed to sign into existing Google account after collision (${e::class.java.simpleName})")
                AppResult.Error(e.localizedMessage ?: "Failed to sign into existing Google account", e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logE("Failed to link Google account (${e::class.java.simpleName})")
            AppResult.Error(e.localizedMessage ?: "Failed to link Google account", e)
        } finally {
            isExplicitAuthInProgress = false
        }
    }

    override suspend fun checkGuestEligibility(): AppResult<Boolean> = withContext(workContext) {
        val user = authDataSource.currentUser
            ?: return@withContext AppResult.Success(true)
        if (!user.isAnonymous) {
            return@withContext AppResult.Success(true)
        }
        if (profileRepository != null) {
            profileRepository.checkGuestMatchEligibility(user.uid)
        } else {
            AppResult.Success(true)
        }
    }

    override suspend fun signOut(): AppResult<Unit> = withContext(workContext) {
        try {
            clearProfileJobs()
            authDataSource.signOut()
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logE("Sign out failed (${e::class.java.simpleName})")
            AppResult.Error(e.localizedMessage ?: "Failed to sign out", e)
        }
    }

    override suspend fun getIdToken(forceRefresh: Boolean): AppResult<String> = withContext(workContext) {
        if (authDataSource.currentUser == null) {
            return@withContext AppResult.Error("No user currently signed in")
        }

        try {
            val token = authDataSource.getCurrentUserIdToken(forceRefresh)
            if (!token.isNullOrBlank()) {
                AppResult.Success(token)
            } else {
                AppResult.Error("Retrieved empty Firebase ID token")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logE("Failed to retrieve Firebase ID token (${e::class.java.simpleName})")
            AppResult.Error(e.localizedMessage ?: "Failed to retrieve Firebase ID token", e)
        }
    }

    private fun AuthenticatedFirebaseUser.toUserProfile(): UserProfile {
        val name = displayName?.takeIf { it.isNotBlank() } ?: "Player_${uid.take(4)}"
        val accountType = if (isAnonymous) AccountType.GUEST else AccountType.PERMANENT
        return UserProfile(
            id = uid,
            displayName = name,
            email = email,
            avatarId = AvatarId.CAT,
            accountType = accountType
        )
    }

    companion object {
        private const val TAG = "FirebaseAuthRepository"

        private fun logW(message: String) {
            runCatching { Log.w(TAG, message) }
        }

        private fun logE(message: String) {
            runCatching { Log.e(TAG, message) }
        }
    }
}

