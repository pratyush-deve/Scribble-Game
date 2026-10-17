package com.pratyush.scribble.feature.game.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pratyush.scribble.core.designsystem.theme.CanvasWhite
import com.pratyush.scribble.core.designsystem.theme.DarkSurface
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceElevated
import com.pratyush.scribble.core.designsystem.theme.DarkSurfaceVariant
import com.pratyush.scribble.core.designsystem.theme.StrokeBlack
import com.pratyush.scribble.core.designsystem.theme.StrokeBlue
import com.pratyush.scribble.core.designsystem.theme.StrokeGreen
import com.pratyush.scribble.core.designsystem.theme.StrokeOrange
import com.pratyush.scribble.core.designsystem.theme.StrokePurple
import com.pratyush.scribble.core.designsystem.theme.StrokeRed
import com.pratyush.scribble.core.designsystem.theme.StrokeYellow
import com.pratyush.scribble.core.designsystem.theme.TextDisabled
import com.pratyush.scribble.core.designsystem.theme.TextPrimary
import com.pratyush.scribble.core.designsystem.theme.TextSecondary
import com.pratyush.scribble.core.designsystem.theme.VibrantCoral
import com.pratyush.scribble.core.designsystem.theme.VibrantCyan
import com.pratyush.scribble.core.designsystem.theme.VibrantViolet
import com.pratyush.scribble.core.model.DrawingTool

@Composable
fun DrawingToolBar(
    selectedTool: DrawingTool,
    onToolSelected: (DrawingTool) -> Unit,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    currentStrokeWidth: Float = 6f,
    onStrokeWidthSelected: (Float) -> Unit = {},
    canUndo: Boolean,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        StrokeBlack, StrokeRed, StrokeBlue, StrokeGreen,
        StrokeYellow, StrokePurple, StrokeOrange
    )

    val strokeSizes = listOf(
        4f to "Thin",
        8f to "Medium",
        16f to "Thick"
    )

    var showColorPalette by rememberSaveable { mutableStateOf(false) }
    var showBrushSizePicker by rememberSaveable { mutableStateOf(false) }
    var showClearConfirmationDialog by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        // Floating Color Palette Popover
        if (showColorPalette) {
            Popup(
                alignment = Alignment.TopCenter,
                onDismissRequest = { showColorPalette = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurfaceElevated,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(bottom = 60.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Color Palette",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            palette.forEach { color ->
                                val isSelected = selectedTool == DrawingTool.PEN && selectedColor == color
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) VibrantCyan else DarkSurfaceVariant,
                                            shape = CircleShape
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            onToolSelected(DrawingTool.PEN)
                                            onColorSelected(color)
                                            showColorPalette = false
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Brush Size Picker Popover
        if (showBrushSizePicker) {
            Popup(
                alignment = Alignment.TopCenter,
                onDismissRequest = { showBrushSizePicker = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurfaceElevated,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(bottom = 60.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Brush Size",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            strokeSizes.forEach { (size, label) ->
                                val isSelected = currentStrokeWidth == size
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) VibrantViolet.copy(alpha = 0.3f) else DarkSurface)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) VibrantCyan else DarkSurfaceVariant,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onStrokeWidthSelected(size)
                                            showBrushSizePicker = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size((size * 1.5f).coerceIn(6f, 24f).dp)
                                            .clip(CircleShape)
                                            .background(TextPrimary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Complete Visible Drawing Toolbar (min 48dp touch targets)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Pen Tool
            IconButton(
                onClick = {
                    onToolSelected(DrawingTool.PEN)
                    showColorPalette = false
                    showBrushSizePicker = false
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (selectedTool == DrawingTool.PEN) DarkSurfaceElevated else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.Brush,
                    contentDescription = "Pen",
                    tint = if (selectedTool == DrawingTool.PEN) VibrantViolet else TextPrimary
                )
            }

            // 2. Color Palette Toggle
            IconButton(
                onClick = {
                    showColorPalette = !showColorPalette
                    showBrushSizePicker = false
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (showColorPalette) DarkSurfaceElevated else Color.Transparent)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = "Color Palette",
                        tint = if (showColorPalette) VibrantCyan else TextPrimary
                    )
                    // Active color dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(1.dp, CanvasWhite, CircleShape)
                    )
                }
            }

            // 3. Brush Size Toggle
            IconButton(
                onClick = {
                    showBrushSizePicker = !showBrushSizePicker
                    showColorPalette = false
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (showBrushSizePicker) DarkSurfaceElevated else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.LineWeight,
                    contentDescription = "Brush Size",
                    tint = if (showBrushSizePicker) VibrantCyan else TextPrimary
                )
            }

            // 4. Eraser Tool
            IconButton(
                onClick = {
                    onToolSelected(DrawingTool.ERASER)
                    showColorPalette = false
                    showBrushSizePicker = false
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (selectedTool == DrawingTool.ERASER) DarkSurfaceElevated else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixNormal,
                    contentDescription = "Eraser",
                    tint = if (selectedTool == DrawingTool.ERASER) VibrantViolet else TextPrimary
                )
            }

            // 5. Undo Tool
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) TextPrimary else TextDisabled
                )
            }

            // 6. Clear Canvas Tool
            IconButton(
                onClick = { showClearConfirmationDialog = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Canvas",
                    tint = VibrantCoral
                )
            }
        }
    }

    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            title = {
                Text(
                    text = "Clear Drawing?",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will erase the entire canvas for everyone. Are you sure?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmationDialog = false
                        onClear()
                    }
                ) {
                    Text(text = "Clear", color = VibrantCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmationDialog = false }) {
                    Text(text = "Cancel", color = TextPrimary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}
