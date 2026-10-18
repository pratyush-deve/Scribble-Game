package com.pratyush.scribble.core.network.repository

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.MatchRecord
import com.pratyush.scribble.core.model.UserPreferences
import com.pratyush.scribble.core.model.UserProfile
import com.pratyush.scribble.core.model.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileRepositoryTest {

    private lateinit var profileRepository: InMemoryProfileRepository

    @Before
    fun setUp() {
        profileRepository = InMemoryProfileRepository()
    }

    @Test
    fun `first time user creates new profile with default stats and avatar`() = runTest {
        val result = profileRepository.getOrCreateProfile(
            uid = "user_new_123",
            defaultDisplayName = "NewPlayer",
            defaultAvatarId = AvatarId.DOG,
            email = "new@example.com"
        )

        assertTrue(result is AppResult.Success)
        val profile = (result as AppResult.Success).data
        assertEquals("user_new_123", profile.id)
        assertEquals("NewPlayer", profile.displayName)
        assertEquals(AvatarId.DOG, profile.avatarId)
        assertEquals(0, profile.stats.gamesPlayed)
        assertEquals(0, profile.stats.wins)
        assertEquals(0, profile.stats.losses)
        assertEquals(0, profile.stats.totalScore)
        assertTrue(profile.preferences.soundEnabled)
    }

    @Test
    fun `existing profile preserves custom name avatar and stats on subsequent logins`() = runTest {
        val existingProfile = UserProfile(
            id = "user_existing_456",
            displayName = "CustomLegend",
            avatarId = AvatarId.PANDA,
            stats = UserStats(
                gamesPlayed = 50,
                wins = 35,
                losses = 12,
                draws = 3,
                totalScore = 15400,
                bestScore = 950,
                winStreak = 7
            ),
            preferences = UserPreferences(soundEnabled = false)
        )
        profileRepository.seedProfile(existingProfile)

        // Login again with Google account name "GoogleDefaultName"
        val loginResult = profileRepository.getOrCreateProfile(
            uid = "user_existing_456",
            defaultDisplayName = "GoogleDefaultName",
            defaultAvatarId = AvatarId.CAT,
            email = "legend@gmail.com"
        )

        assertTrue(loginResult is AppResult.Success)
        val preserved = (loginResult as AppResult.Success).data

        // Must preserve existing custom attributes rather than overwriting
        assertEquals("CustomLegend", preserved.displayName)
        assertEquals(AvatarId.PANDA, preserved.avatarId)
        assertEquals(50, preserved.stats.gamesPlayed)
        assertEquals(35, preserved.stats.wins)
        assertEquals(15400, preserved.stats.totalScore)
        assertEquals(7, preserved.stats.winStreak)
        assertEquals(false, preserved.preferences.soundEnabled)
    }

    @Test
    fun `switching accounts loads distinct profiles without cross-contamination`() = runTest {
        val userA = UserProfile(
            id = "uid_alice",
            displayName = "AliceDraws",
            avatarId = AvatarId.RABBIT,
            stats = UserStats(gamesPlayed = 10, wins = 8, totalScore = 3000)
        )
        val userB = UserProfile(
            id = "uid_bob",
            displayName = "BobSketches",
            avatarId = AvatarId.OWL,
            stats = UserStats(gamesPlayed = 5, wins = 2, totalScore = 1200)
        )
        profileRepository.seedProfile(userA)
        profileRepository.seedProfile(userB)

        val profileA = (profileRepository.getProfile("uid_alice") as AppResult.Success).data
        val profileB = (profileRepository.getProfile("uid_bob") as AppResult.Success).data

        assertNotNull(profileA)
        assertNotNull(profileB)
        assertEquals("AliceDraws", profileA?.displayName)
        assertEquals(AvatarId.RABBIT, profileA?.avatarId)
        assertEquals("BobSketches", profileB?.displayName)
        assertEquals(AvatarId.OWL, profileB?.avatarId)
    }

    @Test
    fun `update display name validates length and persists`() = runTest {
        val user = UserProfile(id = "uid_test", displayName = "InitialName", avatarId = AvatarId.CAT)
        profileRepository.seedProfile(user)

        val emptyUpdate = profileRepository.updateDisplayName("uid_test", "   ")
        assertTrue(emptyUpdate is AppResult.Error)

        val tooLongUpdate = profileRepository.updateDisplayName("uid_test", "12345678901234567890123456")
        assertTrue(tooLongUpdate is AppResult.Error)

        val validUpdate = profileRepository.updateDisplayName("uid_test", "ValidName")
        assertTrue(validUpdate is AppResult.Success)

        val updated = (profileRepository.getProfile("uid_test") as AppResult.Success).data
        assertEquals("ValidName", updated?.displayName)
    }

    @Test
    fun `guest profile created with GUEST accountType and 0 completed matches`() = runTest {
        val result = profileRepository.getOrCreateProfile(
            uid = "guest_uid_1",
            defaultDisplayName = "Guest_1234",
            defaultAvatarId = AvatarId.CAT,
            accountType = com.pratyush.scribble.core.model.AccountType.GUEST
        )

        assertTrue(result is AppResult.Success)
        val profile = (result as AppResult.Success).data
        assertTrue(profile.isGuest)
        assertEquals(0, profile.guestMatchesCompleted)
        assertEquals(2, profile.remainingGuestMatches)
        org.junit.Assert.assertFalse(profile.isGuestLimitReached)

        val eligibility = profileRepository.checkGuestMatchEligibility("guest_uid_1")
        assertTrue(eligibility is AppResult.Success)
        assertTrue((eligibility as AppResult.Success).data)
    }

    @Test
    fun `guest profile reaching 2 completed matches becomes exhausted and ineligible`() = runTest {
        val exhaustedProfile = UserProfile(
            id = "guest_exhausted",
            displayName = "Guest_9999",
            accountType = com.pratyush.scribble.core.model.AccountType.GUEST,
            guestMatchesCompleted = 2,
            guestAccessStatus = "EXHAUSTED"
        )
        profileRepository.seedProfile(exhaustedProfile)

        val profile = (profileRepository.getProfile("guest_exhausted") as AppResult.Success).data
        assertNotNull(profile)
        assertTrue(profile!!.isGuest)
        assertTrue(profile.isGuestLimitReached)
        assertEquals(0, profile.remainingGuestMatches)

        val eligibility = profileRepository.checkGuestMatchEligibility("guest_exhausted")
        assertTrue(eligibility is AppResult.Success)
        org.junit.Assert.assertFalse((eligibility as AppResult.Success).data)
    }

    @Test
    fun `upgradeGuestToPermanent sets accountType to PERMANENT and restores eligibility`() = runTest {
        val exhaustedProfile = UserProfile(
            id = "guest_to_upgrade",
            displayName = "Guest_Player",
            accountType = com.pratyush.scribble.core.model.AccountType.GUEST,
            guestMatchesCompleted = 2,
            guestAccessStatus = "EXHAUSTED"
        )
        profileRepository.seedProfile(exhaustedProfile)

        val upgradeResult = profileRepository.upgradeGuestToPermanent("guest_to_upgrade", "user@gmail.com")
        assertTrue(upgradeResult is AppResult.Success)

        val upgraded = (profileRepository.getProfile("guest_to_upgrade") as AppResult.Success).data
        assertNotNull(upgraded)
        org.junit.Assert.assertFalse(upgraded!!.isGuest)
        org.junit.Assert.assertFalse(upgraded.isGuestLimitReached)
        assertEquals("user@gmail.com", upgraded.email)

        val eligibility = profileRepository.checkGuestMatchEligibility("guest_to_upgrade")
        assertTrue(eligibility is AppResult.Success)
        assertTrue((eligibility as AppResult.Success).data)
    }

    @Test
    fun `runFirestoreOperation rethrows CancellationException without logging error`() {
        var loggedMessage: String? = null
        var caughtCancellation: kotlinx.coroutines.CancellationException? = null

        try {
            FirestoreProfileRepository.runFirestoreOperation<UserProfile>(
                errorLogMessage = "Error in getOrCreateProfile for uid_123",
                defaultErrorMessage = "Failed to get or create profile",
                onErrorLog = { msg, _ -> loggedMessage = msg }
            ) {
                throw kotlinx.coroutines.CancellationException("Job was cancelled")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            caughtCancellation = e
        }

        assertNotNull(caughtCancellation)
        assertEquals("Job was cancelled", caughtCancellation?.message)
        org.junit.Assert.assertNull(loggedMessage)
    }

    @Test
    fun `runFirestoreOperation logs and returns AppResult Error for non-cancellation exceptions`() {
        var loggedMessage: String? = null

        val result = FirestoreProfileRepository.runFirestoreOperation<UserProfile>(
            errorLogMessage = "Error in getOrCreateProfile for uid_123",
            defaultErrorMessage = "Failed to get or create profile",
            onErrorLog = { msg, _ -> loggedMessage = msg }
        ) {
            throw IllegalStateException("Firestore unavailable")
        }

        assertTrue(result is AppResult.Error)
        assertEquals("Firestore unavailable", (result as AppResult.Error).message)
        assertEquals("Error in getOrCreateProfile for uid_123", loggedMessage)
    }

    private class InMemoryProfileRepository : ProfileRepository {
        private val profiles = mutableMapOf<String, MutableStateFlow<UserProfile?>>()
        private val matches = mutableListOf<MatchRecord>()

        fun seedProfile(profile: UserProfile) {
            getFlow(profile.id).value = profile
        }

        private fun getFlow(uid: String): MutableStateFlow<UserProfile?> {
            return profiles.getOrPut(uid) { MutableStateFlow(null) }
        }

        override suspend fun getProfile(uid: String): AppResult<UserProfile?> {
            return AppResult.Success(getFlow(uid).value)
        }

        override suspend fun getOrCreateProfile(
            uid: String,
            defaultDisplayName: String,
            defaultAvatarId: AvatarId,
            email: String?,
            accountType: com.pratyush.scribble.core.model.AccountType
        ): AppResult<UserProfile> {
            val existing = getFlow(uid).value
            if (existing != null) {
                return AppResult.Success(existing)
            }

            val created = UserProfile(
                id = uid,
                displayName = defaultDisplayName,
                email = email,
                avatarId = defaultAvatarId,
                accountType = accountType,
                stats = UserStats(),
                preferences = UserPreferences()
            )
            getFlow(uid).value = created
            return AppResult.Success(created)
        }

        override fun observeProfile(uid: String): Flow<UserProfile?> = getFlow(uid).asStateFlow()

        override suspend fun updateDisplayName(uid: String, displayName: String): AppResult<Unit> {
            val trimmed = displayName.trim()
            if (trimmed.isEmpty() || trimmed.length > 25) {
                return AppResult.Error("Display name must be between 1 and 25 characters")
            }
            val cur = getFlow(uid).value ?: return AppResult.Error("User not found")
            getFlow(uid).value = cur.copy(displayName = trimmed)
            return AppResult.Success(Unit)
        }

        override suspend fun updateAvatar(uid: String, avatarId: AvatarId): AppResult<Unit> {
            val cur = getFlow(uid).value ?: return AppResult.Error("User not found")
            getFlow(uid).value = cur.copy(avatarId = avatarId)
            return AppResult.Success(Unit)
        }

        override suspend fun updateProfile(
            uid: String,
            displayName: String,
            avatarId: AvatarId
        ): AppResult<Unit> {
            val trimmed = displayName.trim()
            if (trimmed.isEmpty() || trimmed.length > 25) {
                return AppResult.Error("Display name must be between 1 and 25 characters")
            }
            val cur = getFlow(uid).value ?: return AppResult.Error("User not found")
            getFlow(uid).value = cur.copy(displayName = trimmed, avatarId = avatarId)
            return AppResult.Success(Unit)
        }

        override suspend fun updateLastSeen(uid: String): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun getMatchHistory(uid: String): AppResult<List<MatchRecord>> {
            return AppResult.Success(matches.filter { it.playerUids.contains(uid) })
        }

        override suspend fun upgradeGuestToPermanent(uid: String, email: String?): AppResult<Unit> {
            val cur = getFlow(uid).value ?: return AppResult.Error("User not found")
            getFlow(uid).value = cur.copy(
                accountType = com.pratyush.scribble.core.model.AccountType.PERMANENT,
                guestAccessStatus = "PERMANENT",
                email = email ?: cur.email
            )
            return AppResult.Success(Unit)
        }

        override suspend fun checkGuestMatchEligibility(uid: String): AppResult<Boolean> {
            val cur = getFlow(uid).value ?: return AppResult.Success(true)
            return AppResult.Success(!cur.isGuestLimitReached)
        }
    }
}
