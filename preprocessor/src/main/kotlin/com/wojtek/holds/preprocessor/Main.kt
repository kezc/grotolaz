package com.wojtek.holds.preprocessor

import com.wojtek.holds.preprocessor.Constants.DEFAULT_VERSION
import com.wojtek.holds.preprocessor.model.HoldConfiguration
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: preprocessor <map-image-path> <wall-image-path> [output-path] [version-id]")
        println("  map-image-path: Path to the binary map image (white holds on black background)")
        println("  wall-image-path: Path to the actual wall photo")
        println("  output-path: Optional path for the output JSON file (default: holds.json)")
        println("  version-id: Optional version identifier for the image set (default: v1)")
        return
    }

    val mapImagePath = args[0]
    val wallImagePath = args[1]
    val outputPath = if (args.size > 2) args[2] else "holds.json"
    val versionId = if (args.size > 3) args[3] else DEFAULT_VERSION

    // Validate input files
    if (!File(mapImagePath).exists()) {
        println("Error: Map image not found at: $mapImagePath")
        return
    }

    if (!File(wallImagePath).exists()) {
        println("Error: Wall image not found at: $wallImagePath")
        return
    }

    println("Processing map image: $mapImagePath")
    println("Wall image: $wallImagePath")

    try {
        val detector = HoldDetector()
        val config = detector.detectHolds(mapImagePath, wallImagePath, versionId)

        println("\nDetected ${config.holds.size} holds:")
        config.holds.forEach { hold ->
            println("  Hold #${hold.id}: position=(${hold.x}, ${hold.y}), size=(${hold.width}x${hold.height})")
        }

        // Serialize to JSON
        val json = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        val jsonString = json.encodeToString<HoldConfiguration>(config)

        // Write to file
        File(outputPath).writeText(jsonString)
        println("\nConfiguration saved to: $outputPath")

    } catch (e: Exception) {
        println("Error processing images: ${e.message}")
        e.printStackTrace()
    }
}
