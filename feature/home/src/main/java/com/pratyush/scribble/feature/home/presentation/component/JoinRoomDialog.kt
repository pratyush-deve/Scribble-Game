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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.ErrorRed
import com.pratyush.scribble.core.designsystem.theme.ScribbleTheme
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCyanLight
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.designsystem.theme.VibrantVioletLight
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
    val isOperationLoading = joinRoomState is JoinRoomState.ResolvingRoom || joinRoomState is JoinRoomState.Joining
    var localLoading by remember { mutableStateOf(false) }
    val effectiveLoading = isOperationLoading || localLoading
    val isInputValid = roomCode.trim().length in 4..8
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isOperationLoading) {
        if (!isOperationLoading) {
            localLoading = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "JoinRoomLoadingTransition")
    val linkRotation by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LinkRotation"
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
                .imePadding()
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
                        // Header: Link icon inside a rounded, subtly illuminated violet container
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
                                    imageVector = Icons.Rounded.Link,
                                    contentDescription = "Link room icon",
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
                            text = "Join the Fun",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Playful decorative doodle underline
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
                            text = "Got a room code? Your friends are waiting!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Room-code input field with label
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "ROOM CODE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = VibrantVioletLight,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )

                            OutlinedTextField(
                                value = roomCode,
                                onValueChange = { input ->
                                    val sanitized = input
                                        .uppercase()
                                        .filter { it.isLetterOrDigit() }
                                        .take(8)
                                    onRoomCodeChanged(sanitized)
                                },
                                placeholder = {
                                    Text(
                                        text = "e.g. AB12X",
                                        color = TextSecondary.copy(alpha = 0.55f),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            letterSpacing = 1.sp
                                        )
                                    )
                                },
                                singleLine = true,
                                enabled = !effectiveLoading,
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                trailingIcon = {
                                    if (roomCode.isNotEmpty() && !effectiveLoading) {
                                        IconButton(
                                            onClick = { onRoomCodeChanged("") },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear code",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    autoCorrectEnabled = false,
                                    keyboardType = KeyboardType.Ascii,
                                    imeAction = if (isInputValid) ImeAction.Go else ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        if (!effectiveLoading && isInputValid) {
                                            localLoading = true
                                            keyboardController?.hide()
                                            onConfirm()
                                        }
                                    },
                                    onDone = {
                                        keyboardController?.hide()
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant,
                                    disabledContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                                    focusedBorderColor = VibrantViolet,
                                    unfocusedBorderColor = VibrantViolet.copy(alpha = 0.35f),
                                    disabledBorderColor = DarkSurfaceVariant,
                                    cursorColor = VibrantVioletLight,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    disabledTextColor = TextPrimary.copy(alpha = 0.5f)
                                )
                            )
                        }

                        // Error Banner
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
                                    .padding(top = 12.dp)
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

                        Spacer(modifier = Modifier.height(22.dp))

                        // Primary Action Button (Join Room) with Animated Loading State
                        Button(
                            onClick = {
                                if (!effectiveLoading && isInputValid) {
                                    localLoading = true
                                    keyboardController?.hide()
                                    onConfirm()
                                }
                            },
                            enabled = !effectiveLoading && isInputValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .semantics {
                                    contentDescription = if (effectiveLoading) {
                                        "Joining room in progress"
                                    } else {
                                        "Join Room"
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
                            val buttonBrush = when {
                                effectiveLoading -> Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF7C3AED).copy(alpha = 0.75f),
                                        Color(0xFF6D28D9).copy(alpha = 0.75f)
                                    )
                                )
                                !isInputValid -> Brush.horizontalGradient(
                                    listOf(
                                        DarkSurfaceVariant,
                                        DarkSurfaceVariant
                                    )
                                )
                                else -> Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF8B5CF6),
                                        Color(0xFF7C3AED)
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = buttonBrush,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (!isInputValid && !effectiveLoading) {
                                            DarkSurfaceVariant
                                        } else {
                                            VibrantVioletLight.copy(alpha = if (effectiveLoading) 0.25f else 0.45f)
                                        },
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
                                    label = "JoinRoomButtonAnimation"
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
                                                    imageVector = Icons.Rounded.Link,
                                                    contentDescription = null,
                                                    tint = TextPrimary.copy(alpha = 0.9f),
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .graphicsLayer {
                                                            rotationZ = linkRotation
                                                        }
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            val progressText = if (joinRoomState is JoinRoomState.ResolvingRoom) {
                                                "Finding room…"
                                            } else {
                                                "Joining room…"
                                            }
                                            Text(
                                                text = progressText,
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
                                            Text(
                                                text = "Join Room",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isInputValid) TextPrimary else TextSecondary.copy(alpha = 0.5f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = if (isInputValid) TextPrimary else TextSecondary.copy(alpha = 0.5f),
                                                modifier = Modifier.size(18.dp)
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
private fun JoinRoomDialogPreview_Idle() {
    ScribbleTheme {
        JoinRoomDialog(
            roomCode = "AB12X",
            onRoomCodeChanged = {},
            joinRoomState = JoinRoomState.Idle,
            errorMessage = null,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun JoinRoomDialogPreview_Empty() {
    ScribbleTheme {
        JoinRoomDialog(
            roomCode = "",
            onRoomCodeChanged = {},
            joinRoomState = JoinRoomState.Idle,
            errorMessage = null,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun JoinRoomDialogPreview_Loading() {
    ScribbleTheme {
        JoinRoomDialog(
            roomCode = "AB12X",
            onRoomCodeChanged = {},
            joinRoomState = JoinRoomState.Joining,
            errorMessage = null,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun JoinRoomDialogPreview_Error() {
    ScribbleTheme {
        JoinRoomDialog(
            roomCode = "MS9999",
            onRoomCodeChanged = {},
            joinRoomState = JoinRoomState.RoomNotFound,
            errorMessage = "Room not found. Check the code and try again.",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
