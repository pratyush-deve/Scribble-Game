package com.pratyush.scribble.feature.game.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratyush.scribble.core.designsystem.component.ScribblePrimaryButton
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.model.WordChoice

@Composable
fun WordSelectionModal(
    choices: List<WordChoice>,
    isDrawer: Boolean,
    onWordSelected: (String) -> Unit,
    drawerName: String? = null,
    remainingSeconds: Int = 10,
    modifier: Modifier = Modifier
) {
    val displaySeconds = remainingSeconds.coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isDrawer) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Choose a word · ${displaySeconds}s",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = VibrantCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Pick the word you want to draw this round:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (choices.isEmpty()) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = VibrantCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Loading word choices...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        choices.forEach { choice ->
                            ScribblePrimaryButton(
                                text = choice.word.uppercase(),
                                onClick = { onWordSelected(choice.word) },
                                containerColor = VibrantViolet
                            )
                        }
                    }
                }
            } else {
                val displayName = drawerName?.ifBlank { null } ?: "Drawer"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "ROUND STARTING",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = VibrantCyan
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.CircularProgressIndicator(
                        color = VibrantCyan,
                        modifier = Modifier.size(42.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "$displayName is choosing a word · ${displaySeconds}s",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
