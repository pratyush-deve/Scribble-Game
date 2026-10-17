package com.pratyush.scribble.feature.lobby.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pratyush.scribble.core.designsystem.component.ScribbleCard
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceElevated
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.model.RoomSettings
import com.pratyush.scribble.core.model.WordDifficulty

@Composable
fun RoomSettingsSection(
    settings: RoomSettings,
    isHost: Boolean,
    onRoundsChanged: (Int) -> Unit,
    onDrawTimeChanged: (Int) -> Unit,
    onDifficultyChanged: (WordDifficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    ScribbleCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "GAME SETTINGS",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Rounds Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rounds", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4, 5).forEach { r ->
                        val selected = settings.rounds == r
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) VibrantViolet else DarkSurfaceElevated)
                                .clickable(enabled = isHost) { onRoundsChanged(r) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = "$r", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Draw Time Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Draw Time", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(45, 60, 80).forEach { t ->
                        val selected = settings.drawTimeSeconds == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) VibrantViolet else DarkSurfaceElevated)
                                .clickable(enabled = isHost) { onDrawTimeChanged(t) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "${t}s", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Difficulty Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Difficulty", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WordDifficulty.entries.forEach { diff ->
                        val selected = settings.wordDifficulty == diff
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) VibrantViolet else DarkSurfaceElevated)
                                .clickable(enabled = isHost) { onDifficultyChanged(diff) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(text = diff.displayName, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
