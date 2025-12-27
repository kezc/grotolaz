package com.wojtek.holds.model

import kotlinx.serialization.Serializable

@Serializable
data class HoldConfiguration(
    val wallImage: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val holds: List<Hold>
)

@Serializable
data class Hold(
    val id: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val polygon: List<Point> = emptyList() // Polygon contour points
)

@Serializable
data class Point(
    val x: Int,
    val y: Int
)

@Serializable
data class SelectedHolds(
    val selectedIds: List<Int>
)
