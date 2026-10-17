package com.pratyush.scribble.feature.game

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.DrawingPoint
import com.pratyush.scribble.core.model.DrawingStroke
import com.pratyush.scribble.core.model.DrawingTool
import com.pratyush.scribble.core.model.GamePhase
import com.pratyush.scribble.core.model.GameScoringEngine
import com.pratyush.scribble.core.model.GuessResult
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.StrokeGeometry
import com.pratyush.scribble.core.model.WordDifficulty
import com.pratyush.scribble.core.network.repository.FakeGameRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MultiplayerGameFlowTest {

    private lateinit var gameRepository: FakeGameRepository
    private val roomId = "test_room_1"
    private val p1 = Player("p1", "Alice", AvatarId.CAT, isHost = true, score = 0)
    private val p2 = Player("p2", "Bob", AvatarId.DOG, score = 0)
    private val p3 = Player("p3", "Charlie", AvatarId.FOX, score = 0)

    @Before
    fun setup() {
        gameRepository = FakeGameRepository()
    }

    @Test
    fun `game initialization offers 3 words strictly to drawer private secret`() = runBlocking {
        gameRepository.initializeGame(
            roomId = roomId,
            players = listOf(p1, p2, p3),
            totalRounds = 3,
            drawTimeSeconds = 60,
            difficulty = WordDifficulty.MEDIUM
        )

        val state = gameRepository.observeGameState(roomId).first()
        assertNotNull(state)
        assertEquals(GamePhase.COUNTDOWN, state?.phase)
        assertEquals("p1", state?.drawerId)
        assertEquals(1, state?.round)
        assertEquals(1, state?.turnNumber)

        // After countdown completes, transition into word selection
        gameRepository.startWordSelection(roomId, 60)
        val wordSelectState = gameRepository.observeGameState(roomId).first()
        assertEquals(GamePhase.SELECTING_WORD, wordSelectState?.phase)

        // Drawer (p1) receives secret choices
        val p1Secret = gameRepository.observeDrawerSecret(roomId, "p1").first()
        assertNotNull(p1Secret)
        assertEquals(3, p1Secret?.wordChoices?.size)

        // Non-drawers (p2, p3) have null secret
        val p2Secret = gameRepository.observeDrawerSecret(roomId, "p2").first()
        assertNull("Guesser must not have drawer secrets", p2Secret)

        val p3Secret = gameRepository.observeDrawerSecret(roomId, "p3").first()
        assertNull("Guesser must not have drawer secrets", p3Secret)
    }

    @Test
    fun `drawer selects word - game enters DRAWING phase with masked word hint`() = runBlocking {
        gameRepository.initializeGame(
            roomId = roomId,
            players = listOf(p1, p2, p3),
            totalRounds = 3,
            drawTimeSeconds = 60,
            difficulty = WordDifficulty.MEDIUM
        )

        gameRepository.selectWord(
            roomId = roomId,
            drawerId = "p1",
            selectedWord = "ELEPHANT",
            difficulty = WordDifficulty.MEDIUM,
            drawTimeSeconds = 60
        )

        val state = gameRepository.observeGameState(roomId).first()
        assertNotNull(state)
        assertEquals(GamePhase.DRAWING, state?.phase)
        assertEquals(8, state?.wordLength)
        // Public state contains masked hint and wordHash, never the plaintext secret
        assertNotNull(state?.maskedWord)
        assertTrue(state!!.maskedWord.contains("_"))
        assertFalse(state.maskedWord.contains("ELEPHANT"))
        assertNull(state.revealedWord)
    }

    @Test
    fun `guess evaluation awards tiered points and prevents duplicate scoring`() = runBlocking {
        gameRepository.initializeGame(
            roomId = roomId,
            players = listOf(p1, p2, p3),
            totalRounds = 3,
            drawTimeSeconds = 60,
            difficulty = WordDifficulty.MEDIUM
        )
        gameRepository.selectWord(roomId, "p1", "ELEPHANT", WordDifficulty.MEDIUM, 60)

        // Guesser 1 (Bob) submits correct guess -> first correct (100 pts)
        val bobResult = gameRepository.submitGuess(roomId, p2, "ELEPHANT")
        assertTrue(bobResult is AppResult.Success)
        val bobGuess = (bobResult as AppResult.Success).data
        assertTrue(bobGuess.isCorrect)
        assertEquals(100, bobGuess.pointsAwarded)
        assertEquals(1, bobGuess.guessOrder)
        assertEquals(100, gameRepository.getPlayerScore(roomId, "p2"))

        // Bob submits again -> AlreadyGuessed, no duplicate points
        val bobRepeat = gameRepository.submitGuess(roomId, p2, "ELEPHANT")
        assertTrue(bobRepeat is AppResult.Success)
        val repeatGuess = (bobRepeat as AppResult.Success).data
        assertFalse(repeatGuess.isCorrect)
        assertEquals(0, repeatGuess.pointsAwarded)
        assertEquals(100, gameRepository.getPlayerScore(roomId, "p2"))

        // Guesser 2 (Charlie) submits correct guess -> second correct (80 pts)
        val charlieResult = gameRepository.submitGuess(roomId, p3, "elephant")
        assertTrue(charlieResult is AppResult.Success)
        val charlieGuess = (charlieResult as AppResult.Success).data
        assertTrue(charlieGuess.isCorrect)
        assertEquals(80, charlieGuess.pointsAwarded)
        assertEquals(2, charlieGuess.guessOrder)
        assertEquals(80, gameRepository.getPlayerScore(roomId, "p3"))
    }

    @Test
    fun `drawer cannot submit guess for their own drawing`() = runBlocking {
        gameRepository.initializeGame(
            roomId = roomId,
            players = listOf(p1, p2, p3),
            totalRounds = 3,
            drawTimeSeconds = 60,
            difficulty = WordDifficulty.MEDIUM
        )
        gameRepository.selectWord(roomId, "p1", "ELEPHANT", WordDifficulty.MEDIUM, 60)

        val drawerGuess = gameRepository.submitGuess(roomId, p1, "ELEPHANT")
        assertTrue(drawerGuess is AppResult.Error)
        assertEquals("Drawer cannot guess", (drawerGuess as AppResult.Error).message)
    }

    @Test
    fun `end of turn awards drawer 50 points per correct guesser and advances drawer`() = runBlocking {
        gameRepository.initializeGame(
            roomId = roomId,
            players = listOf(p1, p2, p3),
            totalRounds = 3,
            drawTimeSeconds = 60,
            difficulty = WordDifficulty.MEDIUM
        )
        gameRepository.selectWord(roomId, "p1", "ELEPHANT", WordDifficulty.MEDIUM, 60)

        // 2 players guess correctly
        gameRepository.submitGuess(roomId, p2, "ELEPHANT")
        gameRepository.submitGuess(roomId, p3, "ELEPHANT")

        // Turn ends
        gameRepository.endTurnAndAdvance(roomId, "ALL_GUESSED")

        // Drawer (p1) gets 2 * 50 = 100 points
        assertEquals(100, gameRepository.getPlayerScore(roomId, "p1"))

        // Next turn starts with next drawer (p2)
        val nextState = gameRepository.observeGameState(roomId).first()
        assertEquals("p2", nextState?.drawerId)
        assertEquals(2, nextState?.turnNumber)
        assertEquals(GamePhase.SELECTING_WORD, nextState?.phase)

        // p2 now has secret choices
        val p2Secret = gameRepository.observeDrawerSecret(roomId, "p2").first()
        assertNotNull(p2Secret)
        assertEquals(3, p2Secret?.wordChoices?.size)
    }

    @Test
    fun `real-time drawing synchronization correctly commits, deletes and clears strokes`() = runBlocking {
        val stroke1 = DrawingStroke(
            id = "s_1",
            points = listOf(DrawingPoint(0.1f, 0.1f), DrawingPoint(0.2f, 0.2f)),
            colorArgb = 0xFF000000,
            strokeWidth = 5f,
            tool = DrawingTool.PEN
        )
        val stroke2 = DrawingStroke(
            id = "s_2",
            points = listOf(DrawingPoint(0.3f, 0.3f), DrawingPoint(0.4f, 0.4f)),
            colorArgb = 0xFFFF0000,
            strokeWidth = 5f,
            tool = DrawingTool.PEN
        )

        // Send strokes
        gameRepository.sendStroke(roomId, "turn_1", stroke1, 1L)
        gameRepository.sendStroke(roomId, "turn_1", stroke2, 2L)

        val strokes = gameRepository.observeDrawingStrokes(roomId, "turn_1").first()
        assertEquals(2, strokes.size)

        // Whole stroke deletion (eraser / undo)
        gameRepository.deleteStrokes(roomId, "turn_1", listOf("s_1"))
        val remainingStrokes = gameRepository.observeDrawingStrokes(roomId, "turn_1").first()
        assertEquals(1, remainingStrokes.size)
        assertEquals("s_2", remainingStrokes.first().id)

        // Clear canvas
        gameRepository.clearDrawing(roomId, "turn_1")
        val clearedStrokes = gameRepository.observeDrawingStrokes(roomId, "turn_1").first()
        assertEquals(0, clearedStrokes.size)
    }

    @Test
    fun `two-player game departure ends game with default win`() = runBlocking {
        gameRepository.initializeGame(
            roomId = roomId,
            players = listOf(p1, p2),
            totalRounds = 3,
            drawTimeSeconds = 60,
            difficulty = WordDifficulty.MEDIUM
        )

        // p2 departs
        gameRepository.handlePlayerDeparture(roomId, "p2")

        val state = gameRepository.observeGameState(roomId).first()
        assertEquals(GamePhase.MATCH_RESULTS, state?.phase)

        val messages = gameRepository.observeChatMessages(roomId).first()
        assertTrue(messages.any { it.text.contains("win by default", ignoreCase = true) })
    }
}
