package com.pratyush.scribble.feature.game

import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.ChatMessageType
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.feature.game.presentation.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameDepartureHandlingTest {

    @Test
    fun `back action opens the confirmation dialog without leaving`() {
        val viewModel = GameViewModel(roomCode = "ROOM1")
        assertFalse(viewModel.gameUiState.value.showLeaveConfirmationDialog)
        assertFalse(viewModel.gameUiState.value.isPlayerDeparted)

        // Trigger back action
        viewModel.onRequestLeaveGame()

        assertTrue("Confirmation dialog should be visible", viewModel.gameUiState.value.showLeaveConfirmationDialog)
        assertFalse("Player must not have departed yet", viewModel.gameUiState.value.isPlayerDeparted)
        assertFalse("Game should still be active", viewModel.gameUiState.value.isGameFinished)
    }

    @Test
    fun `canceling confirmation dialog keeps the player in the game`() {
        val viewModel = GameViewModel(roomCode = "ROOM1")
        viewModel.onRequestLeaveGame()
        assertTrue(viewModel.gameUiState.value.showLeaveConfirmationDialog)

        // User taps "Stay"
        viewModel.onDismissLeaveConfirmation()

        assertFalse("Dialog should be dismissed", viewModel.gameUiState.value.showLeaveConfirmationDialog)
        assertFalse("Player remains in game", viewModel.gameUiState.value.isPlayerDeparted)
        assertFalse("Game remains active", viewModel.gameUiState.value.isGameFinished)
    }

    @Test
    fun `confirmed departure is processed exactly once`() {
        val viewModel = GameViewModel(roomCode = "ROOM1")
        viewModel.onRequestLeaveGame()

        // User taps "Leave game"
        viewModel.onConfirmLeaveGame()

        assertTrue("Player should be marked as departed", viewModel.gameUiState.value.isPlayerDeparted)
        assertFalse("Confirmation dialog should be closed", viewModel.gameUiState.value.showLeaveConfirmationDialog)

        // Simulating repeated fast taps or back presses
        viewModel.onConfirmLeaveGame()
        viewModel.onRequestLeaveGame()

        assertTrue("State remains departed", viewModel.gameUiState.value.isPlayerDeparted)
        assertFalse("Dialog should not re-open after departure", viewModel.gameUiState.value.showLeaveConfirmationDialog)
    }

    @Test
    fun `one of two active players leaves - remaining player wins by default`() {
        val p1 = Player("p1", "Player1", AvatarId.FOX, score = 100)
        val p2 = Player("p2", "Player2", AvatarId.CAT, score = 80)
        val viewModel = GameViewModel(roomCode = "ROOM1", initialPlayers = listOf(p1, p2))

        assertEquals(2, viewModel.gameUiState.value.players.size)
        assertFalse(viewModel.gameUiState.value.isGameFinished)

        // Player 2 leaves the 2-player game
        viewModel.handlePlayerDeparture(departingPlayerId = "p2")

        val state = viewModel.gameUiState.value
        assertTrue("Game should immediately finish", state.isGameFinished)
        assertTrue("Round should end", state.isRoundEnded)
        assertEquals(0, state.remainingTimeSeconds)

        // Player 2 is removed, only Player 1 remains
        assertEquals(1, state.players.size)
        assertEquals("p1", state.players.first().id)
        // Score preserved without spurious round bonuses
        assertEquals(100, state.players.first().score)

        // Win by default notification
        assertNotNull("Must set default win reason", state.defaultWinReason)
        assertEquals("Your opponent left the game. You win by default!", state.defaultWinReason)

        // System message added to chat
        val leaveMessage = state.chatMessages.last()
        assertEquals(ChatMessageType.SYSTEM_INFO, leaveMessage.type)
        assertTrue(leaveMessage.text.contains("Player2 left the game"))
        assertTrue(leaveMessage.text.contains("Player1 wins by default"))
    }

    @Test
    fun `one of three or more players leaves - game continues and system message added`() {
        val p1 = Player("p1", "Player1", AvatarId.FOX, score = 100)
        val p2 = Player("p2", "Player2", AvatarId.CAT, score = 80)
        val p3 = Player("p3", "Player3", AvatarId.PANDA, score = 60)
        val viewModel = GameViewModel(roomCode = "ROOM1", initialPlayers = listOf(p1, p2, p3))

        assertEquals(3, viewModel.gameUiState.value.players.size)

        // Player 3 leaves (a guessing player)
        viewModel.handlePlayerDeparture(departingPlayerId = "p3")

        val state = viewModel.gameUiState.value
        assertFalse("Game should NOT end because 2 players still remain", state.isGameFinished)
        assertEquals(2, state.players.size)
        assertFalse(state.players.any { it.id == "p3" })

        // Check system message
        val notice = state.chatMessages.last()
        assertEquals(ChatMessageType.SYSTEM_INFO, notice.type)
        assertEquals("'Player3' left the game.", notice.text)
    }

    @Test
    fun `departing drawer leaves - turn advances safely to next eligible player`() {
        val p1 = Player("p1", "DrawerPlayer", AvatarId.FOX, score = 100)
        val p2 = Player("p2", "Guesser1", AvatarId.CAT, score = 80)
        val p3 = Player("p3", "Guesser2", AvatarId.PANDA, score = 60)
        val viewModel = GameViewModel(roomCode = "ROOM1", initialIsDrawer = true, initialPlayers = listOf(p1, p2, p3))

        assertEquals("p1", viewModel.gameUiState.value.drawerPlayer?.id)

        // p1 (the active drawer) leaves
        viewModel.handlePlayerDeparture(departingPlayerId = "p1")

        val state = viewModel.gameUiState.value
        assertFalse("Game must continue with remaining 2 players", state.isGameFinished)
        assertEquals(2, state.players.size)
        // Next drawer is assigned
        assertEquals("p2", state.drawerPlayer?.id)
        // Timer reset to 60s for new turn
        assertEquals(60, state.remainingTimeSeconds)

        // Chat notification
        val notice = state.chatMessages.last()
        assertEquals(ChatMessageType.SYSTEM_INFO, notice.type)
        assertEquals("'DrawerPlayer' left the game.", notice.text)
    }

    @Test
    fun `guessing player leaves - current turn continues`() {
        val p1 = Player("p1", "DrawerPlayer", AvatarId.FOX, score = 100)
        val p2 = Player("p2", "Guesser1", AvatarId.CAT, score = 80)
        val p3 = Player("p3", "Guesser2", AvatarId.PANDA, score = 60)
        val viewModel = GameViewModel(roomCode = "ROOM1", initialIsDrawer = true, initialPlayers = listOf(p1, p2, p3))

        // Guesser2 leaves
        viewModel.handlePlayerDeparture(departingPlayerId = "p3")

        val state = viewModel.gameUiState.value
        // Drawer remains unchanged
        assertEquals("p1", state.drawerPlayer?.id)
        assertTrue(state.isLocalDrawer)
        assertEquals(2, state.players.size)
    }

    @Test
    fun `no additional score or turn advancement occurs after game over`() {
        val p1 = Player("p1", "Player1", AvatarId.FOX, score = 100)
        val p2 = Player("p2", "Player2", AvatarId.CAT, score = 80)
        val viewModel = GameViewModel(roomCode = "ROOM1", initialPlayers = listOf(p1, p2))

        // Game ends via default win
        viewModel.handlePlayerDeparture(departingPlayerId = "p2")
        assertTrue(viewModel.gameUiState.value.isGameFinished)

        val finalScore = viewModel.gameUiState.value.players.first().score

        // Attempting to send guesses or departure actions after game over
        viewModel.onChatInputChanged("GUESS")
        viewModel.onSendGuess()
        viewModel.handlePlayerDeparture(departingPlayerId = "p1")

        // Scores and game state must not advance or mutate
        assertEquals(finalScore, viewModel.gameUiState.value.players.first().score)
        assertTrue(viewModel.gameUiState.value.isGameFinished)
    }

    @Test
    fun `repeated exit actions do not generate duplicate departure events`() {
        val viewModel = GameViewModel(roomCode = "ROOM1")

        viewModel.onRequestLeaveGame()
        viewModel.onConfirmLeaveGame()
        val firstTimestamp = viewModel.gameUiState.value.isPlayerDeparted

        assertTrue(firstTimestamp)

        // Trigger again
        viewModel.onConfirmLeaveGame()
        viewModel.onRequestLeaveGame()

        // Still true and no crash or state corruption
        assertTrue(viewModel.gameUiState.value.isPlayerDeparted)
        assertFalse(viewModel.gameUiState.value.showLeaveConfirmationDialog)
    }
}
