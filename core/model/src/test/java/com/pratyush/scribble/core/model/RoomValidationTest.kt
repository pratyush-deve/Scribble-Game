package com.pratyush.scribble.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomValidationTest {

    @Test
    fun `valid room code formats succeed`() {
        assertTrue(RoomCode.isValid("ABCD"))
        assertTrue(RoomCode.isValid("ABC12"))
        assertTrue(RoomCode.isValid("ROOM99"))
    }

    @Test
    fun `invalid room code formats fail`() {
        assertFalse(RoomCode.isValid(""))
        assertFalse(RoomCode.isValid("AB"))
        assertFalse(RoomCode.isValid("TOOLONGCODE"))
        assertFalse(RoomCode.isValid("code!"))
    }

    @Test
    fun `room settings enforces valid player limits`() {
        val settings = RoomSettings(maxPlayers = 8, rounds = 3, drawTimeSeconds = 60)
        assertEquals(8, settings.maxPlayers)
        assertEquals(3, settings.rounds)
        assertEquals(60, settings.drawTimeSeconds)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `room settings rejects invalid player count`() {
        RoomSettings(maxPlayers = 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `room settings rejects invalid round count`() {
        RoomSettings(rounds = 10)
    }
}
