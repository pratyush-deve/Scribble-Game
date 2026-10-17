package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class AvatarId(val id: String, val displayName: String, val emoji: String) {
    CAT("cat", "Cool Cat", "🐱"),
    DOG("dog", "Dapper Dog", "🐶"),
    FOX("fox", "Foxy", "🦊"),
    PANDA("panda", "Chubby Panda", "🐼"),
    KOALA("koala", "Lazy Koala", "🐨"),
    LION("lion", "Brave Lion", "🦁"),
    TIGER("tiger", "Swift Tiger", "🐯"),
    MONKEY("monkey", "Cheeky Monkey", "🐵"),
    BEAR("bear", "Grizzly Bear", "🐻"),
    RABBIT("rabbit", "Speedy Bunny", "🐰"),
    PENGUIN("penguin", "Gentle Penguin", "🐧"),
    OWL("owl", "Wise Owl", "🦉");

    companion object {
        fun fromId(id: String): AvatarId = entries.firstOrNull { it.id == id } ?: CAT
    }
}
