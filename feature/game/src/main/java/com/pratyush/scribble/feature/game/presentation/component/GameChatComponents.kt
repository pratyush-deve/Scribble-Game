package com.pratyush.scribble.feature.game.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Info
import com.pratyush.scribble.core.designsystem.component.AvatarBadge
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCoral
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantMint
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.model.ChatMessage
import com.pratyush.scribble.core.model.ChatMessageType
import com.pratyush.scribble.core.model.Player

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    senderPlayer: Player? = null,
    modifier: Modifier = Modifier
) {
    if (message.text.startsWith("The word was: ")) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(VibrantViolet.copy(alpha = 0.2f))
                .border(1.dp, VibrantViolet.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "💡", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = VibrantCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else if (message.text.startsWith("VOTE_PLAY_AGAIN")) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (senderPlayer != null) {
                AvatarBadge(emoji = senderPlayer.avatarId.emoji, size = 20.dp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            val action = if (message.text == "VOTE_PLAY_AGAIN") "voted to play again! 🔄" else "cancelled replay vote"
            Text(
                text = "${message.senderName} $action",
                style = MaterialTheme.typography.bodySmall,
                color = VibrantCyan,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else if (message.id.startsWith("reaction_") ||
        (message.text.startsWith("🔥 ") && message.text.endsWith("liked the drawing!")) ||
        (message.text.startsWith("👎 ") && message.text.endsWith("disliked the drawing!"))
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (senderPlayer != null) {
                AvatarBadge(emoji = senderPlayer.avatarId.emoji, size = 20.dp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall,
                color = if (message.text.contains("disliked")) VibrantCoral else VibrantMint,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        when (message.type) {
            ChatMessageType.CORRECT_GUESS -> {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(VibrantMint.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (senderPlayer != null) {
                            AvatarBadge(emoji = senderPlayer.avatarId.emoji, size = 20.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = "${message.senderName} guessed the word!",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibrantMint,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            ChatMessageType.SYSTEM_INFO -> {
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "System Notice",
                            tint = TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            else -> {
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (senderPlayer != null) {
                        AvatarBadge(emoji = senderPlayer.avatarId.emoji, size = 20.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "${message.senderName}: ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Type your guess here...", color = TextSecondary, fontSize = 14.sp) },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = VibrantMint,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            enabled = enabled && value.trim().isNotEmpty(),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (enabled && value.trim().isNotEmpty()) VibrantMint else DarkSurfaceVariant)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Guess",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
