package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChatMessageType {
    CHAT,
    CORRECT_GUESS,
    CLOSE_GUESS,
    SYSTEM_INFO
}

@Serializable
data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: ChatMessageType = ChatMessageType.CHAT
)
