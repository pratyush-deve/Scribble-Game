package com.pratyush.scribble.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.TextDisabled
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet

@Composable
fun ScribblePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = VibrantViolet,
    contentColor: Color = TextPrimary
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = DarkSurfaceVariant,
            disabledContentColor = TextDisabled
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
