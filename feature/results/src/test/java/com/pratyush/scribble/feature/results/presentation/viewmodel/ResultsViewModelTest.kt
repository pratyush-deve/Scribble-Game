package com.pratyush.scribble.feature.results.presentation.viewmodel

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.ChatMessage
import com.pratyush.scribble.core.model.DrawingStroke
import com.pratyush.scribble.core.model.DrawerSecretState
import com.pratyush.scribble.core.model.GameStateSnapshot
import com.pratyush.scribble.core.model.GuessResult
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.ReplayVotingState
import com.pratyush.scribble.core.model.Room
import com.pratyush.scribble.core.model.RoomCode
import com.pratyush.scribble.core.model.RoomSettings
import com.pratyush.scribble.core.model.UserProfile
import com.pratyush.scribble.core.model.WordDifficulty
import com.pratyush.scribble.core.network.repository.GameRepository
import com.pratyush.scribble.core.network.repository.RoomRepository
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRoomRepo: FakeResultsRoomRepository
    private lateinit var fakeGameRepo: FakeResultsGameRepository
    private lateinit var fakeAuthRepo: FakeResultsAuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRoomRepo = FakeResultsRoomRepository()
        fakeGameRepo = FakeResultsGameRepository()
        fakeAuthRepo = FakeResultsAuthRepository()
        fakeAuthRepo.setUser(
            UserProfile(id = "user_1", displayName = "Player1", avatarId = AvatarId.FOX)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `failed voting when timer expires triggers failed exit and navigates home after delay`() = runTest {
        val room = Room(
            id = "room_1",
            code = RoomCode("ROOM1"),
            hostId = "user_1",
            players = listOf(
                Player("user_1", "Player1", AvatarId.FOX, score = 100),
                Player("user_2", "Player2", AvatarId.CAT, score = 80)
            )
        )
        fakeRoomRepo.roomFlow.value = room
        // 2 players: requiredMajority is 2, only 1 vote
        fakeGameRepo.replayFlow.value = ReplayVotingState(
            status = "VOTING",
            votes = mapOf("user_1" to true),
            voteCount = 1,
            requiredMajority = 2
        )

        val viewModel = ResultsViewModel(
            roomCode = "ROOM1",
            roomRepository = fakeRoomRepo,
            gameRepository = fakeGameRepo,
            authRepository = fakeAuthRepo
        )

        advanceTimeBy(1000L) // Initial collect
        assertFalse(viewModel.uiState.value.isVoteFailed)
        assertFalse(viewModel.uiState.value.shouldNavigateHome)

        // Advance past voting countdown (15 seconds)
        advanceTimeBy(16000L)

        // Vote failed transition active
        assertTrue(viewModel.uiState.value.isVoteFailed)
        assertTrue(viewModel.uiState.value.isSessionClosed)
        assertEquals("Not enough players to play again!", viewModel.uiState.value.failureMessage)
        assertEquals("The session has ended. Taking you back home...", viewModel.uiState.value.failureSupportingText)
        assertFalse(viewModel.uiState.value.shouldNavigateHome)

        // After 2.5s transition delay, navigation to home is triggered
        advanceTimeBy(3000L)
        assertTrue(viewModel.uiState.value.shouldNavigateHome)
    }

    @Test
    fun `onReturnToHome triggers immediate home navigation`() = runTest {
        val viewModel = ResultsViewModel(
            roomCode = "ROOM1",
            roomRepository = fakeRoomRepo,
            gameRepository = fakeGameRepo,
            authRepository = fakeAuthRepo
        )
        advanceUntilIdle()

        viewModel.onReturnToHome()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSessionClosed)
        assertTrue(viewModel.uiState.value.shouldNavigateHome)
    }

    @Test
    fun `closed session in replayVoting triggers failed exit transition`() = runTest {
        val room = Room(
            id = "room_1",
            code = RoomCode("ROOM1"),
            hostId = "user_1",
            players = listOf(Player("user_1", "Player1", AvatarId.FOX, score = 100))
        )
        fakeRoomRepo.roomFlow.value = room
        val viewModel = ResultsViewModel(
            roomCode = "ROOM1",
            roomRepository = fakeRoomRepo,
            gameRepository = fakeGameRepo,
            authRepository = fakeAuthRepo
        )
        advanceTimeBy(1000L)

        // Remote backend sets status to CLOSED
        fakeGameRepo.replayFlow.value = ReplayVotingState(status = "CLOSED")
        advanceTimeBy(100L)

        assertTrue(viewModel.uiState.value.isVoteFailed)
        assertTrue(viewModel.uiState.value.isSessionClosed)

        advanceTimeBy(3000L)
        assertTrue(viewModel.uiState.value.shouldNavigateHome)
    }

    private class FakeResultsAuthRepository : AuthRepository {
        private val _currentUser = MutableStateFlow<UserProfile?>(null)
        override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()
        fun setUser(user: UserProfile?) { _currentUser.value = user }
        override suspend fun signInWithGoogle(): AppResult<UserProfile> = AppResult.Error("")
        override suspend fun signInAsGuest(displayName: String): AppResult<UserProfile> = AppResult.Error("")
        override suspend fun signOut(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getIdToken(forceRefresh: Boolean): AppResult<String> = AppResult.Success("tok")
    }

    private class FakeResultsRoomRepository : RoomRepository {
        val roomFlow = MutableStateFlow<Room?>(null)
        override suspend fun createRoom(host: Player, settings: RoomSettings): AppResult<Room> = AppResult.Error("")
        override suspend fun joinRoom(roomCode: String, player: Player): AppResult<Room> = AppResult.Error("")
        override suspend fun resolveRoomId(roomCode: String): String? = "room_1"
        override fun observeRoom(roomId: String): Flow<Room?> = roomFlow
        override suspend fun leaveRoom(roomId: String, playerId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateSettings(roomId: String, requesterId: String, settings: RoomSettings): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updatePlayerAvatar(roomId: String, playerId: String, avatarId: AvatarId): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun startGame(roomId: String, requesterId: String): AppResult<Unit> = AppResult.Success(Unit)
        override fun registerPresence(roomId: String, playerId: String) = Unit
        override suspend fun closeAndCleanupRoom(roomId: String, roomCode: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeResultsGameRepository : GameRepository {
        val replayFlow = MutableStateFlow<ReplayVotingState?>(null)
        val gameStateFlow = MutableStateFlow<GameStateSnapshot?>(null)
        override fun observeGameState(roomId: String): Flow<GameStateSnapshot?> = gameStateFlow
        override fun observeReplayVoting(roomId: String): Flow<ReplayVotingState?> = replayFlow
        override suspend fun submitPlayAgainVote(roomId: String, vote: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun finalizeReplaySession(roomId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun finalizeMatch(roomId: String): AppResult<Unit> = AppResult.Success(Unit)

        override fun observeDrawingStrokes(roomId: String, turnId: String): Flow<List<DrawingStroke>> = MutableStateFlow(emptyList())
        override fun observeChatMessages(roomId: String): Flow<List<ChatMessage>> = MutableStateFlow(emptyList())
        override fun observeDrawerSecret(roomId: String, playerId: String): Flow<DrawerSecretState?> = MutableStateFlow(null)

        override suspend fun sendStroke(roomId: String, turnId: String, stroke: DrawingStroke, seq: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteStrokes(roomId: String, turnId: String, strokeIds: List<String>): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun clearDrawing(roomId: String, turnId: String): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun sendChatMessage(roomId: String, message: ChatMessage): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun submitGuess(roomId: String, player: Player, guessText: String): AppResult<GuessResult> = AppResult.Success(GuessResult.wrong(player.id, player.name, guessText))

        override suspend fun initializeGame(roomId: String, players: List<Player>, totalRounds: Int, drawTimeSeconds: Int, difficulty: WordDifficulty): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun selectWord(roomId: String, drawerId: String, selectedWord: String, difficulty: WordDifficulty, drawTimeSeconds: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun startWordSelection(roomId: String, drawTimeSeconds: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun ensureDrawerSecret(roomId: String, turnId: String, drawerId: String, difficulty: WordDifficulty): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun endTurnAndAdvance(roomId: String, reason: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun handlePlayerDeparture(roomId: String, departingPlayerId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun cleanupGame(roomId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun votePlayAgain(roomId: String, player: Player, vote: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun restartGame(roomId: String, players: List<Player>, totalRounds: Int, drawTimeSeconds: Int, difficulty: WordDifficulty): AppResult<Unit> = AppResult.Success(Unit)
    }
}
