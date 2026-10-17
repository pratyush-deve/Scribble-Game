package com.pratyush.scribble.feature.lobby.presentation.viewmodel

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.GamePhase
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.Room
import com.pratyush.scribble.core.model.RoomCode
import com.pratyush.scribble.core.model.RoomSettings
import com.pratyush.scribble.core.model.UserProfile
import com.pratyush.scribble.core.network.repository.RoomRepository
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository
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
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class LobbyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepo: FakeTestAuthRepository
    private lateinit var fakeRoomRepo: FakeTestLobbyRoomRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepo = FakeTestAuthRepository()
        fakeRoomRepo = FakeTestLobbyRoomRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `host enters lobby and observes initial room with 1 player`() = runTest {
        val hostUser = UserProfile(id = "host_1", displayName = "HostPlayer", avatarId = AvatarId.CAT)
        fakeAuthRepo.setUser(hostUser)

        val createResult = fakeRoomRepo.createRoom(
            Player(id = "host_1", name = "HostPlayer", avatarId = AvatarId.CAT, isHost = true)
        )
        val room = (createResult as AppResult.Success).data

        val viewModel = LobbyViewModel(
            initialRoomCode = room.code.value,
            authRepository = fakeAuthRepo,
            roomRepository = fakeRoomRepo
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isHost)
        assertNotNull(state.room)
        assertEquals(1, state.room?.playerCount)
        assertEquals("HostPlayer", state.room?.players?.first()?.name)
    }

    @Test
    fun `when guest joins, host receives update with 2 players`() = runTest {
        val hostUser = UserProfile(id = "host_1", displayName = "HostPlayer", avatarId = AvatarId.CAT)
        fakeAuthRepo.setUser(hostUser)

        val createResult = fakeRoomRepo.createRoom(
            Player(id = "host_1", name = "HostPlayer", avatarId = AvatarId.CAT, isHost = true)
        )
        val room = (createResult as AppResult.Success).data

        val hostViewModel = LobbyViewModel(
            initialRoomCode = room.code.value,
            authRepository = fakeAuthRepo,
            roomRepository = fakeRoomRepo
        )
        advanceUntilIdle()

        // Guest joins the room
        val guest = Player(id = "guest_2", name = "GuestPlayer", avatarId = AvatarId.DOG, isHost = false)
        fakeRoomRepo.joinRoom(room.code.value, guest)
        advanceUntilIdle()

        val hostState = hostViewModel.uiState.value
        assertEquals(2, hostState.room?.playerCount)
        assertTrue(hostState.room?.players?.any { it.id == "guest_2" } == true)
    }

    @Test
    fun `guest observing room sees host and self`() = runTest {
        val hostUser = UserProfile(id = "host_1", displayName = "HostPlayer", avatarId = AvatarId.CAT)
        val createResult = fakeRoomRepo.createRoom(
            Player(id = "host_1", name = "HostPlayer", avatarId = AvatarId.CAT, isHost = true)
        )
        val room = (createResult as AppResult.Success).data

        val guestUser = UserProfile(id = "guest_2", displayName = "GuestPlayer", avatarId = AvatarId.DOG)
        fakeAuthRepo.setUser(guestUser)

        val guest = Player(id = "guest_2", name = "GuestPlayer", avatarId = AvatarId.DOG, isHost = false)
        fakeRoomRepo.joinRoom(room.code.value, guest)

        val guestViewModel = LobbyViewModel(
            initialRoomCode = room.code.value,
            authRepository = fakeAuthRepo,
            roomRepository = fakeRoomRepo
        )
        advanceUntilIdle()

        val guestState = guestViewModel.uiState.value
        assertFalse(guestState.isHost)
        assertEquals(2, guestState.room?.playerCount)
    }

    @Test
    fun `guest departure removes membership from host view`() = runTest {
        val hostUser = UserProfile(id = "host_1", displayName = "HostPlayer", avatarId = AvatarId.CAT)
        fakeAuthRepo.setUser(hostUser)

        val createResult = fakeRoomRepo.createRoom(
            Player(id = "host_1", name = "HostPlayer", avatarId = AvatarId.CAT, isHost = true)
        )
        val room = (createResult as AppResult.Success).data

        val hostViewModel = LobbyViewModel(
            initialRoomCode = room.code.value,
            authRepository = fakeAuthRepo,
            roomRepository = fakeRoomRepo
        )
        advanceUntilIdle()

        // Guest joins then leaves
        val guest = Player(id = "guest_2", name = "GuestPlayer", avatarId = AvatarId.DOG, isHost = false)
        fakeRoomRepo.joinRoom(room.code.value, guest)
        advanceUntilIdle()
        assertEquals(2, hostViewModel.uiState.value.room?.playerCount)

        fakeRoomRepo.leaveRoom(room.id, "guest_2")
        advanceUntilIdle()

        assertEquals(1, hostViewModel.uiState.value.room?.playerCount)
        assertEquals("host_1", hostViewModel.uiState.value.room?.players?.first()?.id)
    }

    @Test
    fun `host departure closes room and guest uiState receives isRoomClosedByHost`() = runTest {
        val host = Player(id = "host_1", name = "HostPlayer", avatarId = AvatarId.CAT, isHost = true)
        val createResult = fakeRoomRepo.createRoom(host)
        val room = (createResult as AppResult.Success).data

        val guestUser = UserProfile(id = "guest_2", displayName = "GuestPlayer", avatarId = AvatarId.DOG)
        fakeAuthRepo.setUser(guestUser)
        fakeRoomRepo.joinRoom(room.code.value, Player("guest_2", "GuestPlayer", AvatarId.DOG, isHost = false))

        val guestViewModel = LobbyViewModel(
            initialRoomCode = room.code.value,
            authRepository = fakeAuthRepo,
            roomRepository = fakeRoomRepo
        )
        advanceUntilIdle()
        assertEquals(2, guestViewModel.uiState.value.room?.playerCount)

        // Host leaves waiting room
        fakeRoomRepo.leaveRoom(room.id, "host_1")
        advanceUntilIdle()

        val state = guestViewModel.uiState.value
        assertTrue(state.isRoomClosedByHost)
        assertNull(state.room)
    }

    @Test
    fun `leaving lobby cancels observation and cleans local state`() = runTest {
        val hostUser = UserProfile(id = "host_1", displayName = "HostPlayer", avatarId = AvatarId.CAT)
        fakeAuthRepo.setUser(hostUser)

        val createResult = fakeRoomRepo.createRoom(
            Player(id = "host_1", name = "HostPlayer", avatarId = AvatarId.CAT, isHost = true)
        )
        val room = (createResult as AppResult.Success).data

        val hostViewModel = LobbyViewModel(
            initialRoomCode = room.code.value,
            authRepository = fakeAuthRepo,
            roomRepository = fakeRoomRepo
        )
        advanceUntilIdle()

        var onCompleteCalled = false
        hostViewModel.onLeaveRoom {
            onCompleteCalled = true
        }
        advanceUntilIdle()

        assertTrue(onCompleteCalled)
        assertNull(hostViewModel.uiState.value.room)
    }

    private class FakeTestAuthRepository : AuthRepository {
        private val _currentUser = MutableStateFlow<UserProfile?>(null)
        override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

        fun setUser(user: UserProfile?) {
            _currentUser.value = user
        }

        override suspend fun signInWithGoogle(): AppResult<UserProfile> = AppResult.Error("Not needed")
        override suspend fun signInAsGuest(displayName: String): AppResult<UserProfile> = AppResult.Error("Not needed")
        override suspend fun signOut(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getIdToken(forceRefresh: Boolean): AppResult<String> = AppResult.Success("fake_token")
    }

    private class FakeTestLobbyRoomRepository : RoomRepository {
        private val rooms = ConcurrentHashMap<String, Room>()
        private val codeToRoomId = ConcurrentHashMap<String, String>()
        private val roomFlows = ConcurrentHashMap<String, MutableStateFlow<Room?>>()

        override suspend fun createRoom(host: Player, settings: RoomSettings): AppResult<Room> {
            val code = "RM1234"
            val roomId = "room_$code"
            val room = Room(
                id = roomId,
                code = RoomCode(code),
                hostId = host.id,
                players = listOf(host.copy(isHost = true)),
                settings = settings,
                phase = GamePhase.WAITING_FOR_PLAYERS
            )
            rooms[roomId] = room
            codeToRoomId[code] = roomId
            roomFlows[roomId] = MutableStateFlow(room)
            return AppResult.Success(room)
        }

        override suspend fun resolveRoomId(roomCode: String): String? {
            return codeToRoomId[roomCode]
        }

        override suspend fun joinRoom(roomCode: String, player: Player): AppResult<Room> {
            val roomId = codeToRoomId[roomCode] ?: return AppResult.Error("Room does not exist or has expired")
            val room = rooms[roomId] ?: return AppResult.Error("ROOM_NOT_FOUND")

            val currentPlayers = room.players.toMutableList()
            if (!currentPlayers.any { it.id == player.id }) {
                currentPlayers.add(player.copy(isHost = false))
            }
            val updated = room.copy(players = currentPlayers)
            rooms[roomId] = updated
            roomFlows[roomId]?.value = updated
            return AppResult.Success(updated)
        }

        override fun observeRoom(roomId: String): Flow<Room?> {
            return roomFlows.getOrPut(roomId) { MutableStateFlow(rooms[roomId]) }.asStateFlow()
        }

        override suspend fun leaveRoom(roomId: String, playerId: String): AppResult<Unit> {
            val room = rooms[roomId] ?: return AppResult.Error("Room not found")
            if (room.hostId == playerId) {
                // Host closes waiting room
                rooms.remove(roomId)
                codeToRoomId.remove(room.code.value)
                roomFlows[roomId]?.value = null
                return AppResult.Success(Unit)
            }

            val remaining = room.players.filter { it.id != playerId }
            val updated = room.copy(players = remaining)
            rooms[roomId] = updated
            roomFlows[roomId]?.value = updated
            return AppResult.Success(Unit)
        }

        override suspend fun updateSettings(roomId: String, requesterId: String, settings: RoomSettings): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updatePlayerAvatar(roomId: String, playerId: String, avatarId: AvatarId): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun startGame(roomId: String, requesterId: String): AppResult<Unit> = AppResult.Success(Unit)
        override fun registerPresence(roomId: String, playerId: String) = Unit
        override suspend fun closeAndCleanupRoom(roomId: String, roomCode: String): AppResult<Unit> {
            rooms.remove(roomId)
            codeToRoomId.remove(roomCode)
            roomFlows[roomId]?.value = null
            return AppResult.Success(Unit)
        }
    }
}
