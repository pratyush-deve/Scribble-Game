package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class RoomCode(val value: String) {
    init {
        require(value.matches(Regex("^[A-Z0-9]{4,6}$"))) {
            "Room code must be 4 to 6 uppercase alphanumeric characters: $value"
        }
    }

    override fun toString(): String = value

    companion object {
        fun isValid(code: String): Boolean = code.trim().uppercase().matches(Regex("^[A-Z0-9]{4,6}$"))
    }
}
