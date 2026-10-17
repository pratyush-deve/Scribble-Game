package com.pratyush.scribble.core.database.repository

import com.pratyush.scribble.core.model.MatchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface GameHistoryRepository {
    val recentMatches: Flow<List<MatchResult>>
    suspend fun saveMatchResult(result: MatchResult)
}

class MockGameHistoryRepository : GameHistoryRepository {
    private val _matches = MutableStateFlow<List<MatchResult>>(emptyList())
    override val recentMatches: Flow<List<MatchResult>> = _matches.asStateFlow()

    override suspend fun saveMatchResult(result: MatchResult) {
        _matches.value = listOf(result) + _matches.value
    }
}
