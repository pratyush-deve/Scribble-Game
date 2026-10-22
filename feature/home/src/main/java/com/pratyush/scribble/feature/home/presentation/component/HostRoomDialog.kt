package com.pratyush.scribble.feature.home.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pratyush.scribble.core.designsystem.theme.ErrorRed
import com.pratyush.scribble.core.designsystem.theme.ScribbleTheme
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCyanLight
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.designsystem.theme.VibrantVioletLight

@Composable
fun HostRoomDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var localLoading by remember { mutableStateOf(false) }
    val effectiveLoading = isLoading || localLoading

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            localLoading = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "HostRoomLoadingTransition")
    val pencilRotation by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PencilRotation"
    )

    Dialog(
        onDismissRequest = {
            if (!effectiveLoading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !effectiveLoading,
            dismissOnClickOutside = !effectiveLoading
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF161824),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            VibrantViolet.copy(alpha = 0.50f),
                            VibrantViolet.copy(alpha = 0.15f)
                        )
                    )
                ),
                shadowElevation = 12.dp
            ) {
                Box {
                    // Subtle top purple radial ambient glow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        VibrantViolet.copy(alpha = 0.20f),
                                        Color.Transparent
                                    ),
                                    radius = 220f
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Drawing-themed decorative header badge
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(VibrantViolet.copy(alpha = 0.12f))
                            )

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF38235C),
                                                Color(0xFF231A3D)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        VibrantVioletLight.copy(alpha = 0.45f),
                                        RoundedCornerShape(18.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Brush,
                                    contentDescription = "Drawing brush icon",
                                    tint = VibrantVioletLight,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Tiny decorative sparkle accent
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = VibrantCyanLight.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.TopEnd)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Title
                        Text(
                            text = "Host New Room",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Restrained decorative doodle underline
                        Canvas(
                            modifier = Modifier
                                .width(46.dp)
                                .height(4.dp)
                        ) {
                            val path = Path().apply {
                                moveTo(0f, size.height * 0.5f)
                                quadraticTo(size.width * 0.25f, 0f, size.width * 0.5f, size.height * 0.5f)
                                quadraticTo(size.width * 0.75f, size.height, size.width, size.height * 0.5f)
                            }
                            drawPath(
                                path = path,
                                color = VibrantVioletLight.copy(alpha = 0.65f),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Subtitle
                        Text(
                            text = "Your game. Your rules. Your friends.",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = VibrantVioletLight,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Description
                        Text(
                            text = "Create a room, configure your match, and invite friends to play together.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        // Error Banner (if any error occurs during creation)
                        AnimatedVisibility(
                            visible = !errorMessage.isNullOrBlank(),
                            enter = fadeIn(tween(200)) + expandVertically(),
                            exit = fadeOut(tween(150)) + shrinkVertically()
                        ) {
                            Surface(
                                color = ErrorRed.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = "Error icon",
                                        tint = ErrorRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ErrorRed,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Primary Action Button with Animated Loading State
                        Button(
                            onClick = {
                                if (!effectiveLoading) {
                                    localLoading = true
                                    onConfirm()
                                }
                            },
                            enabled = !effectiveLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .semantics {
                                    contentDescription = if (effectiveLoading) {
                                        "Creating your room in progress"
                                    } else {
                                        "Create Room"
                                    }
                                    if (effectiveLoading) {
                                        liveRegion = LiveRegionMode.Polite
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 1.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = if (effectiveLoading) {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFF7C3AED).copy(alpha = 0.75f),
                                                    Color(0xFF6D28D9).copy(alpha = 0.75f)
                                                )
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFF8B5CF6),
                                                    Color(0xFF7C3AED)
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        1.dp,
                                        VibrantVioletLight.copy(alpha = if (effectiveLoading) 0.25f else 0.45f),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState = effectiveLoading,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(220)) togetherWith
                                            fadeOut(animationSpec = tween(180))
                                    },
                                    label = "CreateRoomButtonAnimation"
                                ) { loading ->
                                    if (loading) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier.size(22.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    color = TextPrimary,
                                                    strokeWidth = 2.2.dp
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Brush,
                                                    contentDescription = null,
                                                    tint = TextPrimary.copy(alpha = 0.9f),
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .graphicsLayer {
                                                            rotationZ = pencilRotation
                                                        }
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Creating your room…",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = TextPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Create Room",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Secondary Action Button (Cancel)
                        TextButton(
                            onClick = {
                                if (!effectiveLoading) {
                                    onDismiss()
                                }
                            },
                            enabled = !effectiveLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (effectiveLoading) {
                                    TextSecondary.copy(alpha = 0.35f)
                                } else {
                                    TextSecondary
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun HostRoomDialogPreview_Idle() {
    ScribbleTheme {
        HostRoomDialog(
            onConfirm = {},
            onDismiss = {},
            isLoading = false
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun HostRoomDialogPreview_Loading() {
    ScribbleTheme {
        HostRoomDialog(
            onConfirm = {},
            onDismiss = {},
            isLoading = true
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun HostRoomDialogPreview_Error() {
    ScribbleTheme {
        HostRoomDialog(
            onConfirm = {},
            onDismiss = {},
            isLoading = false,
            errorMessage = "Failed to establish connection to room server."
        )
    }
}
