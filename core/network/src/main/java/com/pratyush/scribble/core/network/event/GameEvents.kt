package com.pratyush.scribble.core.network.event

import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.ChatMessage
import com.pratyush.scribble.core.model.DrawingStroke
import com.pratyush.scribble.core.model.GamePhase
import com.pratyush.scribble.core.model.GuessResult
import com.pratyush.scribble.core.model.Player
import com.pratyush.scribble.core.model.Room
import com.pratyush.scribble.core.model.RoomSettings
import com.pratyush.scribble.core.model.WordChoice
import kotlinx.serialization.Serializable

/**
 * Commands sent from client to server.
 */
@Serializable
sealed interface GameClientCommand {
    @Serializable
    data class CreateRoom(val hostName: String, val avatarId: AvatarId) : GameClientCommand

    @Serializable
    data class JoinRoom(val roomCode: String, val playerName: String, val avatarId: AvatarId) : GameClientCommand

    @Serializable
    data class LeaveRoom(val roomCode: String) : GameClientCommand

    @Serializable
    data class UpdateRoomSettings(val roomCode: String, val settings: RoomSettings) : GameClientCommand

    @Serializable
    data class StartGame(val roomCode: String) : GameClientCommand

    @Serializable
    data class SelectWord(val roomCode: String, val word: String) : GameClientCommand

    @Serializable
    data class SendStroke(val roomCode: String, val stroke: DrawingStroke) : GameClientCommand

    @Serializable
    data class UndoStroke(val roomCode: String) : GameClientCommand

    @Serializable
    data class ClearCanvas(val roomCode: String) : GameClientCommand

    @Serializable
    data class SendChatMessage(val roomCode: String, val text: String) : GameClientCommand
}

/**
 * Events sent from server to client.
 */
@Serializable
sealed interface GameServerEvent {
    @Serializable
    data class RoomJoined(val room: Room, val localPlayerId: String) : GameServerEvent

    @Serializable
    data class PlayerJoined(val player: Player) : GameServerEvent

    @Serializable
    data class PlayerLeft(val playerId: String) : GameServerEvent

    @Serializable
    data class LobbyUpdated(val room: Room) : GameServerEvent

    @Serializable
    data class GameStarted(val room: Room) : GameServerEvent

    @Serializable
    data class WordChoicesOffered(val choices: List<WordChoice>, val timeoutSeconds: Int) : GameServerEvent

    @Serializable
    data class RoundStarted(
        val roundNumber: Int,
        val drawerId: String,
        val maskedHint: String,
        val durationSeconds: Int
    ) : GameServerEvent

    @Serializable
    data class StrokeReceived(val stroke: DrawingStroke) : GameServerEvent

    @Serializable
    data object CanvasCleared : GameServerEvent

    @Serializable
    data object StrokeUndone : GameServerEvent

    @Serializable
    data class MessageReceived(val message: ChatMessage) : GameServerEvent

    @Serializable
    data class GuessEvaluated(val guessResult: GuessResult) : GameServerEvent

    @Serializable
    data class TimeTick(val secondsRemaining: Int) : GameServerEvent

    @Serializable
    data class RoundEnded(val word: String, val scores: List<Player>) : GameServerEvent

    @Serializable
    data class MatchEnded(val finalScores: List<Player>, val winnerId: String) : GameServerEvent

    @Serializable
    data class ErrorOccurred(val message: String) : GameServerEvent
}
