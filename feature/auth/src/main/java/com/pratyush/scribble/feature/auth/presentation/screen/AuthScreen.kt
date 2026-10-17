package com.pratyush.scribble.feature.auth.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratyush.scribble.core.designsystem.component.ScribblePrimaryButton
import com.pratyush.scribble.core.designsystem.component.ScribbleSecondaryButton
import com.pratyush.scribble.core.designsystem.theme.CorrectGreen
import com.pratyush.scribble.core.designsystem.theme.DarkBackground
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.ErrorRed
import com.pratyush.scribble.core.designsystem.theme.ScribbleTheme
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.ui.insets.ScribbleScreenContainer
import com.pratyush.scribble.feature.auth.presentation.state.AuthUiState

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onGoogleSignInClicked: () -> Unit,
    onGuestSignInClicked: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var guestName by remember { mutableStateOf("") }

    LaunchedEffect(uiState.authenticatedUser) {
        if (uiState.authenticatedUser != null) {
            onNavigateToHome()
        }
    }

    ScribbleScreenContainer(
        modifier = modifier,
        applyStatusBarInset = true,
        applyNavigationBarInset = true,
        applyImeInset = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(VibrantViolet, VibrantCyan)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Brush,
                    contentDescription = "Scribble Logo",
                    tint = TextPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SCRIBBLE",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = TextPrimary
            )

            Text(
                text = "Draw. Guess. Win.",
                style = MaterialTheme.typography.titleMedium,
                color = VibrantCyan,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Join real-time multiplayer drawing games with your friends!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            if (uiState.isLoading || uiState.authenticatedUser != null) {
                CircularProgressIndicator(color = VibrantViolet)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (uiState.authenticatedUser != null) "Signing in..." else "Authenticating...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                // Google Sign In
                ScribblePrimaryButton(
                    text = "Sign in with Google",
                    onClick = onGoogleSignInClicked,
                    containerColor = VibrantViolet
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "— OR PLAY AS GUEST —",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = guestName,
                    onValueChange = { guestName = it },
                    placeholder = { Text("Enter your nickname", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = VibrantCyan,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                ScribbleSecondaryButton(
                    text = "Play as Guest",
                    onClick = { onGuestSignInClicked(guestName) },
                    enabled = guestName.trim().isNotEmpty()
                )
            }

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorRed,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
fun AuthScreenPreview() {
    ScribbleTheme {
        AuthScreen(
            uiState = AuthUiState(),
            onGoogleSignInClicked = {},
            onGuestSignInClicked = {},
            onNavigateToHome = {}
        )
    }
}
