package com.pratyush.scribble.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeGeometryTest {

    @Test
    fun `point to segment distance calculation is accurate`() {
        // Point (0.5, 0.5), Segment (0.0, 0.0) -> (1.0, 0.0)
        // Distance should be 0.5
        val dist = StrokeGeometry.distancePointToSegment(0.5f, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f)
        assertEquals(0.5f, dist, 0.001f)
    }

    @Test
    fun `intersecting segments have distance zero`() {
        // Horizontal segment (0.2, 0.5) -> (0.8, 0.5)
        // Vertical segment (0.5, 0.2) -> (0.5, 0.8)
        val dist = StrokeGeometry.distanceSegmentToSegment(
            0.2f, 0.5f, 0.8f, 0.5f,
            0.5f, 0.2f, 0.5f, 0.8f
        )
        assertEquals(0.0f, dist, 0.001f)
    }

    @Test
    fun `erasing intersecting stroke removes the entire stroke (whole-stroke erasing)`() {
        // Horizontal stroke from (0.1, 0.5) to (0.9, 0.5) with 9 points
        val points = (1..9).map { DrawingPoint(it / 10f, 0.5f) }
        val stroke = DrawingStroke(
            id = "stroke_1",
            points = points,
            colorArgb = 0xFF000000,
            strokeWidth = 6f,
            tool = DrawingTool.PEN
        )

        // Eraser dragged vertically through middle (0.5, 0.2) -> (0.5, 0.8)
        val result = StrokeGeometry.eraseWholeStrokesAlongSegment(
            existingStrokes = listOf(stroke),
            p1x = 0.5f, p1y = 0.2f,
            p2x = 0.5f, p2y = 0.8f,
            eraserRadius = 0.05f
        )

        // Whole stroke is removed completely
        assertEquals(0, result.size)
    }

    @Test
    fun `erasing completely outside a stroke leaves it untouched`() {
        val points = listOf(DrawingPoint(0.1f, 0.1f), DrawingPoint(0.2f, 0.2f))
        val stroke = DrawingStroke("stroke_2", points, 0xFF000000, 6f, DrawingTool.PEN)

        val result = StrokeGeometry.eraseWholeStrokesAlongSegment(
            existingStrokes = listOf(stroke),
            p1x = 0.8f, p1y = 0.8f,
            p2x = 0.9f, p2y = 0.9f,
            eraserRadius = 0.05f
        )

        assertEquals(1, result.size)
        assertEquals("stroke_2", result[0].id)
    }

    @Test
    fun `overlapping bounding box without segment intersection does not delete stroke`() {
        // L-shaped or diagonal stroke in quadrant: (0.1, 0.1) -> (0.9, 0.9)
        val points = listOf(DrawingPoint(0.1f, 0.1f), DrawingPoint(0.9f, 0.9f))
        val stroke = DrawingStroke("stroke_diag", points, 0xFF000000, 2f, DrawingTool.PEN)

        // Eraser is in corner (0.1, 0.9) -> (0.2, 0.8), well within bounding box [0.1..0.9, 0.1..0.9]
        // but far from the diagonal segment
        val result = StrokeGeometry.eraseWholeStrokesAlongSegment(
            existingStrokes = listOf(stroke),
            p1x = 0.1f, p1y = 0.9f,
            p2x = 0.2f, p2y = 0.8f,
            eraserRadius = 0.02f
        )

        assertEquals(1, result.size)
        assertEquals("stroke_diag", result[0].id)
    }

    @Test
    fun `single point dot stroke is deleted when eraser intersects`() {
        val stroke = DrawingStroke("dot_1", listOf(DrawingPoint(0.5f, 0.5f)), 0xFF000000, 10f, DrawingTool.PEN)

        val result = StrokeGeometry.eraseWholeStrokesAlongSegment(
            existingStrokes = listOf(stroke),
            p1x = 0.48f, p1y = 0.52f,
            p2x = 0.52f, p2y = 0.48f,
            eraserRadius = 0.03f
        )

        assertEquals(0, result.size)
    }
}
