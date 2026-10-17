package com.pratyush.scribble.core.network.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.ChatMessage
import com.pratyush.scribble.core.model.ChatMessageType
import com.pratyush.scribble.core.model.DefaultWordBankDataSource
import com.pratyush.scribble.core.model.DrawingStroke
import com.pratyush.scribble.core.model.DrawerSecretState
import com.pratyush.scribble.core.model.GamePhase
import com.pratyush.scribble.core.model.GameScoringEngine
import com.pratyush.scribble.core.model.GameStateSnapshot
import com.pratyush.scribble.core.model.GuessResult
import com.pratyush.scribble.core.model.MaskedWord
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.WordBankDataSource
import com.pratyush.scribble.core.model.WordChoice
import com.pratyush.scribble.core.model.WordDifficulty
import com.pratyush.scribble.core.network.model.FirebaseChatMessageDto
import com.pratyush.scribble.core.network.model.FirebaseDrawerSecretDto
import com.pratyush.scribble.core.network.model.FirebaseGameStateDto
import com.pratyush.scribble.core.network.model.FirebaseStrokeDto
import com.pratyush.scribble.core.network.model.FirebaseWordChoiceDto
import com.google.firebase.functions.FirebaseFunctions
import com.pratyush.scribble.core.model.ReplayVotingState
import com.pratyush.scribble.core.network.model.FirebaseReplayVotingDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirebaseGameRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val wordBankDataSource: WordBankDataSource = DefaultWordBankDataSource(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
) : GameRepository {

    private val roomsRef: DatabaseReference = database.getReference("rooms")
    private val secretsRef: DatabaseReference = database.getReference("gameSecrets")

    companion object {
        private const val TAG = "FirebaseGameRepository"
        const val SELECTION_WINDOW_MS = 10_000L
    }

    override fun observeGameState(roomId: String): Flow<GameStateSnapshot?> = callbackFlow {
        val ref = roomsRef.child(roomId).child("gameState")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }
                try {
                    val dto = snapshot.getValue(FirebaseGameStateDto::class.java)
                    if (dto != null) {
                        val phase = runCatching { GamePhase.valueOf(dto.phase) }.getOrDefault(GamePhase.SELECTING_WORD)
                        trySend(
                            GameStateSnapshot(
                                phase = phase,
                                round = dto.round,
                                totalRounds = dto.totalRounds,
                                turnNumber = dto.turnNumber,
                                turnId = dto.turnId,
                                drawerId = dto.drawerId,
                                turnDeadline = dto.turnDeadline,
                                maskedWord = dto.maskedWord,
                                wordLength = dto.wordLength,
                                wordHash = dto.wordHash,
                                revealedWord = dto.revealedWord,
                                correctGuessers = dto.correctGuessers,
                                turnOrder = dto.turnOrder,
                                clearSeq = dto.clearSeq,
                                scores = dto.scores
                            )
                        )
                    } else {
                        trySend(null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing gameState snapshot", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeDrawingStrokes(roomId: String, turnId: String): Flow<List<DrawingStroke>> = callbackFlow {
        if (turnId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val strokesRef = roomsRef.child(roomId).child("drawing").child(turnId).child("strokes")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dtoList = mutableListOf<FirebaseStrokeDto>()
                for (child in snapshot.children) {
                    val dto = child.getValue(FirebaseStrokeDto::class.java) ?: continue
                    if (!dto.deleted) {
                        dtoList.add(dto)
                    }
                }
                dtoList.sortBy { it.seq }
                val list = dtoList.map { it.toDrawingStroke() }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        strokesRef.addValueEventListener(listener)
        awaitClose { strokesRef.removeEventListener(listener) }
    }

    override fun observeChatMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val chatRef = roomsRef.child(roomId).child("chat")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val dto = child.getValue(FirebaseChatMessageDto::class.java) ?: continue
                    val type = runCatching { ChatMessageType.valueOf(dto.type) }.getOrDefault(ChatMessageType.CHAT)
                    list.add(
                        ChatMessage(
                            id = dto.id.ifEmpty { child.key.orEmpty() },
                            senderId = dto.senderId,
                            senderName = dto.senderName,
                            text = dto.text,
                            timestamp = dto.timestamp,
                            type = type
                        )
                    )
                }
                list.sortBy { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        chatRef.addValueEventListener(listener)
        awaitClose { chatRef.removeEventListener(listener) }
    }

    override fun observeDrawerSecret(roomId: String, playerId: String): Flow<DrawerSecretState?> = callbackFlow {
        val secretRef = secretsRef.child(roomId).child(playerId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }
                try {
                    val dto = snapshot.getValue(FirebaseDrawerSecretDto::class.java)
                    if (dto != null) {
                        val choices = dto.wordChoices.map {
                            val diff = runCatching { WordDifficulty.valueOf(it.difficulty) }.getOrDefault(WordDifficulty.MEDIUM)
                            WordChoice(it.id, it.word, diff)
                        }
                        trySend(
                            DrawerSecretState(
                                turnId = dto.turnId,
                                drawerId = dto.drawerId,
                                selectedWord = dto.selectedWord,
                                wordChoices = choices
                            )
                        )
                    } else {
                        trySend(null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing secret snapshot", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        secretRef.addValueEventListener(listener)
        awaitClose { secretRef.removeEventListener(listener) }
    }

    override suspend fun sendStroke(
        roomId: String,
        turnId: String,
        stroke: DrawingStroke,
        seq: Long
    ): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        val dto = FirebaseStrokeDto.fromDrawingStroke(stroke, turnId, "", seq)
        val ref = roomsRef.child(roomId).child("drawing").child(turnId).child("strokes").child(stroke.id)
        ref.setValue(dto) { error, _ ->
            if (error != null) {
                cont.resume(AppResult.Error(error.message))
            } else {
                cont.resume(AppResult.Success(Unit))
            }
        }
    }

    override suspend fun deleteStrokes(
        roomId: String,
        turnId: String,
        strokeIds: List<String>
    ): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        if (strokeIds.isEmpty()) {
            cont.resume(AppResult.Success(Unit))
            return@suspendCancellableCoroutine
        }
        val updates = HashMap<String, Any>()
        for (id in strokeIds) {
            updates["$id/deleted"] = true
        }
        roomsRef.child(roomId).child("drawing").child(turnId).child("strokes").updateChildren(updates) { error, _ ->
            if (error != null) {
                cont.resume(AppResult.Error(error.message))
            } else {
                cont.resume(AppResult.Success(Unit))
            }
        }
    }

    override suspend fun clearDrawing(roomId: String, turnId: String): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        val turnDrawingRef = roomsRef.child(roomId).child("drawing").child(turnId)
        turnDrawingRef.child("strokes").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updates = HashMap<String, Any>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    updates["strokes/$key/deleted"] = true
                }
                updates["clearSeq"] = ServerValue.TIMESTAMP
                turnDrawingRef.updateChildren(updates) { error, _ ->
                    if (error != null) {
                        cont.resume(AppResult.Error(error.message))
                    } else {
                        cont.resume(AppResult.Success(Unit))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                cont.resume(AppResult.Error(error.message))
            }
        })
    }

    override suspend fun sendChatMessage(roomId: String, message: ChatMessage): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        val msgRef = roomsRef.child(roomId).child("chat").push()
        val key = msgRef.key ?: message.id
        val dto = FirebaseChatMessageDto(
            id = key,
            senderId = message.senderId,
            senderName = message.senderName,
            text = message.text,
            timestamp = if (message.timestamp > 0) message.timestamp else System.currentTimeMillis(),
            type = message.type.name
        )
        msgRef.setValue(dto) { error, _ ->
            if (error != null) {
                cont.resume(AppResult.Error(error.message))
            } else {
                cont.resume(AppResult.Success(Unit))
            }
        }
    }

    override suspend fun submitGuess(
        roomId: String,
        player: Player,
        guessText: String
    ): AppResult<GuessResult> = suspendCancellableCoroutine { cont ->
        val trimmed = guessText.trim()
        if (trimmed.isEmpty() || trimmed.length > 100) {
            cont.resume(AppResult.Error("Invalid guess length"))
            return@suspendCancellableCoroutine
        }

        val gameStateRef = roomsRef.child(roomId).child("gameState")
        gameStateRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val gameState = snapshot.getValue(FirebaseGameStateDto::class.java)
                if (gameState == null || gameState.phase != "DRAWING" || gameState.drawerId == player.id) {
                    sendChatAndResume(roomId, player, trimmed, ChatMessageType.CHAT, GuessResult.wrong(player.id, player.name, trimmed), cont)
                    return
                }

                val correctGuessers = gameState.correctGuessers
                if (correctGuessers.containsKey(player.id)) {
                    cont.resume(AppResult.Success(GuessResult.alreadyGuessed(player.id, player.name)))
                    return
                }

                val guessHash = DefaultWordBankDataSource.hashWord(trimmed)
                val isCorrect = guessHash.equals(gameState.wordHash, ignoreCase = true)

                if (isCorrect) {
                    val guessOrder = correctGuessers.size + 1
                    val pointsEarned = GameScoringEngine.calculateGuesserPoints(guessOrder)

                    val guesserRef = gameStateRef.child("correctGuessers").child(player.id)
                    guesserRef.setValue(ServerValue.TIMESTAMP) { guesserErr, _ ->
                        if (guesserErr != null) {
                            cont.resume(AppResult.Error(guesserErr.message))
                            return@setValue
                        }

                        val msgRef = roomsRef.child(roomId).child("chat").push()
                        val msgId = msgRef.key ?: "msg_${System.currentTimeMillis()}_${player.id}"
                        val chatDto = FirebaseChatMessageDto(
                            id = msgId,
                            senderId = player.id,
                            senderName = player.name,
                            text = "guessed the word!",
                            type = ChatMessageType.CORRECT_GUESS.name,
                            timestamp = System.currentTimeMillis()
                        )
                        msgRef.setValue(chatDto) { chatErr, _ ->
                            if (chatErr != null) {
                                Log.w(TAG, "Chat notification failed: ${chatErr.message}")
                            }
                            cont.resume(AppResult.Success(GuessResult.correct(player.id, player.name, pointsEarned, guessOrder)))
                        }
                    }
                } else {
                    sendChatAndResume(roomId, player, trimmed, ChatMessageType.CHAT, GuessResult.wrong(player.id, player.name, trimmed), cont)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                cont.resume(AppResult.Error(error.message))
            }
        })
    }

    private fun sendChatAndResume(
        roomId: String,
        player: Player,
        text: String,
        type: ChatMessageType,
        result: GuessResult,
        cont: kotlinx.coroutines.CancellableContinuation<AppResult<GuessResult>>
    ) {
        val msgRef = roomsRef.child(roomId).child("chat").push()
        val msgId = msgRef.key ?: "msg_${System.currentTimeMillis()}_${player.id}"
        val chatDto = FirebaseChatMessageDto(
            id = msgId,
            senderId = player.id,
            senderName = player.name,
            text = text,
            type = type.name,
            timestamp = System.currentTimeMillis()
        )
        msgRef.setValue(chatDto) { error, _ ->
            if (error != null) {
                cont.resume(AppResult.Error(error.message))
            } else {
                cont.resume(AppResult.Success(result))
            }
        }
    }

    override suspend fun initializeGame(
        roomId: String,
        players: List<Player>,
        totalRounds: Int,
        drawTimeSeconds: Int,
        difficulty: WordDifficulty
    ): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        if (players.size < 2) {
            cont.resume(AppResult.Error("Need at least 2 players to start game"))
            return@suspendCancellableCoroutine
        }

        val turnOrder = players.map { it.id }.sorted()
        val firstDrawerId = turnOrder.first()
        val turnId = "turn_1"
        val countdownDeadline = System.currentTimeMillis() + 5000L

        val gameStateDto = FirebaseGameStateDto(
            phase = GamePhase.COUNTDOWN.name,
            round = 1,
            totalRounds = totalRounds,
            turnNumber = 1,
            turnId = turnId,
            drawerId = firstDrawerId,
            turnDeadline = countdownDeadline,
            maskedWord = "",
            wordLength = 0,
            wordHash = "",
            revealedWord = null,
            correctGuessers = emptyMap(),
            turnOrder = turnOrder,
            clearSeq = 0L,
            scores = players.associate { it.id to 0 }
        )

        val updates = mapOf(
            "gameState" to gameStateDto,
            "status" to "IN_PROGRESS"
        )

        roomsRef.child(roomId).updateChildren(updates) { error, _ ->
            if (error != null) {
                cont.resume(AppResult.Error(error.message))
            } else {
                cont.resume(AppResult.Success(Unit))
            }
        }
    }

    override suspend fun startWordSelection(
        roomId: String,
        drawTimeSeconds: Int
    ): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        val deadline = System.currentTimeMillis() + (drawTimeSeconds * 1000L)
        val updates = mapOf(
            "phase" to GamePhase.SELECTING_WORD.name,
            "turnDeadline" to deadline
        )
        roomsRef.child(roomId).child("gameState").updateChildren(updates) { error, _ ->
            if (error != null) {
                cont.resume(AppResult.Error(error.message))
            } else {
                cont.resume(AppResult.Success(Unit))
            }
        }
    }

    override suspend fun ensureDrawerSecret(
        roomId: String,
        turnId: String,
        drawerId: String,
        difficulty: WordDifficulty
    ): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        val secretRef = secretsRef.child(roomId).child(drawerId)
        secretRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existing = snapshot.getValue(FirebaseDrawerSecretDto::class.java)
                if (existing != null && existing.turnId == turnId && existing.wordChoices.isNotEmpty()) {
                    cont.resume(AppResult.Success(Unit))
                    return
                }

                val choices = wordBankDataSource.getChoices(difficulty, count = 3)
                val secretDto = FirebaseDrawerSecretDto(
                    turnId = turnId,
                    drawerId = drawerId,
                    selectedWord = null,
                    wordChoices = choices.map { FirebaseWordChoiceDto(it.id, it.word, it.difficulty.name) }
                )
                secretRef.setValue(secretDto) { error, _ ->
                    if (error != null) {
                        cont.resume(AppResult.Error(error.message))
                    } else {
                        cont.resume(AppResult.Success(Unit))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                cont.resume(AppResult.Error(error.message))
            }
        })
    }

    override suspend fun selectWord(
        roomId: String,
        drawerId: String,
        selectedWord: String,
        difficulty: WordDifficulty,
        drawTimeSeconds: Int
    ): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        val upperWord = selectedWord.trim().uppercase()
        val masked = MaskedWord.fromWord(upperWord)
        val wordHash = DefaultWordBankDataSource.hashWord(upperWord)
        val deadline = System.currentTimeMillis() + (drawTimeSeconds * 1000L)

        // 1. Update drawer secret record
        secretsRef.child(roomId).child(drawerId).child("selectedWord").setValue(upperWord)

        // 2. Update public game state
        val updates = mapOf(
            "phase" to GamePhase.DRAWING.name,
            "maskedWord" to masked.hint,
            "wordLength" to upperWord.length,
            "wordHash" to wordHash,
            "turnDeadline" to deadline,
            "revealedWord" to null
        )

        roomsRef.child(roomId).child("gameState").updateChildren(updates) { error, _ ->
            if (error != null) {
                cont.resume(AppResult.Error(error.message))
            } else {
                cont.resume(AppResult.Success(Unit))
            }
        }
    }

    override suspend fun endTurnAndAdvance(
        roomId: String,
        reason: String
    ): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        val gameStateRef = roomsRef.child(roomId).child("gameState")
        gameStateRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val gameState = currentData.getValue(FirebaseGameStateDto::class.java)
                    ?: return Transaction.abort()

                val turnOrder = gameState.turnOrder
                if (turnOrder.size < 2) {
                    currentData.child("phase").value = GamePhase.MATCH_RESULTS.name
                    return Transaction.success(currentData)
                }

                // 1. Calculate turn points and update cumulative scores
                val currentScores = gameState.scores.toMutableMap()
                val sortedGuessers = gameState.correctGuessers.entries.sortedBy { it.value }
                sortedGuessers.forEachIndexed { index, entry ->
                    val guesserId = entry.key
                    val pts = GameScoringEngine.calculateGuesserPoints(index + 1)
                    currentScores[guesserId] = (currentScores[guesserId] ?: 0) + pts
                }
                val drawerPoints = GameScoringEngine.calculateDrawerPoints(sortedGuessers.size)
                if (gameState.drawerId.isNotBlank()) {
                    currentScores[gameState.drawerId] = (currentScores[gameState.drawerId] ?: 0) + drawerPoints
                }
                currentData.child("scores").value = currentScores

                val currentTurnNumber = gameState.turnNumber
                val currentRound = gameState.round
                val totalRounds = gameState.totalRounds

                val nextTurnNumber = currentTurnNumber + 1
                val isRoundOver = (currentTurnNumber % turnOrder.size) == 0
                val nextRound = if (isRoundOver) currentRound + 1 else currentRound

                if (nextRound > totalRounds) {
                    currentData.child("phase").value = GamePhase.MATCH_RESULTS.name
                    return Transaction.success(currentData)
                }

                val nextDrawerIndex = (nextTurnNumber - 1) % turnOrder.size
                val nextDrawerId = turnOrder[nextDrawerIndex]
                val nextTurnId = "turn_$nextTurnNumber"

                if (isRoundOver) {
                    // Next round 5-second countdown transition
                    currentData.child("phase").value = GamePhase.COUNTDOWN.name
                    currentData.child("round").value = nextRound
                    currentData.child("turnDeadline").value = System.currentTimeMillis() + 5000L
                } else {
                    // Next turn within same round: 5-second word selection
                    currentData.child("phase").value = GamePhase.SELECTING_WORD.name
                    currentData.child("round").value = currentRound
                    currentData.child("turnDeadline").value = System.currentTimeMillis() + SELECTION_WINDOW_MS
                }

                currentData.child("turnNumber").value = nextTurnNumber
                currentData.child("turnId").value = nextTurnId
                currentData.child("drawerId").value = nextDrawerId
                currentData.child("maskedWord").value = ""
                currentData.child("wordLength").value = 0
                currentData.child("wordHash").value = ""
                currentData.child("revealedWord").value = null
                currentData.child("correctGuessers").value = null

                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e(TAG, "endTurnAndAdvance transaction failed: ${error.message}", error.toException())
                    cont.resume(AppResult.Error(error.message))
                } else if (!committed) {
                    cont.resume(AppResult.Error("Could not advance turn"))
                } else {
                    val phase = currentData?.child("phase")?.getValue(String::class.java)
                    if (phase == GamePhase.MATCH_RESULTS.name) {
                        roomsRef.child(roomId).child("status").setValue("FINISHED")
                    }
                    cont.resume(AppResult.Success(Unit))
                }
            }
        })
    }

    override suspend fun handlePlayerDeparture(
        roomId: String,
        departingPlayerId: String
    ): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        val playerRef = roomsRef.child(roomId).child("players").child(departingPlayerId)
        playerRef.removeValue { error, _ ->
            if (error != null) {
                cont.resume(AppResult.Error(error.message))
            } else {
                cont.resume(AppResult.Success(Unit))
            }
        }
    }

    override suspend fun cleanupGame(roomId: String): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        secretsRef.child(roomId).removeValue()
        roomsRef.child(roomId).child("drawing").removeValue()
        roomsRef.child(roomId).child("gameState").removeValue { error, _ ->
            if (error != null) {
                cont.resume(AppResult.Error(error.message))
            } else {
                cont.resume(AppResult.Success(Unit))
            }
        }
    }

    override suspend fun votePlayAgain(roomId: String, player: Player, vote: Boolean): AppResult<Unit> {
        return submitPlayAgainVote(roomId, vote)
    }

    override suspend fun restartGame(
        roomId: String,
        players: List<Player>,
        totalRounds: Int,
        drawTimeSeconds: Int,
        difficulty: WordDifficulty
    ): AppResult<Unit> {
        return initializeGame(
            roomId = roomId,
            players = players,
            totalRounds = totalRounds,
            drawTimeSeconds = drawTimeSeconds,
            difficulty = difficulty
        )
    }

    override fun observeReplayVoting(roomId: String): Flow<ReplayVotingState?> = callbackFlow {
        val ref = roomsRef.child(roomId).child("replayVoting")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }
                try {
                    val dto = snapshot.getValue(FirebaseReplayVotingDto::class.java)
                    if (dto != null) {
                        trySend(
                            ReplayVotingState(
                                status = dto.status,
                                deadline = dto.deadline,
                                votes = dto.votes,
                                voteCount = dto.voteCount,
                                requiredMajority = dto.requiredMajority
                            )
                        )
                    } else {
                        trySend(null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing replayVoting snapshot", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun submitPlayAgainVote(roomId: String, vote: Boolean): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        val data = hashMapOf(
            "roomId" to roomId,
            "vote" to vote
        )
        functions.getHttpsCallable("submitPlayAgainVote")
            .call(data)
            .addOnSuccessListener {
                cont.resume(AppResult.Success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "submitPlayAgainVote failed", e)
                cont.resume(AppResult.Error(e.message ?: "Failed to submit vote"))
            }
    }

    override suspend fun finalizeReplaySession(roomId: String): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        val data = hashMapOf(
            "roomId" to roomId
        )
        functions.getHttpsCallable("finalizeReplaySession")
            .call(data)
            .addOnSuccessListener {
                cont.resume(AppResult.Success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "finalizeReplaySession failed", e)
                cont.resume(AppResult.Error(e.message ?: "Failed to finalize session"))
            }
    }

    override suspend fun finalizeMatch(roomId: String): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        val data = hashMapOf(
            "roomId" to roomId
        )
        functions.getHttpsCallable("finalizeMatchStats")
            .call(data)
            .addOnSuccessListener {
                cont.resume(AppResult.Success(Unit))
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "finalizeMatch failed: ${e.message}", e)
                cont.resume(AppResult.Error(e.message ?: "Failed to finalize match"))
            }
    }
}
