package com.pratyush.scribble.core.network.di

import com.google.firebase.database.FirebaseDatabase
import com.pratyush.scribble.core.network.repository.FirebaseGameRepository
import com.pratyush.scribble.core.network.repository.FirebaseRoomRepository
import com.pratyush.scribble.core.network.repository.FirestoreProfileRepository
import com.pratyush.scribble.core.network.repository.GameRepository
import com.pratyush.scribble.core.network.repository.ProfileRepository
import com.pratyush.scribble.core.network.repository.RoomRepository

object NetworkModule {
    @Volatile
    private var roomRepositoryInstance: RoomRepository? = null

    @Volatile
    private var gameRepositoryInstance: GameRepository? = null

    @Volatile
    private var profileRepositoryInstance: ProfileRepository? = null

    private const val RTDB_URL = "https://scribble-9bba6-default-rtdb.firebaseio.com"

    fun provideProfileRepository(): ProfileRepository {
        val current = profileRepositoryInstance
        if (current != null) return current
        return synchronized(this) {
            val nested = profileRepositoryInstance
            if (nested != null) {
                nested
            } else {
                val created = FirestoreProfileRepository()
                profileRepositoryInstance = created
                created
            }
        }
    }

    fun provideRoomRepository(): RoomRepository {
        val current = roomRepositoryInstance
        if (current != null) return current
        return synchronized(this) {
            val nested = roomRepositoryInstance
            if (nested != null) {
                nested
            } else {
                val created = FirebaseRoomRepository(FirebaseDatabase.getInstance(RTDB_URL))
                roomRepositoryInstance = created
                created
            }
        }
    }

    fun provideGameRepository(): GameRepository {
        val current = gameRepositoryInstance
        if (current != null) return current
        return synchronized(this) {
            val nested = gameRepositoryInstance
            if (nested != null) {
                nested
            } else {
                val created = FirebaseGameRepository(FirebaseDatabase.getInstance(RTDB_URL))
                gameRepositoryInstance = created
                created
            }
        }
    }

    fun setRoomRepositoryForTesting(repository: RoomRepository?) {
        roomRepositoryInstance = repository
    }

    fun setGameRepositoryForTesting(repository: GameRepository?) {
        gameRepositoryInstance = repository
    }

    fun setProfileRepositoryForTesting(repository: ProfileRepository?) {
        profileRepositoryInstance = repository
    }
}
