package com.pratyush.scribble.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameScoringEngineTest {

    @Test
    fun `guesser points decrease by guess order`() {
        assertEquals(100, GameScoringEngine.calculateGuesserPoints(1))
        assertEquals(80, GameScoringEngine.calculateGuesserPoints(2))
        assertEquals(60, GameScoringEngine.calculateGuesserPoints(3))
        assertEquals(40, GameScoringEngine.calculateGuesserPoints(4))
        assertEquals(40, GameScoringEngine.calculateGuesserPoints(5))
        assertEquals(40, GameScoringEngine.calculateGuesserPoints(8))
    }

    @Test
    fun `invalid guess order returns zero points`() {
        assertEquals(0, GameScoringEngine.calculateGuesserPoints(0))
        assertEquals(0, GameScoringEngine.calculateGuesserPoints(-1))
    }

    @Test
    fun `drawer scores 50 points per correct guess`() {
        assertEquals(50, GameScoringEngine.calculateDrawerPoints(1))
        assertEquals(100, GameScoringEngine.calculateDrawerPoints(2))
        assertEquals(150, GameScoringEngine.calculateDrawerPoints(3))
        assertEquals(200, GameScoringEngine.calculateDrawerPoints(4))
    }

    @Test
    fun `drawer scores zero when no players guess correctly`() {
        assertEquals(0, GameScoringEngine.calculateDrawerPoints(0))
        assertEquals(0, GameScoringEngine.calculateDrawerPoints(-1))
    }

    @Test
    fun `isCorrectGuess handles case and whitespace`() {
        assertTrue(GameScoringEngine.isCorrectGuess("elephant", "ELEPHANT"))
        assertTrue(GameScoringEngine.isCorrectGuess("  ELEPHANT  ", "elephant"))
        assertTrue(GameScoringEngine.isCorrectGuess("Elephant", "Elephant"))
        assertFalse(GameScoringEngine.isCorrectGuess("elephants", "elephant"))
        assertFalse(GameScoringEngine.isCorrectGuess("", "elephant"))
        assertFalse(GameScoringEngine.isCorrectGuess("   ", "elephant"))
    }

    @Test
    fun `isCloseGuess detects 1-character typo on long words`() {
        assertTrue(GameScoringEngine.isCloseGuess("ELEPHANTT", "ELEPHANT"))
        assertTrue(GameScoringEngine.isCloseGuess("ELEPHNT", "ELEPHANT"))
        assertTrue(GameScoringEngine.isCloseGuess("ELEPHUNT", "ELEPHANT"))

        // Exact match is not "close", it is correct!
        assertFalse(GameScoringEngine.isCloseGuess("ELEPHANT", "ELEPHANT"))

        // Too far
        assertFalse(GameScoringEngine.isCloseGuess("DOG", "ELEPHANT"))

        // Word too short (< 4 chars) does not trigger close guess
        assertFalse(GameScoringEngine.isCloseGuess("CA", "CAT"))
    }
}
