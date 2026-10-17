package com.pratyush.scribble.feature.home.presentation.viewmodel

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AccountType
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.GamePhase
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.Room
import com.pratyush.scribble.core.model.RoomCode
import com.pratyush.scribble.core.model.RoomSettings
import com.pratyush.scribble.core.model.UserProfile
import com.pratyush.scribble.core.network.repository.RoomRepository
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository
import com.pratyush.scribble.feature.home.presentation.state.JoinRoomState
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepo: FakeHomeAuthRepository
    private lateinit var fakeRoomRepo: FakeHomeRoomRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepo = FakeHomeAuthRepository()
        fakeRoomRepo = FakeHomeRoomRepository()
        fakeAuthRepo.setUser(
            UserProfile(id = "user_1", displayName = "TestPlayer", avatarId = AvatarId.FOX)
        )
        viewModel = HomeViewModel(fakeAuthRepo, fakeRoomRepo)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `validateAndJoinRoom succeeds and sets Joined state`() = runTest {
        viewModel.onJoinRoomClicked()
        viewModel.onRoomCodeChanged("ABCD23")

        fakeRoomRepo.joinResult = AppResult.Success(
            Room(
                id = "room_1",
                code = RoomCode("ABCD23"),
                hostId = "host_1",
                players = listOf(Player("user_1", "TestPlayer", AvatarId.FOX))
            )
        )

        var joinedCode: String? = null
        viewModel.validateAndJoinRoom { code ->
            joinedCode = code
        }

        advanceUntilIdle()

        assertEquals("ABCD23", joinedCode)
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.joinRoomState is JoinRoomState.Joined)
        assertEquals("ABCD23", (state.joinRoomState as JoinRoomState.Joined).roomCode)
        assertFalse(state.isJoinRoomDialogOpen)
    }

    @Test
    fun `validateAndJoinRoom with missing room sets RoomNotFound state`() = runTest {
        viewModel.onJoinRoomClicked()
        viewModel.onRoomCodeChanged("MS9999")

        fakeRoomRepo.joinResult = AppResult.Error("ROOM_NOT_FOUND")

        var joinedCode: String? = null
        viewModel.validateAndJoinRoom { code -> joinedCode = code }

        advanceUntilIdle()

        assertTrue(joinedCode == null)
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.joinRoomState is JoinRoomState.RoomNotFound)
        assertEquals("Room not found. Check the code and try again.", state.errorMessage)
        assertEquals("MS9999", state.roomCodeInput) // Code remains visible
    }

    @Test
    fun `validateAndJoinRoom with closed room sets RoomClosed state`() = runTest {
        viewModel.onJoinRoomClicked()
        viewModel.onRoomCodeChanged("CL4SED")

        fakeRoomRepo.joinResult = AppResult.Error("ROOM_CLOSED")

        viewModel.validateAndJoinRoom {}
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.joinRoomState is JoinRoomState.RoomClosed)
        assertEquals("This room has ended or is already in progress.", state.errorMessage)
    }

    @Test
    fun `validateAndJoinRoom with full room sets RoomFull state`() = runTest {
        viewModel.onJoinRoomClicked()
        viewModel.onRoomCodeChanged("FULLRM")

        fakeRoomRepo.joinResult = AppResult.Error("ROOM_FULL")

        viewModel.validateAndJoinRoom {}
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.joinRoomState is JoinRoomState.RoomFull)
        assertEquals("Room is full. Please try another room.", state.errorMessage)
    }

    @Test
    fun `createHostRoom sets loading and returns code`() = runTest {
        viewModel.onHostRoomClicked()

        fakeRoomRepo.createResult = AppResult.Success(
            Room(
                id = "room_host",
                code = RoomCode("HOST99"),
                hostId = "user_1",
                players = listOf(Player("user_1", "TestPlayer", AvatarId.FOX, isHost = true))
            )
        )

        var createdCode: String? = null
        viewModel.createHostRoom { code ->
            createdCode = code
        }
        advanceUntilIdle()

        assertEquals("HOST99", createdCode)
        assertFalse(viewModel.uiState.value.isHostRoomDialogOpen)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onHowToPlayClicked opens dialog and onDismiss closes dialog`() = runTest {
        assertFalse(viewModel.uiState.value.isHowToPlayDialogOpen)
        viewModel.onHowToPlayClicked()
        assertTrue(viewModel.uiState.value.isHowToPlayDialogOpen)

        viewModel.onDismissHowToPlayDialog()
        assertFalse(viewModel.uiState.value.isHowToPlayDialogOpen)

        viewModel.onHowToPlayClicked()
        assertTrue(viewModel.uiState.value.isHowToPlayDialogOpen)
        viewModel.onDismissDialogs()
        assertFalse(viewModel.uiState.value.isHowToPlayDialogOpen)
    }

    @Test
    fun `guest with limit reached opens guest limit dialog on host or join`() = runTest {
        val exhaustedGuest = UserProfile(
            id = "guest_1",
            displayName = "GuestPlayer",
            avatarId = AvatarId.CAT,
            accountType = AccountType.GUEST,
            guestMatchesCompleted = 2,
            guestAccessStatus = "EXHAUSTED"
        )
        fakeAuthRepo.setUser(exhaustedGuest)
        advanceUntilIdle()

        viewModel.onHostRoomClicked()
        assertTrue(viewModel.uiState.value.isGuestLimitDialogOpen)
        assertFalse(viewModel.uiState.value.isHostRoomDialogOpen)

        viewModel.onDismissGuestLimitDialog()
        assertFalse(viewModel.uiState.value.isGuestLimitDialogOpen)

        viewModel.onJoinRoomClicked()
        assertTrue(viewModel.uiState.value.isGuestLimitDialogOpen)
        assertFalse(viewModel.uiState.value.isJoinRoomDialogOpen)
    }

    @Test
    fun `upgrade to Google upgrades user profile and closes dialog`() = runTest {
        val guest = UserProfile(
            id = "guest_2",
            displayName = "GuestPlayer2",
            avatarId = AvatarId.DOG,
            accountType = AccountType.GUEST,
            guestMatchesCompleted = 2,
            guestAccessStatus = "EXHAUSTED"
        )
        fakeAuthRepo.setUser(guest)
        advanceUntilIdle()

        viewModel.onHostRoomClicked()
        assertTrue(viewModel.uiState.value.isGuestLimitDialogOpen)

        var upgraded = false
        viewModel.onUpgradeToGoogleClicked {
            upgraded = true
        }
        advanceUntilIdle()

        assertTrue(upgraded)
        assertFalse(viewModel.uiState.value.isGuestLimitDialogOpen)
        assertEquals(AccountType.PERMANENT, viewModel.uiState.value.userProfile?.accountType)
    }

    private class FakeHomeAuthRepository : AuthRepository {
        private val _currentUser = MutableStateFlow<UserProfile?>(null)
        override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

        fun setUser(user: UserProfile?) {
            _currentUser.value = user
        }

        override suspend fun signInWithGoogle(): AppResult<UserProfile> = AppResult.Error("Not needed")
        override suspend fun signInAsGuest(displayName: String): AppResult<UserProfile> = AppResult.Error("Not needed")
        override suspend fun signOut(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getIdToken(forceRefresh: Boolean): AppResult<String> = AppResult.Success("fake_token")
        override suspend fun linkGuestWithGoogle(): AppResult<UserProfile> {
            val user = _currentUser.value?.copy(accountType = AccountType.PERMANENT, email = "upgraded@gmail.com")
                ?: return AppResult.Error("No user")
            _currentUser.value = user
            return AppResult.Success(user)
        }
    }

    private class FakeHomeRoomRepository : RoomRepository {
        var joinResult: AppResult<Room> = AppResult.Error("Default")
        var createResult: AppResult<Room> = AppResult.Error("Default")

        override suspend fun createRoom(host: Player, settings: RoomSettings): AppResult<Room> = createResult
        override suspend fun joinRoom(roomCode: String, player: Player): AppResult<Room> = joinResult
        override suspend fun resolveRoomId(roomCode: String): String? = "room_$roomCode"
        override fun observeRoom(roomId: String): Flow<Room?> = MutableStateFlow(null)
        override suspend fun leaveRoom(roomId: String, playerId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateSettings(roomId: String, requesterId: String, settings: RoomSettings): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updatePlayerAvatar(roomId: String, playerId: String, avatarId: AvatarId): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun startGame(roomId: String, requesterId: String): AppResult<Unit> = AppResult.Success(Unit)
        override fun registerPresence(roomId: String, playerId: String) = Unit
        override suspend fun closeAndCleanupRoom(roomId: String, roomCode: String): AppResult<Unit> = AppResult.Success(Unit)
    }
}
