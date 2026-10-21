package com.pratyush.scribble.feature.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratyush.scribble.core.designsystem.theme.ScribbleTheme
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantAmber
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantMint
import java.text.NumberFormat
import java.util.Locale

/**
 * Three independent, responsive statistic tiles displayed directly beneath the profile card:
 * 1. Wins (Trophy icon, green accent)
 * 2. Score (Star icon, gold accent)
 * 3. Games (Gamepad icon, cyan accent)
 */
@Composable
fun PlayerStatsRow(
    totalWins: Int,
    totalScore: Int,
    totalGamesPlayed: Int,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getIntegerInstance(Locale.US)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isNarrow = maxWidth < 320.dp
        val horizontalSpacing = if (isNarrow) 6.dp else 10.dp

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Wins
            PlayerStatItem(
                label = "Wins",
                value = numberFormat.format(totalWins),
                icon = Icons.Default.EmojiEvents,
                accentColor = VibrantMint,
                modifier = Modifier.weight(1f)
            )

            // 2. Score
            PlayerStatItem(
                label = "Score",
                value = numberFormat.format(totalScore),
                icon = Icons.Default.Star,
                accentColor = VibrantAmber,
                modifier = Modifier.weight(1f)
            )

            // 3. Games
            PlayerStatItem(
                label = "Games",
                value = numberFormat.format(totalGamesPlayed),
                icon = Icons.Default.SportsEsports,
                accentColor = VibrantCyan,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PlayerStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E2232),
                        Color(0xFF141724)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.28f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon in soft circular container
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Primary visual element: Number
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Secondary visual element: Label
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                ),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
fun PlayerStatsRowPreview() {
    ScribbleTheme {
        PlayerStatsRow(
            totalWins = 14,
            totalScore = 4850,
            totalGamesPlayed = 22,
            modifier = Modifier.padding(16.dp)
        )
    }
}
