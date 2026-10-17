package com.pratyush.scribble.core.model

/**
 * Authoritative scoring engine for guessers and drawers in Scribble.
 *
 * Scoring Rules:
 * - 1st correct guess: 100 points
 * - 2nd correct guess: 80 points
 * - 3rd correct guess: 60 points
 * - 4th and later correct guesses: 40 points
 * - Each guesser is awarded points at most once per turn.
 * - Drawer earns 50 points for each player who correctly guesses the word.
 * - If zero players guess correctly, drawer earns 0 points.
 */
object GameScoringEngine {

    const val POINTS_FIRST_GUESS = 100
    const val POINTS_SECOND_GUESS = 80
    const val POINTS_THIRD_GUESS = 60
    const val POINTS_LATER_GUESS = 40
    const val DRAWER_BONUS_PER_CORRECT_GUESS = 50

    /**
     * Calculates the points earned by a guesser based on their 1-indexed [guessOrder].
     * (1 = first correct guess, 2 = second correct guess, etc.)
     */
    fun calculateGuesserPoints(guessOrder: Int): Int {
        return when {
            guessOrder <= 0 -> 0
            guessOrder == 1 -> POINTS_FIRST_GUESS
            guessOrder == 2 -> POINTS_SECOND_GUESS
            guessOrder == 3 -> POINTS_THIRD_GUESS
            else -> POINTS_LATER_GUESS
        }
    }

    /**
     * Calculates the points awarded to the drawer at the end of the turn based on [correctGuessCount].
     */
    fun calculateDrawerPoints(correctGuessCount: Int): Int {
        if (correctGuessCount <= 0) return 0
        return correctGuessCount * DRAWER_BONUS_PER_CORRECT_GUESS
    }

    /**
     * Evaluates a guess against an authoritative answer.
     * Trims whitespace and ignores case.
     */
    fun isCorrectGuess(guess: String, answer: String): Boolean {
        val normalizedGuess = guess.trim()
        val normalizedAnswer = answer.trim()
        if (normalizedGuess.isEmpty() || normalizedAnswer.isEmpty()) return false
        return normalizedGuess.equals(normalizedAnswer, ignoreCase = true)
    }

    /**
     * Returns true if guess is close (e.g. Levenshtein edit distance == 1 for words of length >= 4).
     */
    fun isCloseGuess(guess: String, answer: String): Boolean {
        val g = guess.trim().uppercase()
        val a = answer.trim().uppercase()
        if (g.isEmpty() || a.isEmpty() || g == a) return false
        if (a.length < 4) return false
        return levenshteinDistance(g, a) == 1
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
