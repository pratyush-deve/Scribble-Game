package com.pratyush.scribble.feature.profile

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AccountType
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.MatchRecord
import com.pratyush.scribble.core.model.UserProfile
import com.pratyush.scribble.core.model.UserStats
import com.pratyush.scribble.core.network.repository.ProfileRepository
import com.pratyush.scribble.core.network.repository.RoomRepository
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository
import com.pratyush.scribble.feature.profile.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var fakeProfileRepo: FakeProfileRepository
    private lateinit var fakeRoomRepo: FakeRoomRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepo = FakeAuthRepository()
        fakeProfileRepo = FakeProfileRepository()
        fakeRoomRepo = FakeRoomRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads profile and displays stats from repository`() = runTest {
        val initialUser = UserProfile(
            id = "user_1",
            displayName = "DoodleMaster",
            avatarId = AvatarId.FOX,
            stats = UserStats(
                gamesPlayed = 20,
                wins = 12,
                losses = 6,
                draws = 2,
                totalScore = 6500,
                bestScore = 800,
                winStreak = 4
            )
        )
        fakeAuthRepo.setUser(initialUser)
        fakeProfileRepo.saveProfile(initialUser)

        val viewModel = ProfileViewModel(
            authRepository = fakeAuthRepo,
            profileRepository = fakeProfileRepo,
            roomRepository = fakeRoomRepo
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("DoodleMaster", state.editedDisplayName)
        assertEquals(AvatarId.FOX, state.selectedAvatar)
        assertEquals(20, state.userProfile.stats.gamesPlayed)
        assertEquals(12, state.userProfile.stats.wins)
        assertEquals(60.0f, state.userProfile.stats.winRate, 0.1f)
        assertEquals(4, state.userProfile.stats.winStreak)
    }

    @Test
    fun `blank display name sets validation error`() = runTest {
        val initialUser = UserProfile(id = "user_1", displayName = "Player1", avatarId = AvatarId.CAT)
        fakeAuthRepo.setUser(initialUser)
        fakeProfileRepo.saveProfile(initialUser)

        val viewModel = ProfileViewModel(
            authRepository = fakeAuthRepo,
            profileRepository = fakeProfileRepo,
            roomRepository = fakeRoomRepo
        )
        advanceUntilIdle()

        viewModel.onDisplayNameChanged("   ")
        viewModel.onSaveProfile()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals("Display name cannot be blank", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `display name longer than 25 chars sets validation error`() = runTest {
        val initialUser = UserProfile(id = "user_1", displayName = "Player1", avatarId = AvatarId.CAT)
        fakeAuthRepo.setUser(initialUser)
        fakeProfileRepo.saveProfile(initialUser)

        val viewModel = ProfileViewModel(
            authRepository = fakeAuthRepo,
            profileRepository = fakeProfileRepo,
            roomRepository = fakeRoomRepo
        )
        advanceUntilIdle()

        viewModel.onDisplayNameChanged("ThisNameIsWayTooLongToBeAllowed123456")
        viewModel.onSaveProfile()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals("Display name must be 25 characters or less", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `valid changes persist to profileRepository and active room`() = runTest {
        val initialUser = UserProfile(id = "user_1", displayName = "OldName", avatarId = AvatarId.CAT)
        fakeAuthRepo.setUser(initialUser)
        fakeProfileRepo.saveProfile(initialUser)

        val viewModel = ProfileViewModel(
            authRepository = fakeAuthRepo,
            profileRepository = fakeProfileRepo,
            roomRepository = fakeRoomRepo,
            activeRoomId = "room_123"
        )
        advanceUntilIdle()

        viewModel.onDisplayNameChanged("NewHero")
        viewModel.onAvatarSelected(AvatarId.LION)

        var savedCallbackCalled = false
        viewModel.onSaveProfile {
            savedCallbackCalled = true
        }
        advanceUntilIdle()

        assertTrue(savedCallbackCalled)
        assertTrue(viewModel.uiState.value.isSaved)
        assertNull(viewModel.uiState.value.errorMessage)

        // Verify profile repository received updates
        val updatedProfile = fakeProfileRepo.getProfile("user_1")
        assertTrue(updatedProfile is AppResult.Success)
        val data = (updatedProfile as AppResult.Success).data
        assertEquals("NewHero", data?.displayName)
        assertEquals(AvatarId.LION, data?.avatarId)

        // Verify active room received updates
        assertEquals("NewHero", fakeRoomRepo.lastUpdatedPlayerName)
        assertEquals(AvatarId.LION, fakeRoomRepo.lastUpdatedPlayerAvatar)
    }

    @Test
    fun `invalid avatar ID falls back safely to default CAT avatar`() {
        val fallback = AvatarId.fromId("non_existent_avatar_xyz")
        assertEquals(AvatarId.CAT, fallback)

        val valid = AvatarId.fromId("fox")
        assertEquals(AvatarId.FOX, valid)
    }

    private class FakeAuthRepository : AuthRepository {
        private val _currentUser = MutableStateFlow<UserProfile?>(null)
        override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

        fun setUser(user: UserProfile?) {
            _currentUser.value = user
        }

        override suspend fun signInWithGoogle(): AppResult<UserProfile> = AppResult.Error("Not implemented")
        override suspend fun signInAsGuest(displayName: String): AppResult<UserProfile> = AppResult.Error("Not implemented")
        override suspend fun signOut(): AppResult<Unit> {
            _currentUser.value = null
            return AppResult.Success(Unit)
        }
        override suspend fun getIdToken(forceRefresh: Boolean): AppResult<String> = AppResult.Success("token")
    }

    private class FakeProfileRepository : ProfileRepository {
        private val profiles = mutableMapOf<String, MutableStateFlow<UserProfile?>>()

        fun saveProfile(profile: UserProfile) {
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
            accountType: AccountType
        ): AppResult<UserProfile> {
            val existing = getFlow(uid).value
            if (existing != null) return AppResult.Success(existing)

            val created = UserProfile(
                id = uid,
                displayName = defaultDisplayName,
                avatarId = defaultAvatarId,
                email = email,
                accountType = accountType
            )
            getFlow(uid).value = created
            return AppResult.Success(created)
        }

        override suspend fun upgradeGuestToPermanent(uid: String, email: String?): AppResult<Unit> {
            val cur = getFlow(uid).value ?: return AppResult.Error("Not found")
            getFlow(uid).value = cur.copy(accountType = AccountType.PERMANENT, email = email ?: cur.email)
            return AppResult.Success(Unit)
        }

        override suspend fun checkGuestMatchEligibility(uid: String): AppResult<Boolean> {
            val cur = getFlow(uid).value ?: return AppResult.Success(false)
            if (cur.accountType == AccountType.PERMANENT) return AppResult.Success(true)
            return AppResult.Success(!cur.isGuestLimitReached)
        }

        override fun observeProfile(uid: String): Flow<UserProfile?> = getFlow(uid).asStateFlow()

        override suspend fun updateDisplayName(uid: String, displayName: String): AppResult<Unit> {
            val cur = getFlow(uid).value ?: return AppResult.Error("Not found")
            getFlow(uid).value = cur.copy(displayName = displayName)
            return AppResult.Success(Unit)
        }

        override suspend fun updateAvatar(uid: String, avatarId: AvatarId): AppResult<Unit> {
            val cur = getFlow(uid).value ?: return AppResult.Error("Not found")
            getFlow(uid).value = cur.copy(avatarId = avatarId)
            return AppResult.Success(Unit)
        }

        override suspend fun updateProfile(
            uid: String,
            displayName: String,
            avatarId: AvatarId
        ): AppResult<Unit> {
            val cur = getFlow(uid).value ?: return AppResult.Error("Not found")
            getFlow(uid).value = cur.copy(displayName = displayName, avatarId = avatarId)
            return AppResult.Success(Unit)
        }

        override suspend fun updateLastSeen(uid: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getMatchHistory(uid: String): AppResult<List<MatchRecord>> = AppResult.Success(emptyList())
    }

    private class FakeRoomRepository : RoomRepository {
        var lastUpdatedPlayerName: String? = null
        var lastUpdatedPlayerAvatar: AvatarId? = null

        override suspend fun createRoom(host: com.pratyush.scribble.core.model.Player, settings: com.pratyush.scribble.core.model.RoomSettings): AppResult<com.pratyush.scribble.core.model.Room> = AppResult.Error("Not implemented")
        override suspend fun joinRoom(roomCode: String, player: com.pratyush.scribble.core.model.Player): AppResult<com.pratyush.scribble.core.model.Room> = AppResult.Error("Not implemented")
        override suspend fun resolveRoomId(roomCode: String): String? = null
        override fun observeRoom(roomId: String): Flow<com.pratyush.scribble.core.model.Room?> = kotlinx.coroutines.flow.emptyFlow()
        override suspend fun leaveRoom(roomId: String, playerId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateSettings(roomId: String, requesterId: String, settings: com.pratyush.scribble.core.model.RoomSettings): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updatePlayerAvatar(roomId: String, playerId: String, avatarId: AvatarId): AppResult<Unit> {
            lastUpdatedPlayerAvatar = avatarId
            return AppResult.Success(Unit)
        }
        override suspend fun updatePlayerName(roomId: String, playerId: String, name: String): AppResult<Unit> {
            lastUpdatedPlayerName = name
            return AppResult.Success(Unit)
        }
        override suspend fun updatePlayerProfile(roomId: String, playerId: String, name: String, avatarId: AvatarId): AppResult<Unit> {
            lastUpdatedPlayerName = name
            lastUpdatedPlayerAvatar = avatarId
            return AppResult.Success(Unit)
        }
        override suspend fun startGame(roomId: String, requesterId: String): AppResult<Unit> = AppResult.Success(Unit)
        override fun registerPresence(roomId: String, playerId: String) = Unit
        override suspend fun closeAndCleanupRoom(roomId: String, roomCode: String): AppResult<Unit> = AppResult.Success(Unit)
    }
}
