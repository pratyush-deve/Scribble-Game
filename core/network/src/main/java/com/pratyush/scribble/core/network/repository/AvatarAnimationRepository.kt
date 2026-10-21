package com.pratyush.scribble.core.network.repository

import com.pratyush.scribble.core.model.AvatarId

/**
 * Repository responsible for resolving the storage location and download URLs
 * for avatar Lottie animations stored in Firebase Cloud Storage.
 */
interface AvatarAnimationRepository {
    fun getAnimationUrl(avatarId: AvatarId): String
}

class FirebaseAvatarAnimationRepository(
    private val bucketName: String = "scribble-9bba6.firebasestorage.app"
) : AvatarAnimationRepository {

    override fun getAnimationUrl(avatarId: AvatarId): String {
        return "https://firebasestorage.googleapis.com/v0/b/$bucketName/o/avatar-animations%2F${avatarId.id}.json?alt=media"
    }
}
