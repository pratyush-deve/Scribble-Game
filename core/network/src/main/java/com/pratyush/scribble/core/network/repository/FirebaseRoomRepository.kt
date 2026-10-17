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
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.GamePhase
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.Room
import com.pratyush.scribble.core.model.RoomCode
import com.pratyush.scribble.core.model.RoomSettings
import com.pratyush.scribble.core.model.WordDifficulty
import com.pratyush.scribble.core.network.model.FirebasePlayerDto
import com.pratyush.scribble.core.network.model.FirebaseRoomDto
import com.pratyush.scribble.core.network.model.FirebaseRoomSettingsDto
import com.pratyush.scribble.core.network.util.RoomCodeGenerator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Production implementation of [RoomRepository] backed by Firebase Realtime Database.
 */
class FirebaseRoomRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) : RoomRepository {

    private val roomsRef: DatabaseReference = database.getReference("rooms")
    private val roomCodesRef: DatabaseReference = database.getReference("roomCodes")

    override suspend fun createRoom(
        host: Player,
        settings: RoomSettings
    ): AppResult<Room> {
        val maxAttempts = 5
        var attempt = 0
        var generatedCode = ""
        var assignedRoomId = ""

        while (attempt < maxAttempts) {
            attempt++
            generatedCode = RoomCodeGenerator.generateCode(6)
            val candidateRoomId = roomsRef.push().key ?: continue

            val claimed = claimRoomCodeAtomically(generatedCode, candidateRoomId)
            if (claimed) {
                assignedRoomId = candidateRoomId
                break
            }
        }

        if (assignedRoomId.isEmpty()) {
            return AppResult.Error("Failed to claim a unique room code. Please try again.")
        }

        val now = System.currentTimeMillis()
        val roomDto = FirebaseRoomDto(
            id = assignedRoomId,
            code = generatedCode,
            hostId = host.id,
            status = "WAITING",
            createdAt = now,
            lastActivityAt = now,
            settings = FirebaseRoomSettingsDto(
                maxPlayers = settings.maxPlayers,
                rounds = settings.rounds,
                drawTimeSeconds = settings.drawTimeSeconds,
                wordDifficulty = settings.wordDifficulty.name
            ),
            players = mapOf(
                host.id to FirebasePlayerDto(
                    id = host.id,
                    name = host.name,
                    avatarId = host.avatarId.name,
                    isHost = true,
                    score = 0,
                    joinedAt = now,
                    isConnected = true
                )
            )
        )

        return try {
            val setSuccess = suspendCancellableCoroutine<Boolean> { continuation ->
                roomsRef.child(assignedRoomId).setValue(roomDto) { error, _ ->
                    if (error != null) {
                        continuation.resume(false)
                    } else {
                        continuation.resume(true)
                    }
                }
            }

            if (setSuccess) {
                AppResult.Success(roomDto.toDomainRoom())
            } else {
                roomCodesRef.child(generatedCode).removeValue()
                AppResult.Error("Failed to save room data")
            }
        } catch (e: Exception) {
            roomCodesRef.child(generatedCode).removeValue()
            AppResult.Error(e.localizedMessage ?: "Failed to create room")
        }
    }

