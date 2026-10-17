package com.pratyush.scribble.core.model

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Geometric utility functions for stroke segment intersection and distance checks.
 * All coordinates are normalized in [0.0, 1.0].
 */
object StrokeGeometry {

    /**
     * Calculates the minimum Euclidean distance between a point (px, py)
     * and a line segment from (x1, y1) to (x2, y2).
     */
    fun distancePointToSegment(
        px: Float,
        py: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val lengthSquared = dx * dx + dy * dy

        if (lengthSquared == 0f) {
            val distDx = px - x1
            val distDy = py - y1
            return sqrt(distDx * distDx + distDy * distDy)
        }

        // Project point onto segment, clamp t to [0, 1]
        val t = max(0f, min(1f, ((px - x1) * dx + (py - y1) * dy) / lengthSquared))
        val projX = x1 + t * dx
        val projY = y1 + t * dy

        val diffX = px - projX
        val diffY = py - projY
        return sqrt(diffX * diffX + diffY * diffY)
    }

    /**
     * Calculates the minimum distance between two line segments:
     * Segment 1: (p1x, p1y) -> (p2x, p2y)
     * Segment 2: (q1x, q1y) -> (q2x, q2y)
     */
    fun distanceSegmentToSegment(
        p1x: Float, p1y: Float, p2x: Float, p2y: Float,
        q1x: Float, q1y: Float, q2x: Float, q2y: Float
    ): Float {
        // If segments intersect, distance is 0
        if (segmentsIntersect(p1x, p1y, p2x, p2y, q1x, q1y, q2x, q2y)) {
            return 0f
        }

        // Otherwise, distance is the min of the 4 point-to-segment distances
        val d1 = distancePointToSegment(p1x, p1y, q1x, q1y, q2x, q2y)
        val d2 = distancePointToSegment(p2x, p2y, q1x, q1y, q2x, q2y)
        val d3 = distancePointToSegment(q1x, q1y, p1x, p1y, p2x, p2y)
        val d4 = distancePointToSegment(q2x, q2y, p1x, p1y, p2x, p2y)

        return min(min(d1, d2), min(d3, d4))
    }

    private fun ccw(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Float {
        return (cy - ay) * (bx - ax) - (by - ay) * (cx - ax)
    }

    private fun onSegment(px: Float, py: Float, qx: Float, qy: Float, rx: Float, ry: Float): Boolean {
        return qx <= max(px, rx) && qx >= min(px, rx) &&
                qy <= max(py, ry) && qy >= min(py, ry)
    }

    private fun segmentsIntersect(
        x1: Float, y1: Float, x2: Float, y2: Float,
        x3: Float, y3: Float, x4: Float, y4: Float
    ): Boolean {
        val d1 = ccw(x3, y3, x4, y4, x1, y1)
        val d2 = ccw(x3, y3, x4, y4, x2, y2)
        val d3 = ccw(x1, y1, x2, y2, x3, y3)
        val d4 = ccw(x1, y1, x2, y2, x4, y4)

        // General case
        if (((d1 > 0f && d2 < 0f) || (d1 < 0f && d2 > 0f)) &&
            ((d3 > 0f && d4 < 0f) || (d3 < 0f && d4 > 0f))
        ) {
            return true
        }

        // Special Collinear Cases
        if (d1 == 0f && onSegment(x3, y3, x1, y1, x4, y4)) return true
        if (d2 == 0f && onSegment(x3, y3, x2, y2, x4, y4)) return true
        if (d3 == 0f && onSegment(x1, y1, x3, y3, x2, y2)) return true
        if (d4 == 0f && onSegment(x1, y1, x4, y4, x2, y2)) return true

        return false
    }

    /**
     * Checks whether [stroke] intersects with the continuous eraser segment
     * from (p1x, p1y) to (p2x, p2y) with normalized [eraserRadius].
     *
     * Uses bounding-box pre-filtering for efficiency, followed by point-to-segment
     * or segment-to-segment distance checks.
     */
    fun strokeIntersectsSegment(
        stroke: DrawingStroke,
        p1x: Float,
        p1y: Float,
        p2x: Float,
        p2y: Float,
        eraserRadius: Float
    ): Boolean {
        if (stroke.points.isEmpty()) return false

        val strokeThreshold = eraserRadius + (stroke.strokeWidth / 1000f)

        // 1. Bounding box pre-filter for performance
        var minX = stroke.points[0].x
        var maxX = stroke.points[0].x
        var minY = stroke.points[0].y
        var maxY = stroke.points[0].y
        for (i in 1 until stroke.points.size) {
            val pt = stroke.points[i]
            if (pt.x < minX) minX = pt.x
            if (pt.x > maxX) maxX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.y > maxY) maxY = pt.y
        }

        val eraserMinX = min(p1x, p2x) - strokeThreshold
        val eraserMaxX = max(p1x, p2x) + strokeThreshold
        val eraserMinY = min(p1y, p2y) - strokeThreshold
        val eraserMaxY = max(p1y, p2y) + strokeThreshold

        // If bounding boxes do not overlap, this stroke definitely does not intersect
        if (maxX < eraserMinX || minX > eraserMaxX || maxY < eraserMinY || minY > eraserMaxY) {
            return false
        }

        // 2. Exact segment-level collision check
        if (stroke.points.size == 1) {
            val pt = stroke.points[0]
            val dist = distancePointToSegment(pt.x, pt.y, p1x, p1y, p2x, p2y)
            return dist <= strokeThreshold
        }

        for (i in 1 until stroke.points.size) {
            val pPrev = stroke.points[i - 1]
            val pCurr = stroke.points[i]
            val dist = distanceSegmentToSegment(
                pPrev.x, pPrev.y, pCurr.x, pCurr.y,
                p1x, p1y, p2x, p2y
            )
            if (dist <= strokeThreshold) {
                return true
            }
        }

        return false
    }

    /**
     * Erases whole strokes that intersect with the continuous eraser segment
     * from (p1x, p1y) to (p2x, p2y) with normalized [eraserRadius].
     *
     * Whole-stroke erasing removes the entire stroke object from the canvas
     * (like Google Keep / Notes).
     */
    fun eraseWholeStrokesAlongSegment(
        existingStrokes: List<DrawingStroke>,
        p1x: Float,
        p1y: Float,
        p2x: Float,
        p2y: Float,
        eraserRadius: Float
    ): List<DrawingStroke> {
        return existingStrokes.filterNot { stroke ->
            strokeIntersectsSegment(stroke, p1x, p1y, p2x, p2y, eraserRadius)
        }
    }

    /**
     * Returns the IDs of all strokes that intersect the eraser segment.
     */
    fun findIntersectedStrokeIds(
        existingStrokes: List<DrawingStroke>,
        p1x: Float,
        p1y: Float,
        p2x: Float,
        p2y: Float,
        eraserRadius: Float
    ): List<String> {
        return existingStrokes.mapNotNull { stroke ->
            if (strokeIntersectsSegment(stroke, p1x, p1y, p2x, p2y, eraserRadius)) {
                stroke.id
            } else {
                null
            }
        }
    }

    /**
     * Erases whole strokes that intersect with the continuous eraser segment.
     */
    fun eraseStrokesAlongSegment(
        existingStrokes: List<DrawingStroke>,
        p1x: Float,
        p1y: Float,
        p2x: Float,
        p2y: Float,
        eraserRadius: Float
    ): List<DrawingStroke> {
        return eraseWholeStrokesAlongSegment(existingStrokes, p1x, p1y, p2x, p2y, eraserRadius)
    }
}
