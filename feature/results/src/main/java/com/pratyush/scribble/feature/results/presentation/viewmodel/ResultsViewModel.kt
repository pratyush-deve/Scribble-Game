package com.pratyush.scribble.feature.results.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.GamePhase
import com.pratyush.scribble.core.model.GameStateSnapshot
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.PlayerScore
import com.pratyush.scribble.core.model.ReplayVotingState
import com.pratyush.scribble.core.model.Room
import com.pratyush.scribble.core.network.repository.GameRepository
import com.pratyush.scribble.core.network.repository.RoomRepository
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository
import com.pratyush.scribble.feature.results.presentation.state.ResultsUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResultsViewModel(
    val roomCode: String,
    val winReason: String? = null,
    private val roomRepository: RoomRepository? = null,
    private val gameRepository: GameRepository? = null,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val localUid: String = authRepository?.currentUser?.value?.id ?: ""
    private var activeRoomId: String? = null
    private var cachedRoom: Room? = null
    private var hasFinalized: Boolean = false
    private var countdownJob: Job? = null

    private val _uiState = MutableStateFlow(
        ResultsUiState(
            roomCode = roomCode,
            totalRounds = 3,
            localPlayerId = localUid,
            winReason = winReason,
            rankings = emptyList()
        )
    )
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    init {
        loadResults()
        if (winReason == null) {
            startVotingTimer(15)
        }
    }

    private fun startVotingTimer(initialSeconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (remaining in initialSeconds downTo 0) {
                _uiState.update { it.copy(votingTimeRemainingSeconds = remaining) }
                if (remaining == 0) {
                    onVotingTimerExpired()
                    break
                }
                delay(1000L)
            }
        }
    }

    private var exitJob: Job? = null

    private fun triggerFailedVoteExit() {
        if (_uiState.value.isVoteFailed || _uiState.value.shouldNavigateHome || winReason != null) return
        countdownJob?.cancel()
        _uiState.update { it.copy(isVoteFailed = true, isSessionClosed = true) }
        exitJob?.cancel()
        exitJob = viewModelScope.launch {
            delay(2500L)
            _uiState.update { it.copy(shouldNavigateHome = true) }
        }
    }

    private fun onVotingTimerExpired() {
        val state = _uiState.value
        if (state.playAgainVotes >= state.requiredMajority || state.isGameRestarted) {
            return
        }

        // Authoritatively request finalization from trusted backend
        if (!hasFinalized) {
            hasFinalized = true
            val roomId = activeRoomId
            if (roomId != null && gameRepository != null) {
                viewModelScope.launch {
                    gameRepository.finalizeReplaySession(roomId)
                }
            }
        }
        triggerFailedVoteExit()
    }

    private fun loadResults() {
        val roomRepo = roomRepository ?: return
        val gameRepo = gameRepository ?: return

        viewModelScope.launch {
            val roomId = roomRepo.resolveRoomId(roomCode) ?: roomCode
            activeRoomId = roomId

            launch {
                gameRepo.finalizeMatch(roomId)
            }

            combine(
                roomRepo.observeRoom(roomId),
                gameRepo.observeGameState(roomId),
                gameRepo.observeReplayVoting(roomId)
            ) { room: Room?, gameState: GameStateSnapshot?, replayVoting: ReplayVotingState? ->
                Triple(room, gameState, replayVoting)
            }.collectLatest { (room, gameState, replayVoting) ->
                // Check if room or session was closed/deleted
                val effectiveRoom = room ?: cachedRoom
                if (effectiveRoom == null) {
                    if (winReason == null && !_uiState.value.isGameRestarted) {
                        triggerFailedVoteExit()
                    }
                    return@collectLatest
                }
                cachedRoom = effectiveRoom

                // Check if match was authoritatively restarted via replay voting
                if (replayVoting?.status == "RESTARTED") {
                    countdownJob?.cancel()
                    _uiState.update { it.copy(isGameRestarted = true) }
                    return@collectLatest
                }

                if (replayVoting?.status == "CLOSED") {
                    if (winReason == null && !_uiState.value.isGameRestarted) {
                        triggerFailedVoteExit()
                    }
                    return@collectLatest
                }

                val scoresMap = gameState?.scores ?: emptyMap()
                val totalRounds = gameState?.totalRounds ?: effectiveRoom.settings.rounds

                // Match all participants with their actual scores
                val roomPlayerMap = effectiveRoom.players.associateBy { it.id }
                val allParticipantIds = (effectiveRoom.players.map { it.id } + (gameState?.turnOrder ?: emptyList()) + scoresMap.keys).distinct()
                val allParticipants = allParticipantIds.map { playerId ->
                    val existing = roomPlayerMap[playerId]
                    val player = existing ?: Player(
                        id = playerId,
                        name = "Player",
                        avatarId = AvatarId.CAT,
                        score = scoresMap[playerId] ?: 0,
                        isConnected = false,
                        isDeparted = true
                    )
                    val score = scoresMap[playerId] ?: player.score
                    Pair(player, score)
                }

                val activePlayers = allParticipants.filter { !it.first.isDeparted && it.first.isConnected }
                val departedPlayers = allParticipants.filter { it.first.isDeparted || !it.first.isConnected }

                if (winReason != null || activePlayers.size < 2) {
                    countdownJob?.cancel()
                }

                val sortedActive = activePlayers.sortedWith(
                    compareByDescending<Pair<Player, Int>> { it.second }
                        .thenBy { it.first.name }
                )
                val sortedDeparted = departedPlayers.sortedWith(
                    compareByDescending<Pair<Player, Int>> { it.second }
                        .thenBy { it.first.name }
                )

                // Compute ranks with standard tie handling, keeping departed players ranked below active
                var currentRank = 1
                var prevScore: Int? = null
                val activeRankings = sortedActive.mapIndexed { index, (player, score) ->
                    if (prevScore != null && score < prevScore!!) {
                        currentRank = index + 1
                    }
                    prevScore = score
                    PlayerScore(
                        player = player.copy(score = score, isDeparted = false),
                        rank = currentRank,
                        score = score,
                        roundScore = 0,
                        isDeparted = false
                    )
                }

                val departedRankOffset = activeRankings.size
                var departedRank = departedRankOffset + 1
                prevScore = null
                val departedRankings = sortedDeparted.mapIndexed { index, (player, score) ->
                    if (prevScore != null && score < prevScore!!) {
                        departedRank = departedRankOffset + index + 1
                    }
                    prevScore = score
                    PlayerScore(
                        player = player.copy(score = score, isDeparted = true),
                        rank = departedRank,
                        score = score,
                        roundScore = 0,
                        isDeparted = true
                    )
                }

                val rankings = activeRankings + departedRankings

                val totalParticipants = effectiveRoom.players.size
                val defaultMajority = if (totalParticipants > 0) (totalParticipants / 2) + 1 else 1

                val votesMap = replayVoting?.votes ?: emptyMap()
                val activeParticipantIds = effectiveRoom.players.map { it.id }.toSet()
                val votedPlayers = effectiveRoom.players.filter { votesMap[it.id] == true && it.id in activeParticipantIds }
                val playAgainVotes = replayVoting?.voteCount ?: votedPlayers.size
                val requiredMajority = if (replayVoting != null && replayVoting.requiredMajority > 0) {
                    replayVoting.requiredMajority
                } else {
                    defaultMajority
                }

                val isHost = (effectiveRoom.hostId == localUid)
                val hasVoted = votesMap[localUid] == true

                _uiState.update { current ->
                    current.copy(
                        roomCode = roomCode,
                        totalRounds = totalRounds,
                        localPlayerId = localUid,
                        winReason = winReason,
                        rankings = rankings,
                        isHost = isHost,
                        hasVotedPlayAgain = hasVoted,
                        playAgainVotes = playAgainVotes,
                        requiredMajority = requiredMajority,
                        votedPlayers = votedPlayers
                    )
                }
            }
        }
    }

    fun onToggleVotePlayAgain() {
        val roomId = activeRoomId ?: return
        val gameRepo = gameRepository ?: return
        val nextVote = !_uiState.value.hasVotedPlayAgain
        viewModelScope.launch {
            gameRepo.submitPlayAgainVote(roomId, nextVote)
        }
    }

    fun onReturnToHome() {
        val roomId = activeRoomId
        val roomRepo = roomRepository
        countdownJob?.cancel()
        exitJob?.cancel()
        if (roomId != null && roomRepo != null && localUid.isNotEmpty()) {
            viewModelScope.launch {
                roomRepo.leaveRoom(roomId, localUid)
            }
        }
        _uiState.update { it.copy(isSessionClosed = true, shouldNavigateHome = true) }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        exitJob?.cancel()
    }
}
