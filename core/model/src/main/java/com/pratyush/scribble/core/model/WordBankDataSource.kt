package com.pratyush.scribble.core.model

import java.security.MessageDigest

interface WordBankDataSource {
    fun getChoices(
        difficulty: WordDifficulty = WordDifficulty.MEDIUM,
        count: Int = 3,
        excludeWordIds: Set<String> = emptySet()
    ): List<WordChoice>

    fun getWordById(id: String): WordBankEntry?
    fun totalWords(): Int
}

class DefaultWordBankDataSource(
    private val entries: List<WordBankEntry> = DEFAULT_ENTRIES
) : WordBankDataSource {

    companion object {
        fun hashWord(word: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(word.trim().uppercase().toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        val DEFAULT_ENTRIES = listOf(
            // Easy
            WordBankEntry("e_1", "CAT", WordDifficulty.EASY, "Animals"),
            WordBankEntry("e_2", "DOG", WordDifficulty.EASY, "Animals"),
            WordBankEntry("e_3", "SUN", WordDifficulty.EASY, "Nature"),
            WordBankEntry("e_4", "TREE", WordDifficulty.EASY, "Nature"),
            WordBankEntry("e_5", "CAR", WordDifficulty.EASY, "Vehicles"),
            WordBankEntry("e_6", "FISH", WordDifficulty.EASY, "Animals"),
            WordBankEntry("e_7", "BOOK", WordDifficulty.EASY, "Objects"),
            WordBankEntry("e_8", "APPLE", WordDifficulty.EASY, "Food"),
            WordBankEntry("e_9", "BALL", WordDifficulty.EASY, "Objects"),
            WordBankEntry("e_10", "STAR", WordDifficulty.EASY, "Space"),
            WordBankEntry("e_11", "BIRD", WordDifficulty.EASY, "Animals"),
            WordBankEntry("e_12", "CUP", WordDifficulty.EASY, "Objects"),
            WordBankEntry("e_13", "MOON", WordDifficulty.EASY, "Space"),
            WordBankEntry("e_14", "HOUSE", WordDifficulty.EASY, "Buildings"),
            WordBankEntry("e_15", "BOAT", WordDifficulty.EASY, "Vehicles"),
            WordBankEntry("e_16", "HAT", WordDifficulty.EASY, "Clothing"),
            WordBankEntry("e_17", "CAKE", WordDifficulty.EASY, "Food"),
            WordBankEntry("e_18", "CLOUD", WordDifficulty.EASY, "Nature"),
            WordBankEntry("e_19", "DUCK", WordDifficulty.EASY, "Animals"),
            WordBankEntry("e_20", "PIZZA", WordDifficulty.EASY, "Food"),

            // Medium
            WordBankEntry("m_1", "GUITAR", WordDifficulty.MEDIUM, "Music"),
            WordBankEntry("m_2", "ELEPHANT", WordDifficulty.MEDIUM, "Animals"),
            WordBankEntry("m_3", "AIRPLANE", WordDifficulty.MEDIUM, "Vehicles"),
            WordBankEntry("m_4", "BICYCLE", WordDifficulty.MEDIUM, "Vehicles"),
            WordBankEntry("m_5", "CASTLE", WordDifficulty.MEDIUM, "Buildings"),
            WordBankEntry("m_6", "DOLPHIN", WordDifficulty.MEDIUM, "Animals"),
            WordBankEntry("m_7", "PENGUIN", WordDifficulty.MEDIUM, "Animals"),
            WordBankEntry("m_8", "RAINBOW", WordDifficulty.MEDIUM, "Nature"),
            WordBankEntry("m_9", "VOLCANO", WordDifficulty.MEDIUM, "Nature"),
            WordBankEntry("m_10", "MONKEY", WordDifficulty.MEDIUM, "Animals"),
            WordBankEntry("m_11", "ROCKET", WordDifficulty.MEDIUM, "Space"),
            WordBankEntry("m_12", "TELESCOPE", WordDifficulty.MEDIUM, "Science"),
            WordBankEntry("m_13", "HELICOPTER", WordDifficulty.MEDIUM, "Vehicles"),
            WordBankEntry("m_14", "PIRATE", WordDifficulty.MEDIUM, "People"),
            WordBankEntry("m_15", "ROBOT", WordDifficulty.MEDIUM, "Technology"),
            WordBankEntry("m_16", "OCTOPUS", WordDifficulty.MEDIUM, "Animals"),
            WordBankEntry("m_17", "SANDWICH", WordDifficulty.MEDIUM, "Food"),
            WordBankEntry("m_18", "LIGHTHOUSE", WordDifficulty.MEDIUM, "Buildings"),
            WordBankEntry("m_19", "KANGAROO", WordDifficulty.MEDIUM, "Animals"),
            WordBankEntry("m_20", "FIREWORK", WordDifficulty.MEDIUM, "Celebration"),

            // Hard
            WordBankEntry("h_1", "ASTRONAUT", WordDifficulty.HARD, "Space"),
            WordBankEntry("h_2", "SUBMARINE", WordDifficulty.HARD, "Vehicles"),
            WordBankEntry("h_3", "ROLLERCOASTER", WordDifficulty.HARD, "Entertainment"),
            WordBankEntry("h_4", "MICROSCOPE", WordDifficulty.HARD, "Science"),
            WordBankEntry("h_5", "CHAMELEON", WordDifficulty.HARD, "Animals"),
            WordBankEntry("h_6", "LABYRINTH", WordDifficulty.HARD, "Places"),
            WordBankEntry("h_7", "THERMOMETER", WordDifficulty.HARD, "Science"),
            WordBankEntry("h_8", "CAROUSEL", WordDifficulty.HARD, "Entertainment"),
            WordBankEntry("h_9", "ARCHAEOLOGIST", WordDifficulty.HARD, "Professions"),
            WordBankEntry("h_10", "METEORITE", WordDifficulty.HARD, "Space"),
            WordBankEntry("h_11", "CATHEDRAL", WordDifficulty.HARD, "Buildings"),
            WordBankEntry("h_12", "CENTAUR", WordDifficulty.HARD, "Mythology"),
            WordBankEntry("h_13", "GLACIER", WordDifficulty.HARD, "Nature"),
            WordBankEntry("h_14", "KALEIDOSCOPE", WordDifficulty.HARD, "Objects"),
            WordBankEntry("h_15", "SAXOPHONE", WordDifficulty.HARD, "Music")
        )
    }

    override fun getChoices(
        difficulty: WordDifficulty,
        count: Int,
        excludeWordIds: Set<String>
    ): List<WordChoice> {
        // Preferred pool matching difficulty
        var pool = entries.filter { it.difficulty == difficulty && it.id !in excludeWordIds }
        if (pool.size < count) {
            // Fallback: relax exclusion then difficulty
            pool = entries.filter { it.difficulty == difficulty }
            if (pool.size < count) {
                pool = entries
            }
        }

        return pool.shuffled().take(count).map { it.toWordChoice() }
    }

    override fun getWordById(id: String): WordBankEntry? {
        return entries.firstOrNull { it.id == id }
    }

    override fun totalWords(): Int = entries.size
}
