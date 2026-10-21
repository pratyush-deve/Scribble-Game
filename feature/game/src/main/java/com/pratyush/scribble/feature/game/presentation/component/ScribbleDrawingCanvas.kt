package com.pratyush.scribble.feature.game.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pratyush.scribble.core.designsystem.theme.CanvasWhite
import com.pratyush.scribble.core.model.DrawingPoint
import com.pratyush.scribble.core.model.DrawingReaction
import com.pratyush.scribble.core.model.DrawingStroke
import com.pratyush.scribble.core.model.DrawingTool

@Composable
fun ScribbleDrawingCanvas(
    strokes: List<DrawingStroke>,
    isEditable: Boolean,
    onStrokeCompleted: (DrawingStroke) -> Unit,
    onEraserDragSegment: (p1x: Float, p1y: Float, p2x: Float, p2y: Float, radius: Float) -> Unit = { _, _, _, _, _ -> },
    onEraserGestureCompleted: () -> Unit = {},
    currentColor: Color,
    currentStrokeWidth: Float,
    currentTool: DrawingTool,
    modifier: Modifier = Modifier,
    showReactions: Boolean = false,
    selectedReaction: DrawingReaction? = null,
    likeCount: Int = 0,
    dislikeCount: Int = 0,
    reactionErrorMessage: String? = null,
    onReactionClicked: (DrawingReaction) -> Unit = {}
) {
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    var activePoints by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }
    var prevEraserPoint by remember { mutableStateOf<DrawingPoint?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(CanvasWhite)
            .onSizeChanged { canvasSize = it }
            .then(
                if (isEditable) {
                    Modifier.pointerInput(canvasSize, currentColor, currentStrokeWidth, currentTool) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val w = canvasSize.width.toFloat().coerceAtLeast(1f)
                                val h = canvasSize.height.toFloat().coerceAtLeast(1f)
                                val normX = (offset.x / w).coerceIn(0f, 1f)
                                val normY = (offset.y / h).coerceIn(0f, 1f)
                                val startPoint = DrawingPoint(normX, normY)

                                if (currentTool == DrawingTool.ERASER) {
                                    prevEraserPoint = startPoint
                                    // Initial tap erasure at point
                                    onEraserDragSegment(
                                        normX, normY, normX, normY,
                                        (currentStrokeWidth * 2.5f) / w
                                    )
                                } else {
                                    activePoints = listOf(startPoint)
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val w = canvasSize.width.toFloat().coerceAtLeast(1f)
                                val h = canvasSize.height.toFloat().coerceAtLeast(1f)
                                val normX = (change.position.x / w).coerceIn(0f, 1f)
                                val normY = (change.position.y / h).coerceIn(0f, 1f)
                                val currentPoint = DrawingPoint(normX, normY)

                                if (currentTool == DrawingTool.ERASER) {
                                    val prev = prevEraserPoint ?: currentPoint
                                    val eraserRadius = (currentStrokeWidth * 2.5f) / w
                                    onEraserDragSegment(
                                        prev.x, prev.y,
                                        currentPoint.x, currentPoint.y,
                                        eraserRadius
                                    )
                                    prevEraserPoint = currentPoint
                                } else {
                                    activePoints = activePoints + currentPoint
                                }
                            },
                            onDragEnd = {
                                if (currentTool == DrawingTool.ERASER) {
                                    prevEraserPoint = null
                                    onEraserGestureCompleted()
                                } else if (activePoints.isNotEmpty()) {
                                    val stroke = DrawingStroke(
                                        id = "stroke_${System.currentTimeMillis()}_${(1000..9999).random()}",
                                        points = activePoints,
                                        colorArgb = currentColor.toArgb().toLong(),
                                        strokeWidth = currentStrokeWidth,
                                        tool = DrawingTool.PEN
                                    )
                                    onStrokeCompleted(stroke)
                                    activePoints = emptyList()
                                }
                            },
                            onDragCancel = {
                                prevEraserPoint = null
                                activePoints = emptyList()
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Render committed strokes
            strokes.forEach { stroke ->
                val strokeColor = Color(stroke.colorArgb.toInt())
                if (stroke.points.size > 1) {
                    val path = Path()
                    val first = stroke.points.first()
                    path.moveTo(first.x * w, first.y * h)
                    for (i in 1 until stroke.points.size) {
                        val pt = stroke.points[i]
                        path.lineTo(pt.x * w, pt.y * h)
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(
                            width = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else if (stroke.points.size == 1) {
                    val pt = stroke.points.first()
                    drawCircle(
                        color = strokeColor,
                        radius = stroke.strokeWidth / 2f,
                        center = Offset(pt.x * w, pt.y * h)
                    )
                }
            }

            // Render currently dragged pen stroke
            if (currentTool == DrawingTool.PEN) {
                if (activePoints.size > 1) {
                    val path = Path()
                    val first = activePoints.first()
                    path.moveTo(first.x * w, first.y * h)
                    for (i in 1 until activePoints.size) {
                        val pt = activePoints[i]
                        path.lineTo(pt.x * w, pt.y * h)
                    }
                    drawPath(
                        path = path,
                        color = currentColor,
                        style = Stroke(
                            width = currentStrokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else if (activePoints.size == 1) {
                    val pt = activePoints.first()
                    drawCircle(
                        color = currentColor,
                        radius = currentStrokeWidth / 2f,
                        center = Offset(pt.x * w, pt.y * h)
                    )
                }
            }
        }

        if (showReactions) {
            CanvasReactionButtons(
                selectedReaction = selectedReaction,
                likeCount = likeCount,
                dislikeCount = dislikeCount,
                onReactionClicked = onReactionClicked,
                errorMessage = reactionErrorMessage,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
    }
}
