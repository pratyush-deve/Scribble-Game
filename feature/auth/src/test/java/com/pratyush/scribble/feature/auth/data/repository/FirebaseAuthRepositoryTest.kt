package com.pratyush.scribble.feature.auth.data.repository

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AccountType
import com.pratyush.scribble.core.model.AuthState
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.MatchRecord
import com.pratyush.scribble.core.model.UserProfile
import com.pratyush.scribble.core.network.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAuthRepositoryTest {

    @Test
    fun `signInWithGoogle waits for getOrCreateProfile, deduplicates listener calls, and does not cancel profile init`() = runTest {
        val fakeAuthDataSource = FakeFirebaseAuthDataSource()
        val profileDeferred = CompletableDeferred<UserProfile>()
        val fakeProfileRepository = SuspendingFakeProfileRepository(profileDeferred)
        val repoScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        val repository = FirebaseAuthRepository(
            authDataSource = fakeAuthDataSource,
            googleIdTokenSource = { _ -> AppResult.Success("valid_google_id_token") },
            profileRepository = fakeProfileRepository,
            scope = repoScope
        )

        var signInResult: AppResult<UserProfile>? = null
        val callerScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        callerScope.launch {
            signInResult = repository.signInWithGoogle()
        }

        // Let signInWithGoogleIdToken fire AuthStateListener multiple times while getOrCreateProfile is suspended
        testScheduler.advanceUntilIdle()

        // Profile initialization is still suspended: currentUser must still be null and authState must not be Authenticated yet
        assertNull(repository.currentUser.value)
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
        assertNull(signInResult)
        assertEquals(1, fakeProfileRepository.getOrCreateCallCount)
        assertFalse(fakeProfileRepository.wasCancelled)

        // Complete Firestore getOrCreateProfile
        val expectedProfile = UserProfile(
            id = "google_uid_123",
            displayName = "SavedFirestoreName",
            email = "player@gmail.com",
            avatarId = AvatarId.PANDA,
            accountType = AccountType.PERMANENT
        )
        profileDeferred.complete(expectedProfile)
        testScheduler.advanceUntilIdle()

        assertNotNull(signInResult)
        assertTrue(signInResult is AppResult.Success)
        assertEquals(expectedProfile, (signInResult as AppResult.Success).data)
        assertEquals(expectedProfile, repository.currentUser.value)
        assertEquals(AuthState.Authenticated(expectedProfile), repository.authState.value)
        assertEquals(1, fakeProfileRepository.getOrCreateCallCount)
        assertFalse(fakeProfileRepository.wasCancelled)

        callerScope.cancel()
        repoScope.cancel()
    }

    @Test
    fun `cancelling caller ViewModel scope during signInWithGoogle does not cancel repository profile creation`() = runTest {
        val fakeAuthDataSource = FakeFirebaseAuthDataSource()
        val profileDeferred = CompletableDeferred<UserProfile>()
        val fakeProfileRepository = SuspendingFakeProfileRepository(profileDeferred)
        val repoScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        val repository = FirebaseAuthRepository(
            authDataSource = fakeAuthDataSource,
            googleIdTokenSource = { _ -> AppResult.Success("valid_google_id_token") },
            profileRepository = fakeProfileRepository,
            scope = repoScope
        )

        val callerScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        var callerCancelledWithException: CancellationException? = null
        val job = callerScope.launch {
            try {
                repository.signInWithGoogle()
            } catch (e: CancellationException) {
                callerCancelledWithException = e
                throw e
            }
        }

        testScheduler.advanceUntilIdle()
        assertEquals(1, fakeProfileRepository.getOrCreateCallCount)

        // Simulate ViewModel disposal while getOrCreateProfile is in flight
        callerScope.cancel()
        testScheduler.advanceUntilIdle()
        assertTrue(job.isCancelled)
        assertNotNull(callerCancelledWithException)

        // The underlying Firestore getOrCreateProfile on repository scope must NOT be cancelled
        assertFalse(fakeProfileRepository.wasCancelled)

        val createdProfile = UserProfile(
            id = "google_uid_123",
            displayName = "GoogleUser",
            email = "player@gmail.com",
            avatarId = AvatarId.CAT,
            accountType = AccountType.PERMANENT
        )
        profileDeferred.complete(createdProfile)
        testScheduler.advanceUntilIdle()

        assertEquals(createdProfile, repository.currentUser.value)
        assertEquals(AuthState.Authenticated(createdProfile), repository.authState.value)

        repoScope.cancel()
    }

    @Test
    fun `repeated AuthStateListener callbacks for same UID do not cancel in-flight getOrCreateProfile`() = runTest {
        val fakeAuthDataSource = FakeFirebaseAuthDataSource()
        val profileDeferred = CompletableDeferred<UserProfile>()
        val fakeProfileRepository = SuspendingFakeProfileRepository(profileDeferred)
        val repoScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        val repository = FirebaseAuthRepository(
            authDataSource = fakeAuthDataSource,
            googleIdTokenSource = { _ -> AppResult.Success("valid_google_id_token") },
            profileRepository = fakeProfileRepository,
            scope = repoScope
        )

        val user = AuthenticatedFirebaseUser(
            uid = "uid_repeat_1",
            displayName = "RepeatUser",
            email = "repeat@gmail.com",
            isAnonymous = false
        )

        // Fire AuthStateListener multiple times (e.g. token refresh + auth state notification)
        fakeAuthDataSource.emitAuthState(user)
        testScheduler.advanceUntilIdle()
        fakeAuthDataSource.emitAuthState(user)
        testScheduler.advanceUntilIdle()

        assertEquals(1, fakeProfileRepository.getOrCreateCallCount)
        assertFalse(fakeProfileRepository.wasCancelled)

        val expectedProfile = UserProfile(
            id = "uid_repeat_1",
            displayName = "RepeatUser",
            email = "repeat@gmail.com",
            avatarId = AvatarId.CAT,
            accountType = AccountType.PERMANENT
        )
        profileDeferred.complete(expectedProfile)
        testScheduler.advanceUntilIdle()

        assertEquals(expectedProfile, repository.currentUser.value)
        assertEquals(1, fakeProfileRepository.getOrCreateCallCount)
        assertFalse(fakeProfileRepository.wasCancelled)

        repoScope.cancel()
    }

    @Test
    fun `signInAsGuest preserves custom guest nickname when AuthStateListener fires with null displayName`() = runTest {
        val fakeAuthDataSource = FakeFirebaseAuthDataSource()
        val profileDeferred = CompletableDeferred<UserProfile>()
        val fakeProfileRepository = SuspendingFakeProfileRepository(profileDeferred)
        val repoScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        val repository = FirebaseAuthRepository(
            authDataSource = fakeAuthDataSource,
            googleIdTokenSource = { _ -> AppResult.Success("token") },
            profileRepository = fakeProfileRepository,
            scope = repoScope
        )

        var guestResult: AppResult<UserProfile>? = null
        val callerScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        callerScope.launch {
            guestResult = repository.signInAsGuest("SpeedyDoodler")
        }

        testScheduler.advanceUntilIdle()
        assertEquals(1, fakeProfileRepository.getOrCreateCallCount)
        assertEquals("SpeedyDoodler", fakeProfileRepository.lastRequestedDisplayName)
        assertEquals(AccountType.GUEST, fakeProfileRepository.lastRequestedAccountType)

        val createdGuest = UserProfile(
            id = "anon_uid_999",
            displayName = "SpeedyDoodler",
            avatarId = AvatarId.CAT,
            accountType = AccountType.GUEST
        )
        profileDeferred.complete(createdGuest)
        testScheduler.advanceUntilIdle()

        assertTrue(guestResult is AppResult.Success)
        assertEquals("SpeedyDoodler", (guestResult as AppResult.Success).data.displayName)
        assertEquals("SpeedyDoodler", repository.currentUser.value?.displayName)

        callerScope.cancel()
        repoScope.cancel()
    }

    private class FakeFirebaseAuthDataSource : FirebaseAuthDataSource {
        override var currentUser: AuthenticatedFirebaseUser? = null
        private val listeners = mutableListOf<(AuthenticatedFirebaseUser?) -> Unit>()

        override fun addAuthStateListener(listener: (AuthenticatedFirebaseUser?) -> Unit) {
            listeners.add(listener)
            listener(currentUser)
        }

        fun emitAuthState(user: AuthenticatedFirebaseUser?) {
            currentUser = user
            listeners.forEach { it(user) }
        }

        override suspend fun signInWithGoogleIdToken(idToken: String): AuthenticatedFirebaseUser {
            val user = AuthenticatedFirebaseUser(
                uid = "google_uid_123",
                displayName = "GoogleUser",
                email = "player@gmail.com",
                isAnonymous = false
            )
            // Simulate Firebase notifying both ID-token / auth-state listeners immediately
            emitAuthState(user)
            emitAuthState(user)
            return user
        }

        override suspend fun signInAnonymously(): AuthenticatedFirebaseUser {
            val user = AuthenticatedFirebaseUser(
                uid = "anon_uid_999",
                displayName = null,
                email = null,
                isAnonymous = true
            )
            emitAuthState(user)
            return user
        }

        override suspend fun linkCurrentUserWithGoogleIdToken(idToken: String): AuthenticatedFirebaseUser? {
            val cur = currentUser ?: return null
            val linked = cur.copy(email = "player@gmail.com", isAnonymous = false)
            emitAuthState(linked)
            return linked
        }

        override fun signOut() {
            emitAuthState(null)
        }

        override suspend fun getCurrentUserIdToken(forceRefresh: Boolean): String = "fake_firebase_jwt"
    }

    private class SuspendingFakeProfileRepository(
        private val profileDeferred: CompletableDeferred<UserProfile>
    ) : ProfileRepository {
        private val profileFlow = MutableStateFlow<UserProfile?>(null)
        var getOrCreateCallCount: Int = 0
        var wasCancelled: Boolean = false
        var lastRequestedDisplayName: String? = null
        var lastRequestedAccountType: AccountType? = null

        override suspend fun getProfile(uid: String): AppResult<UserProfile?> =
            AppResult.Success(profileFlow.value)

        override suspend fun getOrCreateProfile(
            uid: String,
            defaultDisplayName: String,
            defaultAvatarId: AvatarId,
            email: String?,
            accountType: AccountType
        ): AppResult<UserProfile> {
            getOrCreateCallCount++
            lastRequestedDisplayName = defaultDisplayName
            lastRequestedAccountType = accountType
            return try {
                val profile = profileDeferred.await()
                profileFlow.value = profile
                AppResult.Success(profile)
            } catch (e: CancellationException) {
                wasCancelled = true
                throw e
            }
        }

        override fun observeProfile(uid: String): Flow<UserProfile?> = profileFlow.asStateFlow()
        override suspend fun updateDisplayName(uid: String, displayName: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateAvatar(uid: String, avatarId: AvatarId): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateProfile(uid: String, displayName: String, avatarId: AvatarId): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateLastSeen(uid: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getMatchHistory(uid: String): AppResult<List<MatchRecord>> = AppResult.Success(emptyList())
        override suspend fun upgradeGuestToPermanent(uid: String, email: String?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun checkGuestMatchEligibility(uid: String): AppResult<Boolean> = AppResult.Success(true)
    }
}
