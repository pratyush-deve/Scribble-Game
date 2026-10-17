package com.pratyush.scribble.feature.lobby.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pratyush.scribble.core.designsystem.component.AvatarBadge
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceElevated
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantAmber
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.model.Player

@Composable
fun PlayerLobbyCard(
    player: Player,
    isLocalPlayer: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isLocalPlayer) DarkSurfaceElevated else DarkSurface)
            .border(
                width = if (isLocalPlayer) 2.dp else 1.dp,
                color = if (isLocalPlayer) VibrantViolet else DarkSurfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarBadge(emoji = player.avatarId.emoji, size = 44.dp)

            Spacer(modifier = Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isLocalPlayer) FontWeight.Bold else FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (player.isHost) {
                        Spacer(modifier = Modifier.size(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(VibrantAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Host",
                                    tint = VibrantAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = " HOST",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VibrantAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (isLocalPlayer) {
                    Text(
                        text = "You",
                        style = MaterialTheme.typography.bodySmall,
                        color = VibrantCyan
                    )
                }
            }
        }
    }
}
