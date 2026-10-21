package com.pratyush.scribble.core.network.repository

import com.pratyush.scribble.core.model.AvatarId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarAnimationRepositoryTest {

    private val repository: AvatarAnimationRepository = FirebaseAvatarAnimationRepository()

    @Test
    fun getAnimationUrl_returnsValidFirebaseStorageUrlForEveryAvatar() {
        AvatarId.entries.forEach { avatar ->
            val url = repository.getAnimationUrl(avatar)
            assertTrue("URL should point to scribble-9bba6.firebasestorage.app", url.contains("scribble-9bba6.firebasestorage.app"))
            assertTrue("URL should include avatar-animations path", url.contains("avatar-animations%2F"))
            assertTrue("URL should contain lowercase avatar id", url.endsWith("${avatar.id}.json?alt=media"))
        }
    }

    @Test
    fun getAnimationUrl_formatsCatCorrectly() {
        val url = repository.getAnimationUrl(AvatarId.CAT)
        assertEquals(
            "https://firebasestorage.googleapis.com/v0/b/scribble-9bba6.firebasestorage.app/o/avatar-animations%2Fcat.json?alt=media",
            url
        )
    }
}
