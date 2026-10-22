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

    @Test
    fun `linkGuestWithGoogle upgrades guest profile in Firestore and emits permanent profile`() = runTest {
        val fakeAuthDataSource = FakeFirebaseAuthDataSource()
        val initialGuest = UserProfile(
            id = "guest_101",
            displayName = "GuestArtist",
            avatarId = AvatarId.FOX,
            accountType = AccountType.GUEST,
            guestMatchesCompleted = 2,
            stats = com.pratyush.scribble.core.model.UserStats(gamesPlayed = 2, wins = 1, totalScore = 400)
        )
        val profileDeferred = CompletableDeferred<UserProfile>()
        profileDeferred.complete(initialGuest)
        val fakeProfileRepository = SuspendingFakeProfileRepository(profileDeferred)
        fakeProfileRepository.profileFlow.value = initialGuest
        val repoScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        val repository = FirebaseAuthRepository(
            authDataSource = fakeAuthDataSource,
            googleIdTokenSource = { _ -> AppResult.Success("google_token_abc") },
            profileRepository = fakeProfileRepository,
            scope = repoScope
        )

        // Sign in as anonymous guest first
        fakeAuthDataSource.currentUser = AuthenticatedFirebaseUser("guest_101", null, null, isAnonymous = true)
        fakeAuthDataSource.emitAuthState(fakeAuthDataSource.currentUser)
        testScheduler.advanceUntilIdle()

        assertEquals(AccountType.GUEST, repository.currentUser.value?.accountType)
        assertEquals(2, repository.currentUser.value?.stats?.gamesPlayed)

        // Now link guest with Google
        val linkResult = repository.linkGuestWithGoogle()
        testScheduler.advanceUntilIdle()

        assertTrue(linkResult is AppResult.Success)
        val upgraded = (linkResult as AppResult.Success).data
        assertEquals("guest_101", upgraded.id) // Preserves original UID
        assertEquals("GuestArtist", upgraded.displayName) // Preserves display name
        assertEquals(AvatarId.FOX, upgraded.avatarId) // Preserves avatar
        assertEquals(AccountType.PERMANENT, upgraded.accountType) // Upgraded to permanent
        assertEquals(1, upgraded.stats.wins) // Preserves stats
        assertEquals(400, upgraded.stats.totalScore)
        assertEquals(AccountType.PERMANENT, repository.currentUser.value?.accountType)
        assertEquals(1, fakeProfileRepository.upgradeCallCount)

        repoScope.cancel()
    }

    @Test
    fun `linkGuestWithGoogle propagates Firestore upgrade error and does not fabricate permanent profile`() = runTest {
        val fakeAuthDataSource = FakeFirebaseAuthDataSource()
        val initialGuest = UserProfile(
            id = "guest_202",
            displayName = "PersistentGuest",
            avatarId = AvatarId.BEAR,
            accountType = AccountType.GUEST,
            guestMatchesCompleted = 2,
            stats = com.pratyush.scribble.core.model.UserStats(gamesPlayed = 2, wins = 2, totalScore = 800)
        )
        val profileDeferred = CompletableDeferred<UserProfile>()
        profileDeferred.complete(initialGuest)
        val fakeProfileRepository = SuspendingFakeProfileRepository(profileDeferred)
        fakeProfileRepository.profileFlow.value = initialGuest
        fakeProfileRepository.upgradeResult = AppResult.Error("PERMISSION_DENIED: update rejected")
        val repoScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        val repository = FirebaseAuthRepository(
            authDataSource = fakeAuthDataSource,
            googleIdTokenSource = { _ -> AppResult.Success("google_token_abc") },
            profileRepository = fakeProfileRepository,
            scope = repoScope
        )

        fakeAuthDataSource.currentUser = AuthenticatedFirebaseUser("guest_202", null, null, isAnonymous = true)
        fakeAuthDataSource.emitAuthState(fakeAuthDataSource.currentUser)
        testScheduler.advanceUntilIdle()

        val linkResult = repository.linkGuestWithGoogle()
        testScheduler.advanceUntilIdle()

        assertTrue(linkResult is AppResult.Error)
        assertEquals("PERMISSION_DENIED: update rejected", (linkResult as AppResult.Error).message)

        // Must NOT fabricate a permanent profile, must retain original guest profile and stats
        val currentProfile = repository.currentUser.value
        assertNotNull(currentProfile)
        assertEquals(AccountType.GUEST, currentProfile?.accountType)
        assertEquals("PersistentGuest", currentProfile?.displayName)
        assertEquals(AvatarId.BEAR, currentProfile?.avatarId)
        assertEquals(2, currentProfile?.stats?.gamesPlayed)
        assertEquals(800, currentProfile?.stats?.totalScore)

        repoScope.cancel()
    }

    @Test
    fun `recovery for previously linked non-anonymous account upgrades guest profile without re-requesting id token`() = runTest {
        val fakeAuthDataSource = FakeFirebaseAuthDataSource()
        var idTokenRequestCount = 0
        val existingGuest = UserProfile(
            id = "linked_uid_303",
            displayName = "LinkedGuest",
            avatarId = AvatarId.LION,
            accountType = AccountType.GUEST,
            guestMatchesCompleted = 2,
            stats = com.pratyush.scribble.core.model.UserStats(gamesPlayed = 2, wins = 0, totalScore = 150)
        )
        val profileDeferred = CompletableDeferred<UserProfile>()
        profileDeferred.complete(existingGuest)
        val fakeProfileRepository = SuspendingFakeProfileRepository(profileDeferred)
        fakeProfileRepository.profileFlow.value = existingGuest
        val repoScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        val repository = FirebaseAuthRepository(
            authDataSource = fakeAuthDataSource,
            googleIdTokenSource = { _ ->
                idTokenRequestCount++
                AppResult.Success("token")
            },
            profileRepository = fakeProfileRepository,
            scope = repoScope
        )

        // Auth already linked (isAnonymous == false), but Firestore profile is still GUEST
        val nonAnonUser = AuthenticatedFirebaseUser("linked_uid_303", "LinkedGuest", "user@gmail.com", isAnonymous = false)
        fakeAuthDataSource.currentUser = nonAnonUser
        fakeAuthDataSource.emitAuthState(nonAnonUser)
        testScheduler.advanceUntilIdle()

        // Calling linkGuestWithGoogle should enter recovery flow without requesting a Google ID token again
        val result = repository.linkGuestWithGoogle()
        testScheduler.advanceUntilIdle()

        assertEquals(0, idTokenRequestCount) // Did not request token again!
        assertTrue(result is AppResult.Success)
        val recovered = (result as AppResult.Success).data
        assertEquals(AccountType.PERMANENT, recovered.accountType)
        assertEquals("LinkedGuest", recovered.displayName)
        assertEquals(150, recovered.stats.totalScore)
        assertEquals(1, fakeProfileRepository.upgradeCallCount)
        assertEquals(AccountType.PERMANENT, repository.currentUser.value?.accountType)

        repoScope.cancel()
    }

    @Test
    fun `recovery for already permanent profile succeeds without upgrading again`() = runTest {
        val fakeAuthDataSource = FakeFirebaseAuthDataSource()
        val permanentProfile = UserProfile(
            id = "perm_uid_404",
            displayName = "AlreadyPerm",
            avatarId = AvatarId.OWL,
            accountType = AccountType.PERMANENT,
            stats = com.pratyush.scribble.core.model.UserStats(gamesPlayed = 10, wins = 5, totalScore = 2000)
        )
        val profileDeferred = CompletableDeferred<UserProfile>()
        profileDeferred.complete(permanentProfile)
        val fakeProfileRepository = SuspendingFakeProfileRepository(profileDeferred)
        fakeProfileRepository.profileFlow.value = permanentProfile
        val repoScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        val repository = FirebaseAuthRepository(
            authDataSource = fakeAuthDataSource,
            googleIdTokenSource = { _ -> AppResult.Success("token") },
            profileRepository = fakeProfileRepository,
            scope = repoScope
        )

        val nonAnonUser = AuthenticatedFirebaseUser("perm_uid_404", "AlreadyPerm", "perm@gmail.com", isAnonymous = false)
        fakeAuthDataSource.currentUser = nonAnonUser
        fakeAuthDataSource.emitAuthState(nonAnonUser)
        testScheduler.advanceUntilIdle()

        val result = repository.linkGuestWithGoogle()
        testScheduler.advanceUntilIdle()

        assertTrue(result is AppResult.Success)
        assertEquals(AccountType.PERMANENT, (result as AppResult.Success).data.accountType)
        assertEquals(0, fakeProfileRepository.upgradeCallCount) // No need to upgrade again

        repoScope.cancel()
    }

    @Test
    fun `recovery for missing profile returns meaningful error`() = runTest {
        val fakeAuthDataSource = FakeFirebaseAuthDataSource()
        val profileDeferred = CompletableDeferred<UserProfile>()
        val fakeProfileRepository = SuspendingFakeProfileRepository(profileDeferred)
        fakeProfileRepository.customGetProfileResult = AppResult.Success(null) // Document missing
        val repoScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        val repository = FirebaseAuthRepository(
            authDataSource = fakeAuthDataSource,
            googleIdTokenSource = { _ -> AppResult.Success("token") },
            profileRepository = fakeProfileRepository,
            scope = repoScope
        )

        val nonAnonUser = AuthenticatedFirebaseUser("missing_uid_505", "Missing", "missing@gmail.com", isAnonymous = false)
        fakeAuthDataSource.currentUser = nonAnonUser

        val result = repository.linkGuestWithGoogle()
        testScheduler.advanceUntilIdle()

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("Profile not found"))

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
        val profileFlow = MutableStateFlow<UserProfile?>(null)
        var getOrCreateCallCount: Int = 0
        var upgradeCallCount: Int = 0
        var upgradeResult: AppResult<Unit> = AppResult.Success(Unit)
        var customGetProfileResult: AppResult<UserProfile?>? = null
        var wasCancelled: Boolean = false
        var lastRequestedDisplayName: String? = null
        var lastRequestedAccountType: AccountType? = null

        override suspend fun getProfile(uid: String): AppResult<UserProfile?> =
            customGetProfileResult ?: AppResult.Success(profileFlow.value)

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
        override suspend fun upgradeGuestToPermanent(uid: String, email: String?): AppResult<Unit> {
            upgradeCallCount++
            return if (upgradeResult is AppResult.Success) {
                val cur = profileFlow.value
                if (cur != null) {
                    val upgraded = cur.copy(accountType = AccountType.PERMANENT, email = email ?: cur.email)
                    profileFlow.value = upgraded
                }
                AppResult.Success(Unit)
            } else {
                upgradeResult
            }
        }
        override suspend fun checkGuestMatchEligibility(uid: String): AppResult<Boolean> = AppResult.Success(true)
    }
}
