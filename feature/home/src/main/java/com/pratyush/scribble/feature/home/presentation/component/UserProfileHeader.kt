package com.pratyush.scribble.feature.home.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pratyush.scribble.core.designsystem.theme.ScribbleTheme
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.UserProfile

/**
 * Legacy header component. Delegates to [ProfileShowcaseCard] to showcase the large animated avatar.
 */
@Composable
fun UserProfileHeader(
    userProfile: UserProfile,
    onProfileClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProfileShowcaseCard(
        userProfile = userProfile,
        onProfileClicked = onProfileClicked,
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
fun UserProfileHeaderPreview() {
    ScribbleTheme {
        UserProfileHeader(
            userProfile = UserProfile(
                id = "preview_1",
                displayName = "TigerMaster",
                avatarId = AvatarId.TIGER,
                totalWins = 14,
                totalScore = 4850,
                totalGamesPlayed = 22
            ),
            onProfileClicked = {}
        )
    }
}
