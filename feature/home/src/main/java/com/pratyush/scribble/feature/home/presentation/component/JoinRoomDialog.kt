package com.pratyush.scribble.feature.home.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pratyush.scribble.core.designsystem.component.ScribblePrimaryButton
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.ErrorRed
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.feature.home.presentation.state.JoinRoomState

@Composable
fun JoinRoomDialog(
    roomCode: String,
    onRoomCodeChanged: (String) -> Unit,
    joinRoomState: JoinRoomState = JoinRoomState.Idle,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isLoading = joinRoomState is JoinRoomState.ResolvingRoom || joinRoomState is JoinRoomState.Joining

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Join Room",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter the 4-6 character room code shared by your friend:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = roomCode,
                    onValueChange = onRoomCodeChanged,
                    placeholder = { Text("e.g. AB12X", color = TextSecondary) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        disabledContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = VibrantCyan,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        disabledBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        disabledTextColor = TextPrimary.copy(alpha = 0.7f)
                    )
                )

                if (isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = VibrantCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val progressText = if (joinRoomState is JoinRoomState.ResolvingRoom) {
                            "Finding your room…"
                        } else {
                            "Joining room…"
                        }
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibrantCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }
            }
        },
        confirmButton = {
            ScribblePrimaryButton(
                text = if (isLoading) "Joining..." else "Join",
                onClick = onConfirm,
                enabled = !isLoading && roomCode.trim().length >= 4,
                containerColor = VibrantCyan
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(
                    text = "Cancel",
                    color = if (isLoading) TextSecondary.copy(alpha = 0.4f) else TextSecondary
                )
            }
        }
    )
}
