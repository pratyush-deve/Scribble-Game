package com.pratyush.scribble.core.network.client

import com.pratyush.scribble.core.network.event.GameClientCommand
import com.pratyush.scribble.core.network.event.GameServerEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GameSocketClient {
    val isConnected: StateFlow<Boolean>
    val incomingEvents: Flow<GameServerEvent>

    suspend fun connect(serverUrl: String)
    suspend fun disconnect()
    suspend fun sendCommand(command: GameClientCommand)
}
