package com.pratyush.scribble.feature.home.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pratyush.scribble.core.designsystem.component.ScribbleCard
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan

@Composable
fun HowToPlayCard(
    modifier: Modifier = Modifier
) {
    ScribbleCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = DarkSurfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = VibrantCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "How to Play",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "1. When it's your turn, pick a secret word and draw it on the canvas.\n" +
                        "2. Guess what other players are drawing in live chat to earn points.\n" +
                        "3. The faster you guess correctly, the more points you score!",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
