package com.pratyush.scribble.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MaskedWordTest {

    @Test
    fun `word is masked correctly without hints`() {
        val masked = MaskedWord.fromWord("APPLE")
        assertEquals(5, masked.length)
        assertEquals("_ _ _ _ _", masked.hint)
    }

    @Test
    fun `multi-word phrase preserves spacing structure`() {
        val masked = MaskedWord.fromWord("HOT DOG")
        assertEquals(6, masked.length)
        assertEquals("_ _ _   _ _ _", masked.hint)
    }

    @Test
    fun `revealed letters are unmasked`() {
        val masked = MaskedWord.fromWord("CAT", revealedIndices = setOf(0, 2))
        assertEquals("C _ T", masked.hint)
    }
}
