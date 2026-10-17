package com.pratyush.scribble.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratyush.scribble.core.designsystem.theme.DarkBackground
import com.pratyush.scribble.core.designsystem.theme.ScribbleTheme
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet

/**
 * Neutral branded loading/splash screen displayed during cold launch while initial
 * authentication state is being resolved by Firebase.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = VibrantViolet,
                strokeWidth = 3.dp
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
fun SplashScreenPreview() {
    ScribbleTheme {
        SplashScreen()
    }
}
