package com.wojtek.holds.preprocessor

import com.wojtek.holds.preprocessor.model.Hold
import com.wojtek.holds.preprocessor.model.HoldConfiguration
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.sqrt
import kotlin.math.abs

/**
 * Detects climbing holds from a binary map image and generates polygon contours.
 *
 * Uses flood-fill algorithm to identify hold regions, Moore neighborhood tracing
 * for contour detection, and Douglas-Peucker algorithm for polygon simplification.
 */
class HoldDetector {

    data class Point(val x: Int, val y: Int)

    companion object {
        private const val WHITE_THRESHOLD = 200
        private const val SIMPLIFICATION_EPSILON = 8.0  // Higher value = smoother, less detailed polygons
        private const val MAX_CONTOUR_POINTS = 10000

        // Direction vectors for 8-connectivity (N, NE, E, SE, S, SW, W, NW)
        private val DIRECTION_X = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
        private val DIRECTION_Y = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)
    }

    /**
     * Detects all holds in the map image and returns their configuration.
     *
     * @param mapImagePath Path to binary map image (white holds on black background)
     * @param wallImagePath Path to the actual wall photo
     * @return Configuration containing all detected holds with polygon contours
     */
    fun detectHolds(mapImagePath: String, wallImagePath: String): HoldConfiguration {
        val mapImage = ImageIO.read(File(mapImagePath))
        val width = mapImage.width
        val height = mapImage.height

        // Ensure wall image matches map dimensions
        ensureWallImageMatchesMap(wallImagePath, width, height)

        // Track visited pixels to avoid detecting the same hold multiple times
        val visited = Array(height) { BooleanArray(width) }
        val holds = mutableListOf<Hold>()

        // Scan the image for white pixels (hold starts)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (shouldProcessPixel(x, y, mapImage, visited)) {
                    val hold = detectSingleHold(x, y, mapImage, visited, holds.size)
                    hold?.let { holds.add(it) }
                }
            }
        }

        return HoldConfiguration(
            wallImage = File(wallImagePath).name,
            imageWidth = width,
            imageHeight = height,
            holds = holds
        )
    }

    /**
     * Ensures the wall image has the same dimensions as the map.
     * If dimensions don't match, resizes the wall image and overwrites it.
     *
     * @param wallImagePath Path to the wall image
     * @param targetWidth Required width (from map image)
     * @param targetHeight Required height (from map image)
     */
    private fun ensureWallImageMatchesMap(
        wallImagePath: String,
        targetWidth: Int,
        targetHeight: Int
    ) {
        val wallFile = File(wallImagePath)
        val wallImage = ImageIO.read(wallFile)

        if (wallImage.width != targetWidth || wallImage.height != targetHeight) {
            println("Wall image dimensions (${wallImage.width}x${wallImage.height}) differ from map (${targetWidth}x${targetHeight})")
            println("Resizing wall image to match map dimensions...")

            val resizedImage = resizeImage(wallImage, targetWidth, targetHeight)

            // Save resized image (overwrite original)
            val format = wallFile.extension.lowercase().takeIf { it in listOf("png", "jpg", "jpeg") } ?: "png"
            ImageIO.write(resizedImage, format, wallFile)

            println("Wall image resized and saved to: $wallImagePath")
        } else {
            println("Wall image dimensions match map dimensions (${targetWidth}x${targetHeight})")
        }
    }

    /**
     * Resizes an image to the specified dimensions using high-quality scaling.
     *
     * @param original Original image
     * @param targetWidth Target width
     * @param targetHeight Target height
     * @return Resized image
     */
    private fun resizeImage(
        original: java.awt.image.BufferedImage,
        targetWidth: Int,
        targetHeight: Int
    ): java.awt.image.BufferedImage {
        val resized = java.awt.image.BufferedImage(
            targetWidth,
            targetHeight,
            java.awt.image.BufferedImage.TYPE_INT_RGB
        )

        val graphics = resized.createGraphics()
        try {
            // Use high-quality rendering hints
            graphics.setRenderingHint(
                java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC
            )
            graphics.setRenderingHint(
                java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY
            )
            graphics.setRenderingHint(
                java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON
            )

            graphics.drawImage(original, 0, 0, targetWidth, targetHeight, null)
        } finally {
            graphics.dispose()
        }

        return resized
    }

    /**
     * Checks if a pixel should be processed as a potential hold start.
     */
    private fun shouldProcessPixel(
        x: Int,
        y: Int,
        image: java.awt.image.BufferedImage,
        visited: Array<BooleanArray>
    ): Boolean {
        return !visited[y][x] && isWhitePixel(image.getRGB(x, y))
    }

    /**
     * Detects a single hold starting from the given coordinates.
     */
    private fun detectSingleHold(
        startX: Int,
        startY: Int,
        image: java.awt.image.BufferedImage,
        visited: Array<BooleanArray>,
        holdId: Int
    ): Hold? {
        val holdPixels = floodFillHold(image, startX, startY, visited)
        return if (holdPixels.isNotEmpty()) {
            createHoldFromPixels(holdId, holdPixels)
        } else {
            null
        }
    }

    /**
     * Determines if a pixel is white (part of a hold).
     */
    private fun isWhitePixel(rgb: Int): Boolean {
        val red = (rgb shr 16) and 0xFF
        val green = (rgb shr 8) and 0xFF
        val blue = rgb and 0xFF

        return red > WHITE_THRESHOLD &&
               green > WHITE_THRESHOLD &&
               blue > WHITE_THRESHOLD
    }

    /**
     * Performs flood-fill to find all pixels belonging to a hold.
     *
     * Uses a stack-based approach to avoid recursion depth issues.
     */
    private fun floodFillHold(
        image: java.awt.image.BufferedImage,
        startX: Int,
        startY: Int,
        visited: Array<BooleanArray>
    ): List<Point> {
        val pixels = mutableListOf<Point>()
        val stack = mutableListOf(Point(startX, startY))
        val width = image.width
        val height = image.height

        while (stack.isNotEmpty()) {
            val point = stack.removeAt(stack.lastIndex)

            if (!isValidPoint(point, width, height, visited)) {
                continue
            }

            if (!isWhitePixel(image.getRGB(point.x, point.y))) {
                continue
            }

            // Mark as visited and add to result
            visited[point.y][point.x] = true
            pixels.add(point)

            // Add 4-connected neighbors to stack
            stack.add(Point(point.x + 1, point.y))
            stack.add(Point(point.x - 1, point.y))
            stack.add(Point(point.x, point.y + 1))
            stack.add(Point(point.x, point.y - 1))
        }

        return pixels
    }

    /**
     * Checks if a point is valid (within bounds and not visited).
     */
    private fun isValidPoint(
        point: Point,
        width: Int,
        height: Int,
        visited: Array<BooleanArray>
    ): Boolean {
        return point.x in 0 until width &&
               point.y in 0 until height &&
               !visited[point.y][point.x]
    }

    /**
     * Creates a Hold object from a list of pixels.
     * Calculates bounding box and traces polygon contour.
     */
    private fun createHoldFromPixels(id: Int, pixels: List<Point>): Hold {
        val minX = pixels.minOf { it.x }
        val maxX = pixels.maxOf { it.x }
        val minY = pixels.minOf { it.y }
        val maxY = pixels.maxOf { it.y }

        val contour = tracePolygonContour(pixels.toSet(), minX, minY, maxX, maxY)

        return Hold(
            id = id,
            x = minX,
            y = minY,
            width = maxX - minX + 1,
            height = maxY - minY + 1,
            polygon = contour.map { com.wojtek.holds.preprocessor.model.Point(it.x, it.y) }
        )
    }

    /**
     * Traces the polygon contour of a hold using Moore neighborhood boundary tracing.
     *
     * @param pixelSet Set of all pixels in the hold
     * @param minX Minimum x coordinate of bounding box
     * @param minY Minimum y coordinate of bounding box
     * @param maxX Maximum x coordinate of bounding box
     * @param maxY Maximum y coordinate of bounding box
     * @return Simplified polygon contour
     */
    private fun tracePolygonContour(
        pixelSet: Set<Point>,
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int
    ): List<Point> {
        val startPoint = findStartPoint(pixelSet, minX, minY, maxX, maxY)
            ?: return emptyList()

        val rawContour = traceBoundary(pixelSet, startPoint)
        return simplifyPolygon(rawContour, SIMPLIFICATION_EPSILON)
    }

    /**
     * Finds the topmost-leftmost point in the pixel set.
     */
    private fun findStartPoint(
        pixelSet: Set<Point>,
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int
    ): Point? {
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val point = Point(x, y)
                if (point in pixelSet) {
                    return point
                }
            }
        }
        return null
    }

    /**
     * Traces the boundary of a shape using Moore neighborhood algorithm.
     *
     * Follows the contour by checking 8-connected neighbors in clockwise order.
     */
    private fun traceBoundary(pixelSet: Set<Point>, startPoint: Point): List<Point> {
        val contour = mutableListOf<Point>()
        var current = startPoint
        var direction = 0 // Current search direction
        var isFirstMove = true

        do {
            contour.add(current)

            // Start searching from previous direction (clockwise)
            val searchStartDir = if (isFirstMove) 0 else (direction + 5) % 8
            val nextPoint = findNextBoundaryPoint(pixelSet, current, searchStartDir)

            if (nextPoint == null) break

            current = nextPoint.first
            direction = nextPoint.second
            isFirstMove = false

            // Stop if we've returned to start
            if (!isFirstMove && current == startPoint) break

        } while (contour.size < MAX_CONTOUR_POINTS) // Safety limit

        return contour
    }

    /**
     * Finds the next boundary point by checking 8-connected neighbors.
     *
     * @return Pair of (next point, direction) or null if not found
     */
    private fun findNextBoundaryPoint(
        pixelSet: Set<Point>,
        current: Point,
        searchStartDir: Int
    ): Pair<Point, Int>? {
        for (i in 0 until 8) {
            val checkDir = (searchStartDir + i) % 8
            val nextX = current.x + DIRECTION_X[checkDir]
            val nextY = current.y + DIRECTION_Y[checkDir]
            val nextPoint = Point(nextX, nextY)

            if (nextPoint in pixelSet) {
                return Pair(nextPoint, checkDir)
            }
        }
        return null
    }

    /**
     * Simplifies a polygon using the Douglas-Peucker algorithm.
     *
     * Reduces the number of points while preserving the general shape.
     *
     * @param points Original polygon points
     * @param epsilon Maximum allowed distance for point removal
     * @return Simplified polygon
     */
    private fun simplifyPolygon(points: List<Point>, epsilon: Double): List<Point> {
        if (points.size < 3) return points

        return douglasPeucker(points, epsilon)
    }

    /**
     * Recursive Douglas-Peucker algorithm implementation.
     */
    private fun douglasPeucker(points: List<Point>, epsilon: Double): List<Point> {
        if (points.size < 3) return points

        // Find the point with maximum distance from line segment
        var maxDistance = 0.0
        var maxIndex = 0
        val endIndex = points.size - 1

        for (i in 1 until endIndex) {
            val distance = perpendicularDistance(points[i], points[0], points[endIndex])
            if (distance > maxDistance) {
                maxDistance = distance
                maxIndex = i
            }
        }

        // If max distance is greater than epsilon, recursively simplify
        return if (maxDistance > epsilon) {
            val leftSegment = douglasPeucker(points.subList(0, maxIndex + 1), epsilon)
            val rightSegment = douglasPeucker(points.subList(maxIndex, points.size), epsilon)
            leftSegment.dropLast(1) + rightSegment
        } else {
            listOf(points[0], points[endIndex])
        }
    }

    /**
     * Calculates the perpendicular distance from a point to a line segment.
     *
     * Uses the cross product formula for distance calculation.
     */
    private fun perpendicularDistance(point: Point, lineStart: Point, lineEnd: Point): Double {
        val dx = (lineEnd.x - lineStart.x).toDouble()
        val dy = (lineEnd.y - lineStart.y).toDouble()
        val norm = sqrt(dx * dx + dy * dy)

        if (norm == 0.0) {
            // Line segment is a point
            val px = (point.x - lineStart.x).toDouble()
            val py = (point.y - lineStart.y).toDouble()
            return sqrt(px * px + py * py)
        }

        // Calculate perpendicular distance using cross product
        val numerator = abs(
            dy * point.x - dx * point.y +
            lineEnd.x * lineStart.y - lineEnd.y * lineStart.x
        )

        return numerator / norm
    }
}
