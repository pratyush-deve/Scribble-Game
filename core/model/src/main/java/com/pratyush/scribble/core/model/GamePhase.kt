package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class GamePhase {
    WAITING_FOR_PLAYERS,
    COUNTDOWN,
    SELECTING_WORD,
    DRAWING,
    ROUND_RESULTS,
    MATCH_RESULTS
}
