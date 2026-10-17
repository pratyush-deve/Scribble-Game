package com.pratyush.scribble.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DrawingStrokeTest {

    @Test
    fun `normalized drawing point coordinates strictly within zero and one`() {
        val pt = DrawingPoint(0.25f, 0.75f)
        assertEquals(0.25f, pt.x)
        assertEquals(0.75f, pt.y)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `point coordinates outside bounds throw exception`() {
        DrawingPoint(1.2f, -0.5f)
    }

    @Test
    fun `stroke maintains points and tool integrity`() {
        val stroke = DrawingStroke(
            id = "stroke_1",
            points = listOf(DrawingPoint(0.1f, 0.1f), DrawingPoint(0.2f, 0.2f)),
            colorArgb = 0xFFFFFFFF,
            strokeWidth = 8f,
            tool = DrawingTool.PEN
        )
        assertEquals(2, stroke.points.size)
        assertEquals(DrawingTool.PEN, stroke.tool)
        assertNotNull(stroke.id)
    }
}
