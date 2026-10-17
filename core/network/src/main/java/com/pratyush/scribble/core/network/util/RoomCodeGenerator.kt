package com.pratyush.scribble.core.network.util

import java.security.SecureRandom

/**
 * Utility for generating and validating cryptographically secure, unambiguous room codes.
 * Excludes ambiguous characters like O/0 and I/1.
 */
object RoomCodeGenerator {
    private const val CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val secureRandom = SecureRandom()

    fun generateCode(length: Int = 6): String {
        require(length in 4..8) { "Room code length must be between 4 and 8" }
        val chars = CharArray(length)
        for (i in 0 until length) {
            chars[i] = CHARSET[secureRandom.nextInt(CHARSET.length)]
        }
        return String(chars)
    }

    fun normalize(code: String): String {
        return code.trim().uppercase()
    }

    fun isValid(code: String): Boolean {
        val normalized = normalize(code)
        if (normalized.length !in 4..8) return false
        return normalized.all { it in CHARSET }
    }
}
