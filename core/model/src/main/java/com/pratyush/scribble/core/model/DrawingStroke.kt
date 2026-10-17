package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DrawingStroke(
    val id: String,
    val points: List<DrawingPoint>,
    val colorArgb: Long,
    val strokeWidth: Float,
    val tool: DrawingTool = DrawingTool.PEN
)
