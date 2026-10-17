package com.pratyush.scribble.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DrawingPoint(
    val x: Float,
    val y: Float
) {
    init {
        require(x in 0.0f..1.0f && y in 0.0f..1.0f) {
            "Normalized coordinates must be between 0.0 and 1.0 (x=$x, y=$y)"
        }
    }
}
