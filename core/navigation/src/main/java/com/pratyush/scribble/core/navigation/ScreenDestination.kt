package com.pratyush.scribble.core.navigation

import kotlinx.serialization.Serializable

sealed interface ScreenDestination {

    @Serializable
    data object Splash : ScreenDestination

    @Serializable
    data object Auth : ScreenDestination

    @Serializable
    data object Home : ScreenDestination

    @Serializable
    data object Profile : ScreenDestination

    @Serializable
    data class Lobby(val roomCode: String) : ScreenDestination

    @Serializable
    data class Game(val roomCode: String) : ScreenDestination

    @Serializable
    data class Results(val roomCode: String, val winReason: String? = null) : ScreenDestination
}
