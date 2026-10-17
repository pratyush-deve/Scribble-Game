package com.pratyush.scribble.feature.results.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratyush.scribble.core.designsystem.component.AvatarBadge
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceElevated
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantAmber
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantMint
import com.pratyush.scribble.core.model.PlayerScore

@Composable
fun WinnerPodium(
    rankings: List<PlayerScore>,
    modifier: Modifier = Modifier
) {
    val first = rankings.firstOrNull()
    val second = rankings.getOrNull(1)
    val third = rankings.getOrNull(2)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Second place (Silver)
        if (second != null) {
            PodiumStep(
                playerScore = second,
                stepHeight = 90,
                crownColor = TextSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        // First place (Gold)
        if (first != null) {
            PodiumStep(
                playerScore = first,
                stepHeight = 130,
                crownColor = VibrantAmber,
                modifier = Modifier.weight(1.1f)
            )
        }

        // Third place (Bronze)
        if (third != null) {
            PodiumStep(
                playerScore = third,
                stepHeight = 70,
                crownColor = VibrantCyan,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PodiumStep(
    playerScore: PlayerScore,
    stepHeight: Int,
    crownColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (playerScore.rank == 1) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Winner Trophy",
                tint = VibrantAmber,
                modifier = Modifier.size(32.dp)
            )
        }

        AvatarBadge(emoji = playerScore.player.avatarId.emoji, size = if (playerScore.rank == 1) 56.dp else 44.dp)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = playerScore.player.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1
        )

        Text(
            text = "${playerScore.score} pts",
            style = MaterialTheme.typography.bodySmall,
            color = crownColor,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stepHeight.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(DarkSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#${playerScore.rank}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = crownColor.copy(alpha = 0.8f)
            )
        }
    }
}
