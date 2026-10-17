package com.pratyush.scribble.core.ui.avatar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pratyush.scribble.core.designsystem.component.AvatarBadge
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.model.AvatarId

@Composable
fun AvatarGridPicker(
    selectedAvatar: AvatarId,
    onAvatarSelected: (AvatarId) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(AvatarId.entries, key = { it.id }) { avatar ->
            val isSelected = avatar == selectedAvatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) VibrantViolet else DarkSurfaceVariant,
                        shape = CircleShape
                    )
                    .clickable { onAvatarSelected(avatar) }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                AvatarBadge(emoji = avatar.emoji, size = 52.dp)
            }
        }
    }
}
