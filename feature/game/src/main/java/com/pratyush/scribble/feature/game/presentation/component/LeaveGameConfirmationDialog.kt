package com.pratyush.scribble.feature.game.presentation.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.ErrorRed
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary

@Composable
fun LeaveGameConfirmationDialog(
    onConfirmLeave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Leave game?",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = "Are you sure you want to leave this game? Your departure may end the game for other players.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmLeave) {
                Text(
                    text = "Leave game",
                    style = MaterialTheme.typography.labelLarge,
                    color = ErrorRed
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Stay",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
            }
        },
        modifier = modifier
    )
}
