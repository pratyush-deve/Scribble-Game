package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class MaskedWord(
    val length: Int,
    val hint: String,
    val letterCount: List<Int> = listOf(length)
) {
    companion object {
        fun fromWord(word: String, revealedIndices: Set<Int> = emptySet()): MaskedWord {
            val words = word.trim().split(" ")
            val letterCounts = words.map { it.length }
            var charIndex = 0
            val wordHints = words.map { w ->
                val letters = mutableListOf<String>()
                for (ch in w) {
                    if (revealedIndices.contains(charIndex)) {
                        letters.add(ch.uppercaseChar().toString())
                    } else {
                        letters.add("_")
                    }
                    charIndex++
                }
                letters.joinToString(" ")
            }
            return MaskedWord(
                length = word.replace(" ", "").length,
                hint = wordHints.joinToString("   "),
                letterCount = letterCounts
            )
        }
    }
}
