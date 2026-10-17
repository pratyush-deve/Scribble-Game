package com.pratyush.scribble.feature.game

import com.pratyush.scribble.core.model.ChatMessageType
import com.pratyush.scribble.core.model.MaskedWord
import com.pratyush.scribble.feature.game.presentation.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordVisibilitySecurityTest {

    @Test
    fun `drawer sees the complete secret word`() {
        val viewModel = GameViewModel(roomCode = "ROOM1", initialIsDrawer = true)
        val state = viewModel.gameUiState.value

        assertTrue("Player should be marked as local drawer", state.isLocalDrawer)
        assertEquals("ELEPHANT", state.drawerSecretWord)
        assertEquals("ELEPHANT", state.displayedWord)
        assertFalse("Round should not be ended initially", state.isRoundEnded)
    }

    @Test
    fun `guessing player sees only the masked word and never the secret word`() {
        val viewModel = GameViewModel(roomCode = "ROOM1", initialIsDrawer = false)
        val state = viewModel.gameUiState.value

        assertFalse("Player should be marked as guesser", state.isLocalDrawer)
        assertNull("Guessing player state MUST NOT hold the drawerSecretWord", state.drawerSecretWord)

        // The displayedWord must be exactly the masked hint "_ _ _ _ _ _ _ _"
        val expectedMaskedHint = MaskedWord.fromWord("ELEPHANT").hint
        assertEquals(expectedMaskedHint, state.displayedWord)
        assertNotEquals("ELEPHANT", state.displayedWord)
        assertFalse("Displayed word must not contain raw letters", state.displayedWord.contains("ELEPHANT"))
    }

    @Test
    fun `masked word preserves the correct letter count without revealing characters`() {
        val word = "ELEPHANT"
        val masked = MaskedWord.fromWord(word)

        assertEquals("Length must match total non-space characters", 8, masked.length)
        assertEquals(8, masked.hint.split(" ").filter { it.isNotEmpty() }.size)
        assertTrue("All placeholders must be blanks", masked.hint.all { it == '_' || it == ' ' })
    }

    @Test
    fun `correct guess feedback does not reveal the secret word to chat`() {
        val viewModel = GameViewModel(roomCode = "ROOM1", initialIsDrawer = false)

        viewModel.onChatInputChanged("ELEPHANT")
        viewModel.onSendGuess()

        val state = viewModel.gameUiState.value
        assertTrue("Player should be marked as having guessed correctly", state.hasGuessedCorrectly)

        val latestMessage = state.chatMessages.last()
        assertEquals(ChatMessageType.CORRECT_GUESS, latestMessage.type)
        assertEquals("guessed the word!", latestMessage.text)

        // The text must NOT leak the answer string to the chat stream
        assertFalse("Chat message text must not contain the secret word", latestMessage.text.contains("ELEPHANT", ignoreCase = true))
    }

    @Test
    fun `word is revealed to everyone only after the round has officially ended`() {
        val viewModel = GameViewModel(roomCode = "ROOM1", initialIsDrawer = false)

        // Before round ends: guesser sees only blanks
        assertEquals(MaskedWord.fromWord("ELEPHANT").hint, viewModel.gameUiState.value.displayedWord)
        assertFalse(viewModel.gameUiState.value.isRoundEnded)

        // Trigger end of round
        viewModel.finishGameNow()

        val endedState = viewModel.gameUiState.value
        assertTrue("Round should now be marked ended", endedState.isRoundEnded)
        // Now and only now is the secret word revealed in displayedWord
        assertEquals("ELEPHANT", endedState.displayedWord)
    }

    @Test
    fun `drawer selecting a new word updates drawer with word and guesser with new mask`() {
        val drawerViewModel = GameViewModel(roomCode = "ROOM1", initialIsDrawer = true)
        drawerViewModel.onWordSelected("GIRAFFE")

        val drawerState = drawerViewModel.gameUiState.value
        assertEquals("GIRAFFE", drawerState.displayedWord)
        assertEquals("GIRAFFE", drawerState.drawerSecretWord)

        val guesserViewModel = GameViewModel(roomCode = "ROOM1", initialIsDrawer = false)
        guesserViewModel.onWordSelected("GIRAFFE")

        val guesserState = guesserViewModel.gameUiState.value
        assertNull(guesserState.drawerSecretWord)
        val expectedHint = MaskedWord.fromWord("GIRAFFE").hint
        assertEquals(expectedHint, guesserState.displayedWord)
        assertFalse(guesserState.displayedWord.contains("GIRAFFE"))
    }
}
