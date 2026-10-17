package com.pratyush.scribble.feature.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratyush.scribble.core.designsystem.component.ScribblePrimaryButton
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet

@Composable
fun GuestLimitDialog(
    isLoading: Boolean,
    onUpgradeClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(VibrantViolet, VibrantCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎨", fontSize = 26.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Free Matches Used",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You have completed your 2 free guest matches! Sign in with Google to save your stats, keep your avatar, and unlock unlimited multiplayer games.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        },
        confirmButton = {
            ScribblePrimaryButton(
                text = if (isLoading) "Connecting to Google..." else "Continue with Google",
                onClick = onUpgradeClicked,
                enabled = !isLoading,
                containerColor = VibrantViolet,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            if (!isLoading) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Not Now", color = TextSecondary)
                }
            }
        }
    )
}
