package com.pratyush.scribble.core.network.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AccountType
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.MatchRecord
import com.pratyush.scribble.core.model.UserPreferences
import com.pratyush.scribble.core.model.UserProfile
import com.pratyush.scribble.core.model.UserStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ProfileRepository {

    private val usersCollection = firestore.collection(USERS_COLLECTION)
    private val matchesCollection = firestore.collection(MATCHES_COLLECTION)

    override suspend fun getProfile(uid: String): AppResult<UserProfile?> = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext AppResult.Error("User ID cannot be blank")
        try {
            val snapshot = usersCollection.document(uid).get().await()
            AppResult.Success(snapshot.toUserProfile())
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching profile for $uid", e)
            AppResult.Error(e.localizedMessage ?: "Failed to load profile")
        }
    }

    override suspend fun getOrCreateProfile(
        uid: String,
        defaultDisplayName: String,
        defaultAvatarId: AvatarId,
        email: String?,
        accountType: AccountType
    ): AppResult<UserProfile> = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext AppResult.Error("User ID cannot be blank")
        try {
            val docRef = usersCollection.document(uid)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val profile = snapshot.toUserProfile()
                if (profile != null) {
                    // Update lastSeenAt without touching custom name, avatar, or stats
                    docRef.update("lastSeenAt", System.currentTimeMillis()).await()
                    return@withContext AppResult.Success(profile.copy(lastSeenAt = System.currentTimeMillis()))
                }
            }

            // Create initial profile with defaults
            val now = System.currentTimeMillis()
            val initialDisplayName = defaultDisplayName.trim().ifBlank { "Player_${uid.take(4)}" }
            val initialStats = UserStats()
            val initialPreferences = UserPreferences()

            val data = hashMapOf<String, Any?>(
                "uid" to uid,
                "displayName" to initialDisplayName,
                "avatarId" to defaultAvatarId.id,
                "accountType" to accountType.name,
                "guestMatchesCompleted" to 0,
                "guestAccessStatus" to "ACTIVE",
                "createdAt" to now,
                "updatedAt" to now,
                "lastSeenAt" to now,
                "stats" to hashMapOf(
                    "gamesPlayed" to initialStats.gamesPlayed,
                    "wins" to initialStats.wins,
                    "losses" to initialStats.losses,
                    "draws" to initialStats.draws,
                    "totalScore" to initialStats.totalScore,
                    "bestScore" to initialStats.bestScore,
                    "winStreak" to initialStats.winStreak
                ),
                "preferences" to hashMapOf(
                    "soundEnabled" to initialPreferences.soundEnabled
                )
            )

            docRef.set(data).await()

            val created = UserProfile(
                id = uid,
                displayName = initialDisplayName,
                email = email,
                avatarId = defaultAvatarId,
                accountType = accountType,
                guestMatchesCompleted = 0,
                guestAccessStatus = "ACTIVE",
                createdAt = now,
                updatedAt = now,
                lastSeenAt = now,
                stats = initialStats,
                preferences = initialPreferences
            )
            AppResult.Success(created)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getOrCreateProfile for $uid", e)
            AppResult.Error(e.localizedMessage ?: "Failed to get or create profile")
        }
    }

    override fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listenerRegistration = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for user profile $uid", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toUserProfile())
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    override suspend fun updateDisplayName(uid: String, displayName: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        val trimmed = displayName.trim()
        if (trimmed.isEmpty() || trimmed.length > 25) {
            return@withContext AppResult.Error("Display name must be between 1 and 25 characters")
        }

        try {
            usersCollection.document(uid).update(
                mapOf(
                    "displayName" to trimmed,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating displayName for $uid", e)
            AppResult.Error(e.localizedMessage ?: "Failed to update display name")
        }
    }

    override suspend fun updateAvatar(uid: String, avatarId: AvatarId): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            usersCollection.document(uid).update(
                mapOf(
                    "avatarId" to avatarId.id,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating avatar for $uid", e)
            AppResult.Error(e.localizedMessage ?: "Failed to update avatar")
        }
    }

    override suspend fun updateProfile(
        uid: String,
        displayName: String,
        avatarId: AvatarId
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        val trimmed = displayName.trim()
        if (trimmed.isEmpty() || trimmed.length > 25) {
            return@withContext AppResult.Error("Display name must be between 1 and 25 characters")
        }

        try {
            usersCollection.document(uid).update(
                mapOf(
                    "displayName" to trimmed,
                    "avatarId" to avatarId.id,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile for $uid", e)
            AppResult.Error(e.localizedMessage ?: "Failed to update profile")
        }
    }

    override suspend fun updateLastSeen(uid: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            usersCollection.document(uid).update(
                "lastSeenAt", System.currentTimeMillis()
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating lastSeenAt for $uid", e)
            AppResult.Error(e.localizedMessage ?: "Failed to update last seen")
        }
    }

    override suspend fun getMatchHistory(uid: String): AppResult<List<MatchRecord>> = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext AppResult.Success(emptyList())
        try {
            val querySnapshot = matchesCollection
                .whereArrayContains("playerUids", uid)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()

            val records = querySnapshot.documents.mapNotNull { doc ->
                doc.toMatchRecord()
            }
            AppResult.Success(records)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting match history for $uid", e)
            AppResult.Error(e.localizedMessage ?: "Failed to load match history")
        }
    }

    override suspend fun upgradeGuestToPermanent(uid: String, email: String?): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext AppResult.Error("User ID cannot be blank")
        try {
            val now = System.currentTimeMillis()
            val updates = mutableMapOf<String, Any?>(
                "accountType" to AccountType.PERMANENT.name,
                "guestAccessStatus" to "PERMANENT",
                "updatedAt" to now,
                "lastSeenAt" to now
            )
            if (!email.isNullOrBlank()) {
                updates["email"] = email
            }
            usersCollection.document(uid).update(updates).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error upgrading guest to permanent for $uid", e)
            AppResult.Error(e.localizedMessage ?: "Failed to upgrade guest account")
        }
    }

    override suspend fun checkGuestMatchEligibility(uid: String): AppResult<Boolean> = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext AppResult.Error("User ID cannot be blank")
        try {
            val snapshot = usersCollection.document(uid).get().await()
            if (!snapshot.exists()) {
                // New user: eligible
                return@withContext AppResult.Success(true)
            }
            val profile = snapshot.toUserProfile()
            if (profile == null) {
                return@withContext AppResult.Success(true)
            }
            if (profile.isGuest && profile.isGuestLimitReached) {
                return@withContext AppResult.Success(false)
            }
            AppResult.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking guest eligibility for $uid", e)
            AppResult.Error(e.localizedMessage ?: "Failed to verify guest eligibility")
        }
    }

    private fun DocumentSnapshot.toUserProfile(): UserProfile? {
        if (!exists()) return null
        val uid = getString("uid") ?: id
        val displayName = getString("displayName") ?: "Player_${uid.take(4)}"
        val avatarIdStr = getString("avatarId") ?: "cat"
        val avatar = AvatarId.fromId(avatarIdStr)
        val email = getString("email")
        val accountTypeStr = getString("accountType") ?: "PERMANENT"
        val accountType = try {
            AccountType.valueOf(accountTypeStr)
        } catch (e: Exception) {
            AccountType.PERMANENT
        }
        val guestMatchesCompleted = getLong("guestMatchesCompleted")?.toInt() ?: 0
        val guestAccessStatus = getString("guestAccessStatus") ?: "ACTIVE"
        val createdAt = getLong("createdAt") ?: 0L
        val updatedAt = getLong("updatedAt") ?: 0L
        val lastSeenAt = getLong("lastSeenAt") ?: 0L

        val statsMap = get("stats") as? Map<*, *>
        val stats = UserStats(
            gamesPlayed = (statsMap?.get("gamesPlayed") as? Number)?.toInt() ?: 0,
            wins = (statsMap?.get("wins") as? Number)?.toInt() ?: 0,
            losses = (statsMap?.get("losses") as? Number)?.toInt() ?: 0,
            draws = (statsMap?.get("draws") as? Number)?.toInt() ?: 0,
            totalScore = (statsMap?.get("totalScore") as? Number)?.toInt() ?: 0,
            bestScore = (statsMap?.get("bestScore") as? Number)?.toInt() ?: 0,
            winStreak = (statsMap?.get("winStreak") as? Number)?.toInt() ?: 0
        )

        val prefMap = get("preferences") as? Map<*, *>
        val preferences = UserPreferences(
            soundEnabled = (prefMap?.get("soundEnabled") as? Boolean) ?: true
        )

        return UserProfile(
            id = uid,
            displayName = displayName,
            email = email,
            avatarId = avatar,
            accountType = accountType,
            guestMatchesCompleted = guestMatchesCompleted,
            guestAccessStatus = guestAccessStatus,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastSeenAt = lastSeenAt,
            stats = stats,
            preferences = preferences
        )
    }

    private fun DocumentSnapshot.toMatchRecord(): MatchRecord? {
        if (!exists()) return null
        val matchId = getString("matchId") ?: id
        val completedAt = getLong("completedAt") ?: 0L
        @Suppress("UNCHECKED_CAST")
        val playerUids = get("playerUids") as? List<String> ?: emptyList()
        val scoresMap = get("scores") as? Map<*, *>
        val scores = scoresMap?.mapNotNull { (k, v) ->
            if (k is String && v is Number) k to v.toInt() else null
        }?.toMap() ?: emptyMap()
        val winnerUid = getString("winnerUid")
        val isDraw = getBoolean("isDraw") ?: false
        val rounds = getLong("rounds")?.toInt() ?: 3
        val drawTimeSeconds = getLong("drawTimeSeconds")?.toInt() ?: 60

        return MatchRecord(
            matchId = matchId,
            completedAt = completedAt,
            playerUids = playerUids,
            scores = scores,
            winnerUid = winnerUid,
            isDraw = isDraw,
            rounds = rounds,
            drawTimeSeconds = drawTimeSeconds
        )
    }

    companion object {
        private const val TAG = "FirestoreProfileRepo"
        const val USERS_COLLECTION = "users"
        const val MATCHES_COLLECTION = "matches"
    }
}
