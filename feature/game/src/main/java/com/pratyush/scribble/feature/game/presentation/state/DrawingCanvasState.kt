package com.pratyush.scribble.feature.game.presentation.state

import androidx.compose.ui.graphics.Color
import com.pratyush.scribble.core.designsystem.theme.StrokeBlack
import com.pratyush.scribble.core.model.DrawingStroke
import com.pratyush.scribble.core.model.DrawingTool

data class DrawingCanvasState(
    val strokes: List<DrawingStroke> = emptyList(),
    val currentPoints: List<Pair<Float, Float>> = emptyList(),
    val currentColor: Color = StrokeBlack,
    val currentStrokeWidth: Float = 6f,
    val currentTool: DrawingTool = DrawingTool.PEN,
    val canUndo: Boolean = false
)
