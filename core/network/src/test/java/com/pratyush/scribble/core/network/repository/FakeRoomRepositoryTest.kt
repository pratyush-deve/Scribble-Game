package com.pratyush.scribble.core.network.repository

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.GamePhase
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.Room
import com.pratyush.scribble.core.model.RoomCode
import com.pratyush.scribble.core.model.RoomSettings
import com.pratyush.scribble.core.network.util.RoomCodeGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class FakeRoomRepositoryTest {

    private lateinit var fakeRepository: FakeTestRoomRepository

    @Before
    fun setup() {
        fakeRepository = FakeTestRoomRepository()
    }

    @Test
    fun `authenticated host creates room successfully and is registered as host`() = kotlinx.coroutines.runBlocking {
        val host = Player("host_1", "HostPlayer", AvatarId.CAT, isHost = true)
        val result = fakeRepository.createRoom(host)

        assertTrue(result is AppResult.Success)
        val room = (result as AppResult.Success).data
        assertEquals("host_1", room.hostId)
        assertEquals(1, room.playerCount)
        assertTrue(room.players.first().isHost)
        assertEquals(GamePhase.WAITING_FOR_PLAYERS, room.phase)
    }

    @Test
    fun `player can join an existing waiting room`() = kotlinx.coroutines.runBlocking {
        val host = Player("host_1", "HostPlayer", AvatarId.CAT, isHost = true)
        val createResult = fakeRepository.createRoom(host)
        val code = (createResult as AppResult.Success).data.code.value

        val guest = Player("guest_2", "GuestPlayer", AvatarId.DOG, isHost = false)
        val joinResult = fakeRepository.joinRoom(code, guest)

        assertTrue(joinResult is AppResult.Success)
        val room = (joinResult as AppResult.Success).data
        assertEquals(2, room.playerCount)
        assertTrue(room.players.any { it.id == "guest_2" })
    }

    @Test
    fun `joining nonexistent room returns error`() = kotlinx.coroutines.runBlocking {
        val guest = Player("guest_2", "GuestPlayer", AvatarId.DOG)
        val result = fakeRepository.joinRoom("XYZ999", guest)

        assertTrue(result is AppResult.Error)
        assertEquals("Room does not exist or has expired", (result as AppResult.Error).message)
    }

    @Test
    fun `joining full room returns room full error`() = kotlinx.coroutines.runBlocking {
        val host = Player("host_1", "HostPlayer", AvatarId.CAT, isHost = true)
        val createResult = fakeRepository.createRoom(host, RoomSettings(maxPlayers = 2))
        val code = (createResult as AppResult.Success).data.code.value

        val guest1 = Player("guest_1", "Guest1", AvatarId.DOG)
        val guest2 = Player("guest_2", "Guest2", AvatarId.FOX)

        fakeRepository.joinRoom(code, guest1)
        val fullResult = fakeRepository.joinRoom(code, guest2)

        assertTrue(fullResult is AppResult.Error)
        assertTrue((fullResult as AppResult.Error).message.contains("full"))
    }

    @Test
    fun `prevent duplicate membership adds player only once`() = kotlinx.coroutines.runBlocking {
        val host = Player("host_1", "HostPlayer", AvatarId.CAT, isHost = true)
        val createResult = fakeRepository.createRoom(host)
        val code = (createResult as AppResult.Success).data.code.value

        val guest = Player("guest_1", "Guest1", AvatarId.DOG)
        fakeRepository.joinRoom(code, guest)
        val reJoinResult = fakeRepository.joinRoom(code, guest)

        assertTrue(reJoinResult is AppResult.Success)
        val room = (reJoinResult as AppResult.Success).data
        assertEquals(2, room.playerCount)
    }

    @Test
    fun `host departure in waiting room closes room and notifies observers with null`() = kotlinx.coroutines.runBlocking {
        val host = Player("host_1", "HostPlayer", AvatarId.CAT, isHost = true)
        val createResult = fakeRepository.createRoom(host)
        val room = (createResult as AppResult.Success).data

        val guest = Player("guest_2", "GuestPlayer", AvatarId.DOG, isHost = false)
        fakeRepository.joinRoom(room.code.value, guest)

        fakeRepository.leaveRoom(room.id, "host_1")

        val updatedRoom = fakeRepository.getRoom(room.id)
        assertTrue(updatedRoom == null)
        val resolved = fakeRepository.resolveRoomId(room.code.value)
        assertTrue(resolved == null)
    }

    @Test
    fun `guest departure removes guest from room and leaves host active`() = kotlinx.coroutines.runBlocking {
        val host = Player("host_1", "HostPlayer", AvatarId.CAT, isHost = true)
        val createResult = fakeRepository.createRoom(host)
        val room = (createResult as AppResult.Success).data

        val guest = Player("guest_2", "GuestPlayer", AvatarId.DOG, isHost = false)
        fakeRepository.joinRoom(room.code.value, guest)

        val leaveResult = fakeRepository.leaveRoom(room.id, "guest_2")
        assertTrue(leaveResult is AppResult.Success)

        val updatedRoom = fakeRepository.getRoom(room.id)
        assertNotNull(updatedRoom)
        assertEquals(1, updatedRoom?.playerCount)
        assertEquals("host_1", updatedRoom?.players?.first()?.id)
    }

    @Test
    fun `resolveRoomId returns roomId for existing code and null for invalid or missing code`() = kotlinx.coroutines.runBlocking {
        val host = Player("host_1", "HostPlayer", AvatarId.CAT, isHost = true)
        val createResult = fakeRepository.createRoom(host)
        val room = (createResult as AppResult.Success).data

        val resolved = fakeRepository.resolveRoomId(room.code.value)
        assertEquals(room.id, resolved)

        val missing = fakeRepository.resolveRoomId("ZZZZZZ")
        assertTrue(missing == null)
    }

    @Test
    fun `only host can change room settings`() = kotlinx.coroutines.runBlocking {
        val host = Player("host_1", "HostPlayer", AvatarId.CAT, isHost = true)
        val createResult = fakeRepository.createRoom(host)
        val room = (createResult as AppResult.Success).data

        val unauthorizedResult = fakeRepository.updateSettings(room.id, "unauthorized_player", RoomSettings(rounds = 4))
        assertTrue(unauthorizedResult is AppResult.Error)

        val hostResult = fakeRepository.updateSettings(room.id, "host_1", RoomSettings(rounds = 4))
        assertTrue(hostResult is AppResult.Success)
    }

    @Test
    fun `start game requires at least two players`() = kotlinx.coroutines.runBlocking {
        val host = Player("host_1", "HostPlayer", AvatarId.CAT, isHost = true)
        val createResult = fakeRepository.createRoom(host)
        val room = (createResult as AppResult.Success).data

        val earlyStart = fakeRepository.startGame(room.id, "host_1")
        assertTrue(earlyStart is AppResult.Error)

        fakeRepository.joinRoom(room.code.value, Player("p2", "Player2", AvatarId.FOX))
        val validStart = fakeRepository.startGame(room.id, "host_1")
        assertTrue(validStart is AppResult.Success)
    }

    class FakeTestRoomRepository : RoomRepository {
        private val rooms = ConcurrentHashMap<String, Room>()
        private val codeToRoomId = ConcurrentHashMap<String, String>()
        private val roomFlows = ConcurrentHashMap<String, MutableStateFlow<Room?>>()

        fun getRoom(roomId: String): Room? = rooms[roomId]

        override suspend fun createRoom(host: Player, settings: RoomSettings): AppResult<Room> {
            val code = RoomCodeGenerator.generateCode(6)
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
            val normalized = RoomCodeGenerator.normalize(roomCode)
            return codeToRoomId[normalized]
        }

        override suspend fun joinRoom(roomCode: String, player: Player): AppResult<Room> {
            val normalized = RoomCodeGenerator.normalize(roomCode)
            val roomId = codeToRoomId[normalized] ?: return AppResult.Error("Room does not exist or has expired")
            val room = rooms[roomId] ?: return AppResult.Error("Room not found")

            if (room.phase != GamePhase.WAITING_FOR_PLAYERS) {
                return AppResult.Error("Game already in progress")
            }

            val existingList = room.players.toMutableList()
            val existingIndex = existingList.indexOfFirst { it.id == player.id }

            if (existingIndex == -1 && existingList.size >= room.settings.maxPlayers) {
                return AppResult.Error("Room is full")
            }

            if (existingIndex != -1) {
                existingList[existingIndex] = existingList[existingIndex].copy(isConnected = true)
            } else {
                existingList.add(player.copy(isHost = false, isConnected = true))
            }

            val updated = room.copy(players = existingList)
            rooms[roomId] = updated
            roomFlows[roomId]?.value = updated
            return AppResult.Success(updated)
        }

        override fun observeRoom(roomId: String): Flow<Room?> {
            return roomFlows.getOrPut(roomId) { MutableStateFlow(rooms[roomId]) }.asStateFlow()
        }

        override suspend fun leaveRoom(roomId: String, playerId: String): AppResult<Unit> {
            val room = rooms[roomId] ?: return AppResult.Error("Room not found")

            // Host departure policy: close the waiting room
            if (room.hostId == playerId) {
                rooms.remove(roomId)
                codeToRoomId.remove(room.code.value)
                roomFlows[roomId]?.value = null
                return AppResult.Success(Unit)
            }

            // Guest departure
            val remaining = room.players.filter { it.id != playerId }.toMutableList()
            if (remaining.isEmpty()) {
                rooms.remove(roomId)
                codeToRoomId.remove(room.code.value)
                roomFlows[roomId]?.value = null
                return AppResult.Success(Unit)
            }

            val updated = room.copy(players = remaining)
            rooms[roomId] = updated
            roomFlows[roomId]?.value = updated
            return AppResult.Success(Unit)
        }

        override suspend fun updateSettings(
            roomId: String,
            requesterId: String,
            settings: RoomSettings
        ): AppResult<Unit> {
            val room = rooms[roomId] ?: return AppResult.Error("Room not found")
            if (room.hostId != requesterId) return AppResult.Error("Only host can change settings")

            val updated = room.copy(settings = settings)
            rooms[roomId] = updated
            roomFlows[roomId]?.value = updated
            return AppResult.Success(Unit)
        }

        override suspend fun updatePlayerAvatar(
            roomId: String,
            playerId: String,
            avatarId: AvatarId
        ): AppResult<Unit> {
            val room = rooms[roomId] ?: return AppResult.Error("Room not found")
            val updatedPlayers = room.players.map { if (it.id == playerId) it.copy(avatarId = avatarId) else it }
            val updated = room.copy(players = updatedPlayers)
            rooms[roomId] = updated
            roomFlows[roomId]?.value = updated
            return AppResult.Success(Unit)
        }

        override suspend fun startGame(roomId: String, requesterId: String): AppResult<Unit> {
            val room = rooms[roomId] ?: return AppResult.Error("Room not found")
            if (room.hostId != requesterId) return AppResult.Error("Only host can start game")
            if (room.players.size < 2) return AppResult.Error("At least 2 players required")

            val updated = room.copy(phase = GamePhase.DRAWING)
            rooms[roomId] = updated
            roomFlows[roomId]?.value = updated
            return AppResult.Success(Unit)
        }

        override fun registerPresence(roomId: String, playerId: String) = Unit

        override suspend fun closeAndCleanupRoom(roomId: String, roomCode: String): AppResult<Unit> {
            rooms.remove(roomId)
            codeToRoomId.remove(roomCode)
            roomFlows[roomId]?.value = null
            return AppResult.Success(Unit)
        }
    }
}