    private suspend fun claimRoomCodeAtomically(code: String, roomId: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val codeRef = roomCodesRef.child(code)
            codeRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val existing = currentData.getValue(String::class.java)
                    return if (existing == null) {
                        currentData.value = roomId
                        Transaction.success(currentData)
                    } else {
                        Transaction.abort()
                    }
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null || !committed) {
                        continuation.resume(false)
                    } else {
                        continuation.resume(true)
                    }
                }
            })
        }
    }

    override suspend fun resolveRoomId(roomCode: String): String? {
        val normalizedCode = RoomCodeGenerator.normalize(roomCode)
        if (!RoomCodeGenerator.isValid(normalizedCode)) return null

        return suspendCancellableCoroutine { continuation ->
            roomCodesRef.child(normalizedCode).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    continuation.resume(snapshot.getValue(String::class.java))
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(null)
                }
            })
        }
    }

    override suspend fun joinRoom(roomCode: String, player: Player): AppResult<Room> {
        val normalizedCode = RoomCodeGenerator.normalize(roomCode)
        if (!RoomCodeGenerator.isValid(normalizedCode)) {
            return AppResult.Error("Invalid room code format")
        }

        // 1. Resolve roomId from roomCode with timeout
        val roomId = withTimeoutOrNull(7000L) {
            resolveRoomId(normalizedCode)
        } ?: return AppResult.Error("ROOM_NOT_FOUND")

        // 2. Perform narrow path join on /rooms/$roomId/players/$playerId
        return executeJoin(roomId, player)
    }

    private suspend fun executeJoin(roomId: String, player: Player): AppResult<Room> {
        return suspendCancellableCoroutine { continuation ->
            val roomRef = roomsRef.child(roomId)
            roomRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        continuation.resume(AppResult.Error("ROOM_NOT_FOUND"))
                        return
                    }

                    val status = snapshot.child("status").getValue(String::class.java) ?: "WAITING"
                    if (status != "WAITING") {
                        continuation.resume(AppResult.Error("ROOM_CLOSED"))
                        return
                    }

                    val maxPlayers = snapshot.child("settings").child("maxPlayers").getValue(Int::class.java) ?: 8
                    val playersSnap = snapshot.child("players")
                    val isAlreadyMember = playersSnap.hasChild(player.id)

                    if (!isAlreadyMember && playersSnap.childrenCount >= maxPlayers) {
                        continuation.resume(AppResult.Error("ROOM_FULL"))
                        return
                    }

                    val now = System.currentTimeMillis()
                    val existingPlayer = playersSnap.child(player.id)
                    val isHost = existingPlayer.child("isHost").getValue(Boolean::class.java) ?: false
                    val score = existingPlayer.child("score").getValue(Int::class.java) ?: 0
                    val joinedAt = existingPlayer.child("joinedAt").getValue(Long::class.java) ?: now

                    val playerDto = FirebasePlayerDto(
                        id = player.id,
                        name = player.name,
                        avatarId = player.avatarId.name,
                        isHost = isHost,
                        score = score,
                        joinedAt = joinedAt,
                        isConnected = true
                    )

                    val playerRef = roomRef.child("players").child(player.id)
                    playerRef.setValue(playerDto) { error, _ ->
                        if (error != null) {
                            continuation.resume(AppResult.Error(error.message))
                        } else {
                            val domainRoom = parseRoomSnapshot(snapshot, additionalPlayer = playerDto)
                            continuation.resume(AppResult.Success(domainRoom))
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(AppResult.Error("NETWORK_ERROR: ${error.message}"))
                }
            })
        }
    }

    override fun observeRoom(roomId: String): Flow<Room?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }
                val status = snapshot.child("status").getValue(String::class.java) ?: "WAITING"
                if (status == "CLOSED") {
                    trySend(null)
                    return
                }

                try {
                    val room = parseRoomSnapshot(snapshot)
                    trySend(room)
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseRoomRepository", "Error parsing room snapshot", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val ref = roomsRef.child(roomId)
        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    override suspend fun leaveRoom(roomId: String, playerId: String): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        val roomRef = roomsRef.child(roomId)
        roomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    cont.resume(AppResult.Success(Unit))
                    return
                }
                val hostId = snapshot.child("hostId").getValue(String::class.java)
                val code = snapshot.child("code").getValue(String::class.java)

                if (hostId == playerId) {
                    // Host leaves: close room and remove room code
                    if (!code.isNullOrBlank()) {
                        roomCodesRef.child(code).removeValue()
                    }
                    roomRef.removeValue { error, _ ->
                        if (error != null) {
                            cont.resume(AppResult.Error(error.message))
                        } else {
                            cont.resume(AppResult.Success(Unit))
                        }
                    }
                } else {
                    // Guest leaves: remove guest from /rooms/$roomId/players/$playerId
                    roomRef.child("players").child(playerId).removeValue { error, _ ->
                        if (error != null) {
                            cont.resume(AppResult.Error(error.message))
                        } else {
                            cont.resume(AppResult.Success(Unit))
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                cont.resume(AppResult.Error("NETWORK_ERROR: ${error.message}"))
            }
        })
    }

    override suspend fun updateSettings(
        roomId: String,
        requesterId: String,
        settings: RoomSettings
    ): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        val roomRef = roomsRef.child(roomId)
        roomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    cont.resume(AppResult.Error("ROOM_NOT_FOUND"))
                    return
                }
                val hostId = snapshot.child("hostId").getValue(String::class.java)
                if (hostId != requesterId) {
                    cont.resume(AppResult.Error("Only the host can modify room settings"))
                    return
                }

                val settingsDto = FirebaseRoomSettingsDto(
                    maxPlayers = settings.maxPlayers,
                    rounds = settings.rounds,
                    drawTimeSeconds = settings.drawTimeSeconds,
                    wordDifficulty = settings.wordDifficulty.name
                )

                roomRef.child("settings").setValue(settingsDto) { error, _ ->
                    if (error != null) {
                        cont.resume(AppResult.Error(error.message))
                    } else {
                        cont.resume(AppResult.Success(Unit))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                cont.resume(AppResult.Error("NETWORK_ERROR: ${error.message}"))
            }
        })
    }

    override suspend fun updatePlayerAvatar(
        roomId: String,
        playerId: String,
        avatarId: AvatarId
    ): AppResult<Unit> {
        return suspendCancellableCoroutine { continuation ->
            val avatarRef = roomsRef.child(roomId).child("players").child(playerId).child("avatarId")
            avatarRef.setValue(avatarId.name) { error, _ ->
                if (error != null) {
                    continuation.resume(AppResult.Error(error.message))
                } else {
                    continuation.resume(AppResult.Success(Unit))
                }
            }
        }
    }

    override suspend fun updatePlayerName(
        roomId: String,
        playerId: String,
        name: String
    ): AppResult<Unit> {
        return suspendCancellableCoroutine { continuation ->
            val nameRef = roomsRef.child(roomId).child("players").child(playerId).child("name")
            nameRef.setValue(name) { error, _ ->
                if (error != null) {
                    continuation.resume(AppResult.Error(error.message))
                } else {
                    continuation.resume(AppResult.Success(Unit))
                }
            }
        }
    }

    override suspend fun updatePlayerProfile(
        roomId: String,
        playerId: String,
        name: String,
        avatarId: AvatarId
    ): AppResult<Unit> {
        val nameResult = updatePlayerName(roomId, playerId, name)
        if (nameResult !is AppResult.Success) return nameResult
        return updatePlayerAvatar(roomId, playerId, avatarId)
    }

    override suspend fun startGame(roomId: String, requesterId: String): AppResult<Unit> = suspendCancellableCoroutine { cont ->
        val roomRef = roomsRef.child(roomId)
        roomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    cont.resume(AppResult.Error("ROOM_NOT_FOUND"))
                    return
                }
                val hostId = snapshot.child("hostId").getValue(String::class.java)
                if (hostId != requesterId) {
                    cont.resume(AppResult.Error("Only the host can start the game"))
                    return
                }

                val playersSnap = snapshot.child("players")
                if (playersSnap.childrenCount < 2) {
                    cont.resume(AppResult.Error("At least 2 players are required to start"))
                    return
                }

                roomRef.child("status").setValue("IN_PROGRESS") { error, _ ->
                    if (error != null) {
                        cont.resume(AppResult.Error(error.message))
                    } else {
                        cont.resume(AppResult.Success(Unit))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                cont.resume(AppResult.Error("NETWORK_ERROR: ${error.message}"))
            }
        })
    }

    override fun registerPresence(roomId: String, playerId: String) {
        val playerRef = roomsRef.child(roomId).child("players").child(playerId)
        val connectedRef = database.getReference(".info/connected")

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isConnected = snapshot.getValue(Boolean::class.java) ?: false
                if (isConnected) {
                    // When connection is lost, mark isConnected = false
                    playerRef.child("isConnected").onDisconnect().setValue(false)
                    // Mark connected now
                    playerRef.child("isConnected").setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) = Unit
        })
    }

    private fun parseRoomSnapshot(snapshot: DataSnapshot, additionalPlayer: FirebasePlayerDto? = null): Room {
        val id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key.orEmpty()
        val code = snapshot.child("code").getValue(String::class.java).orEmpty()
        val hostId = snapshot.child("hostId").getValue(String::class.java).orEmpty()
        val status = snapshot.child("status").getValue(String::class.java) ?: "WAITING"

        val settingsSnap = snapshot.child("settings")
        val maxPlayers = settingsSnap.child("maxPlayers").getValue(Int::class.java) ?: 8
        val rounds = settingsSnap.child("rounds").getValue(Int::class.java) ?: 3
        val drawTimeSeconds = settingsSnap.child("drawTimeSeconds").getValue(Int::class.java) ?: 60
        val difficultyStr = settingsSnap.child("wordDifficulty").getValue(String::class.java) ?: "MEDIUM"
        val difficulty = runCatching { WordDifficulty.valueOf(difficultyStr) }.getOrDefault(WordDifficulty.MEDIUM)

        val players = mutableListOf<Player>()
        val playersSnap = snapshot.child("players")
        for (child in playersSnap.children) {
            val childKey = child.key ?: continue
            val playerId = child.child("id").getValue(String::class.java)?.takeIf { it.isNotBlank() } ?: childKey
            val name = child.child("name").getValue(String::class.java)?.takeIf { it.isNotBlank() } ?: "Player"
            val avatarStr = child.child("avatarId").getValue(String::class.java) ?: "CAT"
            val avatarId = runCatching { AvatarId.valueOf(avatarStr) }.getOrDefault(AvatarId.CAT)
            val isHost = child.child("isHost").getValue(Boolean::class.java) ?: (playerId == hostId)
            val score = child.child("score").getValue(Int::class.java) ?: 0
            val isConnected = child.child("isConnected").getValue(Boolean::class.java) ?: true

            players.add(
                Player(
                    id = playerId,
                    name = name,
                    avatarId = avatarId,
                    isHost = isHost,
                    score = score,
                    isConnected = isConnected
                )
            )
        }

        if (additionalPlayer != null && players.none { it.id == additionalPlayer.id }) {
            players.add(
                Player(
                    id = additionalPlayer.id,
                    name = additionalPlayer.name,
                    avatarId = runCatching { AvatarId.valueOf(additionalPlayer.avatarId) }.getOrDefault(AvatarId.CAT),
                    isHost = additionalPlayer.isHost,
                    score = additionalPlayer.score,
                    isConnected = additionalPlayer.isConnected
                )
            )
        }

        players.sortWith(compareByDescending<Player> { it.isHost }.thenBy { it.name })

        val phase = when (status) {
            "WAITING" -> GamePhase.WAITING_FOR_PLAYERS
            "IN_PROGRESS" -> GamePhase.DRAWING
            "FINISHED" -> GamePhase.MATCH_RESULTS
            else -> GamePhase.WAITING_FOR_PLAYERS
        }

        return Room(
            id = id,
            code = RoomCode(code),
            hostId = hostId,
            players = players,
            settings = RoomSettings(
                maxPlayers = maxPlayers,
                rounds = rounds,
                drawTimeSeconds = drawTimeSeconds,
                wordDifficulty = difficulty
            ),
            phase = phase
        )
    }

    private fun FirebaseRoomDto.toDomainRoom(): Room {
        val domainPlayers = players.values.map { p ->
            Player(
                id = p.id,
                name = p.name,
                avatarId = runCatching { AvatarId.valueOf(p.avatarId) }.getOrDefault(AvatarId.CAT),
                isHost = p.isHost,
                score = p.score,
                isConnected = p.isConnected
            )
        }.sortedWith(compareByDescending<Player> { it.isHost }.thenBy { it.name })

        val domainPhase = when (status) {
            "WAITING" -> GamePhase.WAITING_FOR_PLAYERS
            "IN_PROGRESS" -> GamePhase.DRAWING
            "FINISHED" -> GamePhase.MATCH_RESULTS
            else -> GamePhase.WAITING_FOR_PLAYERS
        }

        val domainDifficulty = runCatching {
            WordDifficulty.valueOf(settings.wordDifficulty)
        }.getOrDefault(WordDifficulty.MEDIUM)

        return Room(
            id = id,
            code = RoomCode(code),
            hostId = hostId,
            players = domainPlayers,
            settings = RoomSettings(
                maxPlayers = settings.maxPlayers,
                rounds = settings.rounds,
                drawTimeSeconds = settings.drawTimeSeconds,
                wordDifficulty = domainDifficulty
            ),
            phase = domainPhase
        )
    }

    override suspend fun closeAndCleanupRoom(roomId: String, roomCode: String): AppResult<Unit> = suspendCancellableCoroutine { continuation ->
        roomCodesRef.child(roomCode).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mappedRoomId = snapshot.getValue(String::class.java)
                if (mappedRoomId == roomId) {
                    roomCodesRef.child(roomCode).removeValue()
                }

                roomsRef.child(roomId).child("status").setValue("CLOSED") { statusErr, _ ->
                    if (statusErr != null) {
                        Log.w("FirebaseRoomRepository", "Failed to set status CLOSED: ${statusErr.message}")
                    }

                    roomsRef.child(roomId).removeValue { roomErr, _ ->
                        if (roomErr != null) {
                            continuation.resume(AppResult.Error(roomErr.message))
                        } else {
                            continuation.resume(AppResult.Success(Unit))
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resume(AppResult.Error(error.message))
            }
        })
    }
}
