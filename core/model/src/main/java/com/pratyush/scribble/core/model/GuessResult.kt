package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class GuessResult(
    val playerId: String,
    val playerName: String,
    val isCorrect: Boolean,
    val isClose: Boolean = false,
    val pointsAwarded: Int = 0,
    val guessOrder: Int = 0,
    val guessText: String = ""
) {
    companion object {
        fun correct(playerId: String, playerName: String, points: Int, order: Int): GuessResult =
            GuessResult(playerId = playerId, playerName = playerName, isCorrect = true, pointsAwarded = points, guessOrder = order)

        fun wrong(playerId: String, playerName: String, text: String): GuessResult =
            GuessResult(playerId = playerId, playerName = playerName, isCorrect = false, guessText = text)

        fun close(playerId: String, playerName: String, text: String): GuessResult =
            GuessResult(playerId = playerId, playerName = playerName, isCorrect = false, isClose = true, guessText = text)

        fun alreadyGuessed(playerId: String, playerName: String): GuessResult =
            GuessResult(playerId = playerId, playerName = playerName, isCorrect = false)
    }
}
