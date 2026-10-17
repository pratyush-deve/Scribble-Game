package com.pratyush.scribble.feature.home.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pratyush.scribble.core.designsystem.component.ScribblePrimaryButton
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet

@Composable
fun HostRoomDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Host New Room",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "A new game room will be created where you are the host. You can configure rounds, draw time, and invite friends with a code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            ScribblePrimaryButton(
                text = "Create Room",
                onClick = onConfirm,
                containerColor = VibrantViolet
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextSecondary)
            }
        }
    )
}
