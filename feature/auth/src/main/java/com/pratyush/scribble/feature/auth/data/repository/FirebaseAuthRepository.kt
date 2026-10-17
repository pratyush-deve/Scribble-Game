package com.pratyush.scribble.feature.auth.data.repository

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Production implementation of [AuthRepository] backed by Firebase Authentication and Firestore profiles.
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val googleIdTokenSource: suspend () -> AppResult<String>,
    private val profileRepository: ProfileRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : AuthRepository {

    private val initialUser: FirebaseUser? = firebaseAuth.currentUser
    private val initialProfile: UserProfile? = initialUser?.toUserProfile()

    private val _currentUser = MutableStateFlow<UserProfile?>(initialProfile)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(
        if (initialProfile != null) AuthState.Authenticated(initialProfile) else AuthState.Loading
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var profileObserveJob: Job? = null

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            profileObserveJob?.cancel()
            if (user == null) {
                _currentUser.value = null
                _authState.value = AuthState.Unauthenticated
            } else {
                val profile = user.toUserProfile()
                _currentUser.value = profile
                _authState.value = AuthState.Authenticated(profile)
                if (profileRepository != null) {
                    profileObserveJob = scope.launch {
                        val accountType = if (user.isAnonymous) AccountType.GUEST else AccountType.PERMANENT
                        val res = profileRepository.getOrCreateProfile(
                            uid = user.uid,
                            defaultDisplayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Player_${user.uid.take(4)}",
                            defaultAvatarId = AvatarId.CAT,
                            email = user.email,
                            accountType = accountType
                        )
                        if (res is AppResult.Success) {
                            _currentUser.value = res.data
                            _authState.value = AuthState.Authenticated(res.data)
                        }
                        profileRepository.observeProfile(user.uid).collect { updatedProfile ->
                            if (updatedProfile != null) {
                                _currentUser.value = updatedProfile
                                _authState.value = AuthState.Authenticated(updatedProfile)
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun signInWithGoogle(): AppResult<UserProfile> = withContext(Dispatchers.IO) {
        val currentFirebaseUser = firebaseAuth.currentUser
        if (currentFirebaseUser != null && currentFirebaseUser.isAnonymous) {
            // Already logged in as an anonymous guest: link credentials to preserve guest UID!
            return@withContext linkGuestWithGoogle()
        }

        val tokenResult = googleIdTokenSource()
        if (tokenResult !is AppResult.Success) {
            return@withContext AppResult.Error((tokenResult as? AppResult.Error)?.message ?: "Failed to retrieve Google ID token")
        }

        val idToken = tokenResult.data
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                val profile = if (profileRepository != null) {
                    when (val res = profileRepository.getOrCreateProfile(
                        uid = user.uid,
                        defaultDisplayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Player_${user.uid.take(4)}",
                        defaultAvatarId = AvatarId.CAT,
                        email = user.email,
                        accountType = AccountType.PERMANENT
                    )) {
                        is AppResult.Success -> res.data
                        else -> user.toUserProfile()
                    }
                } else {
                    user.toUserProfile()
                }
                _currentUser.value = profile
                _authState.value = AuthState.Authenticated(profile)
                AppResult.Success(profile)
            } else {
                AppResult.Error("Firebase sign-in completed but user profile was null")
            }
        } catch (e: Exception) {
            AppResult.Error(e.localizedMessage ?: "Firebase Google sign-in failed")
        }
    }

    override suspend fun signInAsGuest(displayName: String): AppResult<UserProfile> = withContext(Dispatchers.IO) {
        val resolvedName = displayName.trim().ifBlank { "Guest_${(100..999).random()}" }
        try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val user = authResult.user
            if (user != null) {
                val profile = if (profileRepository != null) {
                    when (val res = profileRepository.getOrCreateProfile(
                        uid = user.uid,
                        defaultDisplayName = resolvedName,
                        defaultAvatarId = AvatarId.CAT,
                        email = null,
                        accountType = AccountType.GUEST
                    )) {
                        is AppResult.Success -> res.data
                        else -> UserProfile(
                            id = user.uid,
                            displayName = resolvedName,
                            email = null,
                            avatarId = AvatarId.CAT,
                            accountType = AccountType.GUEST
                        )
                    }
                } else {
                    UserProfile(
                        id = user.uid,
                        displayName = resolvedName,
                        email = null,
                        avatarId = AvatarId.CAT,
                        accountType = AccountType.GUEST
                    )
                }
                _currentUser.value = profile
                _authState.value = AuthState.Authenticated(profile)
                AppResult.Success(profile)
            } else {
                AppResult.Error("Anonymous sign-in completed but user was null")
            }
        } catch (e: Exception) {
            AppResult.Error(e.localizedMessage ?: "Guest sign-in failed")
        }
    }

    override suspend fun linkGuestWithGoogle(): AppResult<UserProfile> = withContext(Dispatchers.IO) {
        val currentUser = firebaseAuth.currentUser
            ?: return@withContext AppResult.Error("No user currently signed in")

        val tokenResult = googleIdTokenSource()
        if (tokenResult !is AppResult.Success) {
            return@withContext AppResult.Error((tokenResult as? AppResult.Error)?.message ?: "Failed to retrieve Google ID token")
        }

        val idToken = tokenResult.data
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        if (!currentUser.isAnonymous) {
            return@withContext signInWithGoogle()
        }

        try {
            val linkResult = currentUser.linkWithCredential(credential).await()
            val linkedUser = linkResult.user ?: currentUser
            val uid = linkedUser.uid

            if (profileRepository != null) {
                profileRepository.upgradeGuestToPermanent(uid, linkedUser.email)
            }

            val updatedProfile = if (profileRepository != null) {
                when (val res = profileRepository.getProfile(uid)) {
                    is AppResult.Success -> res.data ?: linkedUser.toUserProfile().copy(accountType = AccountType.PERMANENT)
                    else -> linkedUser.toUserProfile().copy(accountType = AccountType.PERMANENT)
                }
            } else {
                linkedUser.toUserProfile().copy(accountType = AccountType.PERMANENT)
            }

            _currentUser.value = updatedProfile
            _authState.value = AuthState.Authenticated(updatedProfile)
            AppResult.Success(updatedProfile)
        } catch (collisionEx: FirebaseAuthUserCollisionException) {
            // Account collision: Google account is already linked to another Firebase user.
            // Requirement 6: Handle collision gracefully, do not delete or overwrite either account, sign into the existing Google account.
            try {
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val existingUser = authResult.user
                if (existingUser != null) {
                    val profile = if (profileRepository != null) {
                        when (val res = profileRepository.getOrCreateProfile(
                            uid = existingUser.uid,
                            defaultDisplayName = existingUser.displayName?.takeIf { it.isNotBlank() } ?: "Player_${existingUser.uid.take(4)}",
                            defaultAvatarId = AvatarId.CAT,
                            email = existingUser.email,
                            accountType = AccountType.PERMANENT
                        )) {
                            is AppResult.Success -> res.data
                            else -> existingUser.toUserProfile().copy(accountType = AccountType.PERMANENT)
                        }
                    } else {
                        existingUser.toUserProfile().copy(accountType = AccountType.PERMANENT)
                    }
                    _currentUser.value = profile
                    _authState.value = AuthState.Authenticated(profile)
                    AppResult.Success(profile)
                } else {
                    AppResult.Error("Failed to sign into existing account")
                }
            } catch (e: Exception) {
                AppResult.Error(e.localizedMessage ?: "Failed to sign into existing Google account")
            }
        } catch (e: Exception) {
            AppResult.Error(e.localizedMessage ?: "Failed to link Google account")
        }
    }

    override suspend fun checkGuestEligibility(): AppResult<Boolean> = withContext(Dispatchers.IO) {
        val user = firebaseAuth.currentUser
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

    override suspend fun signOut(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            profileObserveJob?.cancel()
            profileObserveJob = null
            firebaseAuth.signOut()
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e.localizedMessage ?: "Failed to sign out")
        }
    }

    override suspend fun getIdToken(forceRefresh: Boolean): AppResult<String> = withContext(Dispatchers.IO) {
        val user = firebaseAuth.currentUser
            ?: return@withContext AppResult.Error("No user currently signed in")

        try {
            val tokenResult = user.getIdToken(forceRefresh).await()
            val token = tokenResult.token
            if (!token.isNullOrBlank()) {
                AppResult.Success(token)
            } else {
                AppResult.Error("Retrieved empty Firebase ID token")
            }
        } catch (e: Exception) {
            AppResult.Error(e.localizedMessage ?: "Failed to retrieve Firebase ID token")
        }
    }

    private fun FirebaseUser.toUserProfile(): UserProfile {
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
}
