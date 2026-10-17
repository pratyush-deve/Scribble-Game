package com.pratyush.scribble.feature.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pratyush.scribble.core.designsystem.component.ScribblePrimaryButton
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceElevated
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet

data class HowToPlayStep(
    val number: Int,
    val iconEmoji: String,
    val title: String,
    val description: String
)

private val HOW_TO_PLAY_STEPS = listOf(
    HowToPlayStep(
        number = 1,
        iconEmoji = "🎮",
        title = "Create or join a room",
        description = "Host a room or enter a room code to join friends. Wait until everyone is ready and the host starts the game."
    ),
    HowToPlayStep(
        number = 2,
        iconEmoji = "💡",
        title = "Choose your secret word",
        description = "When it is your turn to draw, select one of the available words before the selection timer runs out."
    ),
    HowToPlayStep(
        number = 3,
        iconEmoji = "✏️",
        title = "Draw it out",
        description = "Use the drawing tools to illustrate your chosen word. Do not type or reveal the word directly in chat."
    ),
    HowToPlayStep(
        number = 4,
        iconEmoji = "💬",
        title = "Guess in chat",
        description = "When another player draws, enter your guess in the chat. The drawer cannot see the secret word in the guesser's interface."
    ),
    HowToPlayStep(
        number = 5,
        iconEmoji = "⚡",
        title = "Score points",
        description = "Guess correctly to earn points. Correct guesses made earlier should earn more points according to the game's scoring system."
    ),
    HowToPlayStep(
        number = 6,
        iconEmoji = "🔄",
        title = "Take turns",
        description = "Every player gets their drawing turn. Watch the timer and be ready to guess when other players draw."
    ),
    HowToPlayStep(
        number = 7,
        iconEmoji = "🏅",
        title = "Check the leaderboard",
        description = "At the end of the match, review the scores and vote to play again if you want another round."
    )
)

@Composable
fun HowToPlayDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .border(
                    width = 1.5.dp,
                    color = VibrantViolet.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header: Title and Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "How to Play Scribble",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Rules & gameplay walkthrough",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibrantCyan
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Steps Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HOW_TO_PLAY_STEPS.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceElevated.copy(alpha = 0.6f))
                                .border(
                                    width = 1.dp,
                                    color = DarkSurfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(VibrantViolet.copy(alpha = 0.25f))
                                    .border(1.dp, VibrantViolet.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = step.iconEmoji,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${step.number}. ${step.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = step.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action: Got it!
                ScribblePrimaryButton(
                    text = "Got it!",
                    onClick = onDismiss,
                    containerColor = VibrantViolet
                )
            }
        }
    }
}
