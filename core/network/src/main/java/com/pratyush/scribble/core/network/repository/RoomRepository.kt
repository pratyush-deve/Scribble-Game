package com.pratyush.scribble.core.network.repository

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.Room
import com.pratyush.scribble.core.model.RoomSettings
import kotlinx.coroutines.flow.Flow

interface RoomRepository {
    suspend fun createRoom(host: Player, settings: RoomSettings = RoomSettings()): AppResult<Room>
    suspend fun joinRoom(roomCode: String, player: Player): AppResult<Room>
    suspend fun resolveRoomId(roomCode: String): String?
    fun observeRoom(roomId: String): Flow<Room?>
    suspend fun leaveRoom(roomId: String, playerId: String): AppResult<Unit>
    suspend fun updateSettings(roomId: String, requesterId: String, settings: RoomSettings): AppResult<Unit>
    suspend fun updatePlayerAvatar(roomId: String, playerId: String, avatarId: AvatarId): AppResult<Unit>
    suspend fun updatePlayerName(roomId: String, playerId: String, name: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun updatePlayerProfile(roomId: String, playerId: String, name: String, avatarId: AvatarId): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun startGame(roomId: String, requesterId: String): AppResult<Unit>
    fun registerPresence(roomId: String, playerId: String)
    suspend fun closeAndCleanupRoom(roomId: String, roomCode: String): AppResult<Unit>
}
