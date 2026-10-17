package com.pratyush.scribble.core.network.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class FirebasePlayerDto(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("avatarId") @set:PropertyName("avatarId") var avatarId: String = "CAT",
    @get:PropertyName("isHost") @set:PropertyName("isHost") var isHost: Boolean = false,
    @get:PropertyName("score") @set:PropertyName("score") var score: Int = 0,
    @get:PropertyName("joinedAt") @set:PropertyName("joinedAt") var joinedAt: Long = 0L,
    @get:PropertyName("isConnected") @set:PropertyName("isConnected") var isConnected: Boolean = true
)

@IgnoreExtraProperties
data class FirebaseRoomSettingsDto(
    @get:PropertyName("maxPlayers") @set:PropertyName("maxPlayers") var maxPlayers: Int = 8,
    @get:PropertyName("rounds") @set:PropertyName("rounds") var rounds: Int = 3,
    @get:PropertyName("drawTimeSeconds") @set:PropertyName("drawTimeSeconds") var drawTimeSeconds: Int = 60,
    @get:PropertyName("wordDifficulty") @set:PropertyName("wordDifficulty") var wordDifficulty: String = "MEDIUM"
)

@IgnoreExtraProperties
data class FirebaseRoomDto(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("code") @set:PropertyName("code") var code: String = "",
    @get:PropertyName("hostId") @set:PropertyName("hostId") var hostId: String = "",
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "WAITING",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long = 0L,
    @get:PropertyName("lastActivityAt") @set:PropertyName("lastActivityAt") var lastActivityAt: Long = 0L,
    @get:PropertyName("settings") @set:PropertyName("settings") var settings: FirebaseRoomSettingsDto = FirebaseRoomSettingsDto(),
    @get:PropertyName("players") @set:PropertyName("players") var players: Map<String, FirebasePlayerDto> = emptyMap()
)
