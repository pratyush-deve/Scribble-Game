package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String? = null,
    val avatarId: AvatarId = AvatarId.CAT,
    val accountType: AccountType = AccountType.PERMANENT,
    val guestMatchesCompleted: Int = 0,
    val guestAccessStatus: String = "ACTIVE",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val lastSeenAt: Long = 0L,
    val stats: UserStats = UserStats(),
    val preferences: UserPreferences = UserPreferences()
) {
    constructor(
        id: String,
        displayName: String,
        email: String? = null,
        avatarId: AvatarId = AvatarId.CAT,
        totalGamesPlayed: Int = 0,
        totalWins: Int = 0,
        totalScore: Int = 0
    ) : this(
        id = id,
        displayName = displayName,
        email = email,
        avatarId = avatarId,
        accountType = AccountType.PERMANENT,
        guestMatchesCompleted = 0,
        guestAccessStatus = "ACTIVE",
        createdAt = 0L,
        updatedAt = 0L,
        lastSeenAt = 0L,
        stats = UserStats(
            gamesPlayed = totalGamesPlayed,
            wins = totalWins,
            totalScore = totalScore
        ),
        preferences = UserPreferences()
    )

    val totalGamesPlayed: Int get() = stats.gamesPlayed
    val totalWins: Int get() = stats.wins
    val totalScore: Int get() = stats.totalScore

    val isGuest: Boolean get() = accountType == AccountType.GUEST
    val isGuestLimitReached: Boolean get() = isGuest && (guestMatchesCompleted >= 2 || guestAccessStatus == "EXHAUSTED")
    val remainingGuestMatches: Int get() = if (isGuest) (2 - guestMatchesCompleted).coerceAtLeast(0) else Int.MAX_VALUE
}
