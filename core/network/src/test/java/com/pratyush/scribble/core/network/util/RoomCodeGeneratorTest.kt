package com.pratyush.scribble.core.network.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomCodeGeneratorTest {

    @Test
    fun `generateCode returns string of specified length`() {
        val code6 = RoomCodeGenerator.generateCode(6)
        assertEquals(6, code6.length)

        val code4 = RoomCodeGenerator.generateCode(4)
        assertEquals(4, code4.length)
    }

    @Test
    fun `generateCode excludes ambiguous characters O, 0, I, 1`() {
        val ambiguousChars = setOf('O', '0', 'I', '1')
        for (i in 1..200) {
            val code = RoomCodeGenerator.generateCode(6)
            for (c in code) {
                assertFalse("Code $code contained ambiguous char $c", c in ambiguousChars)
            }
        }
    }

    @Test
    fun `normalize trims whitespace and converts to uppercase`() {
        assertEquals("ABCD23", RoomCodeGenerator.normalize("  abcd23  "))
    }

    @Test
    fun `isValid accepts valid codes without ambiguous characters`() {
        assertTrue(RoomCodeGenerator.isValid("ABCD23"))
        assertTrue(RoomCodeGenerator.isValid("XY79PQ"))
    }

    @Test
    fun `isValid rejects codes with ambiguous or special characters`() {
        assertFalse(RoomCodeGenerator.isValid("ABCD01")) // contains 0 and 1
        assertFalse(RoomCodeGenerator.isValid("ABCIO2")) // contains I and O
        assertFalse(RoomCodeGenerator.isValid("AB@#$1")) // contains symbols
        assertFalse(RoomCodeGenerator.isValid("AB"))     // too short
        assertFalse(RoomCodeGenerator.isValid("ABCDEFGHIJK")) // too long
    }
}
