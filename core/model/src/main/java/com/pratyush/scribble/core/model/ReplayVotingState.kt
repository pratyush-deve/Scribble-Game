package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ReplayVotingState(
    val status: String = "VOTING", // "VOTING" | "RESTARTED" | "CLOSED"
    val deadline: Long = 0L,
    val votes: Map<String, Boolean> = emptyMap(),
    val voteCount: Int = 0,
    val requiredMajority: Int = 1
)
