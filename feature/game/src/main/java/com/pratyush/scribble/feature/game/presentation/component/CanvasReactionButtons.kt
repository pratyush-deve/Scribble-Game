package com.pratyush.scribble.feature.game.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceElevated
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCoral
import com.pratyush.scribble.core.designsystem.theme.VibrantMint
import com.pratyush.scribble.core.model.DrawingReaction

/**
 * Compact Like / Dislike reaction buttons rendered at the bottom-right corner of the canvas.
 *
 * Visually unobtrusive, displaying live reaction counts and distinct active highlights
 * for whichever reaction the local guessing player has currently submitted.
 */
@Composable
fun CanvasReactionButtons(
    selectedReaction: DrawingReaction?,
    likeCount: Int,
    dislikeCount: Int,
    onReactionClicked: (DrawingReaction) -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val isLiked = selectedReaction == DrawingReaction.LIKE
    val isDisliked = selectedReaction == DrawingReaction.DISLIKE

    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceElevated.copy(alpha = 0.95f))
                    .border(
                        width = 1.dp,
                        color = VibrantCoral.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = VibrantCoral,
                    fontSize = 10.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurfaceElevated.copy(alpha = 0.92f))
                .border(
                    width = 1.dp,
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Like Button Item
            ReactionItem(
                icon = Icons.Default.ThumbUp,
                contentDescription = "Like drawing",
                count = likeCount,
                isSelected = isLiked,
                activeColor = VibrantMint,
                onClick = { onReactionClicked(DrawingReaction.LIKE) }
            )

            // Compact Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(DarkSurfaceVariant)
            )

            // Dislike Button Item
            ReactionItem(
                icon = Icons.Default.ThumbDown,
                contentDescription = "Dislike drawing",
                count = dislikeCount,
                isSelected = isDisliked,
                activeColor = VibrantCoral,
                onClick = { onReactionClicked(DrawingReaction.DISLIKE) }
            )
        }
    }
}

@Composable
private fun ReactionItem(
    icon: ImageVector,
    contentDescription: String,
    count: Int,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "reactionBgAnim"
    )
    val animatedIconColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else TextSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "reactionIconAnim"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else TextPrimary,
        animationSpec = tween(durationMillis = 200),
        label = "reactionTextAnim"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = animatedIconColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = animatedTextColor,
            fontSize = 11.sp
        )
    }
}
