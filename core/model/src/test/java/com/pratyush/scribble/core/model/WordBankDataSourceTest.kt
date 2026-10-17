package com.pratyush.scribble.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordBankDataSourceTest {

    private val dataSource = DefaultWordBankDataSource()

    @Test
    fun `getChoices returns exact requested count of distinct choices`() {
        val choices = dataSource.getChoices(WordDifficulty.EASY, count = 3)
        assertEquals(3, choices.size)
        assertEquals(3, choices.map { it.word }.distinct().size)
    }

    @Test
    fun `getChoices respects difficulty`() {
        val easyChoices = dataSource.getChoices(WordDifficulty.EASY, count = 3)
        assertTrue(easyChoices.all { it.difficulty == WordDifficulty.EASY })

        val hardChoices = dataSource.getChoices(WordDifficulty.HARD, count = 3)
        assertTrue(hardChoices.all { it.difficulty == WordDifficulty.HARD })
    }

    @Test
    fun `getChoices avoids excluded words`() {
        val initialChoices = dataSource.getChoices(WordDifficulty.EASY, count = 5)
        val excludedIds = initialChoices.map { it.id }.toSet()

        val nextChoices = dataSource.getChoices(WordDifficulty.EASY, count = 3, excludeWordIds = excludedIds)
        assertTrue(nextChoices.none { it.id in excludedIds })
    }

    @Test
    fun `hashWord produces consistent SHA-256 hex string`() {
        val hash1 = DefaultWordBankDataSource.hashWord("ELEPHANT")
        val hash2 = DefaultWordBankDataSource.hashWord("  elephant  ")
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length) // 256 bits = 64 hex chars
        assertFalse(hash1.contains("ELEPHANT", ignoreCase = true))
    }
}
