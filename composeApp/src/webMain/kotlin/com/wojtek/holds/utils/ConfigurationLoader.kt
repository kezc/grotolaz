package com.wojtek.holds.utils

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.wojtek.holds.Constants.DEFAULT_VERSION
import com.wojtek.holds.model.HoldConfiguration
import holds.composeapp.generated.resources.Res
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.skia.Image as SkiaImage

/**
 * Result of configuration loading operation.
 */
sealed class ConfigurationLoadResult {
    data class Success(val configuration: HoldConfiguration) : ConfigurationLoadResult()
    data class Error(val message: String) : ConfigurationLoadResult()
    object Loading : ConfigurationLoadResult()
}

/**
 * Loads hold configuration from resources.
 *
 * @param version Version identifier for the configuration to load (e.g., "v1", "v2")
 * @return ConfigurationLoadResult with the loaded configuration or error
 */
@OptIn(ExperimentalResourceApi::class)
suspend fun loadHoldConfiguration(
    version: String = DEFAULT_VERSION
): ConfigurationLoadResult {
    return try {
        val resourcePath = "files/$version/holds.json"
        val configText = Res.readBytes(resourcePath).decodeToString()
        val config = Json.decodeFromString<HoldConfiguration>(configText)
        ConfigurationLoadResult.Success(config)
    } catch (e: Exception) {
        ConfigurationLoadResult.Error("Nie udało się wczytać konfiguracji chwytów dla wersji '$version': ${e.message}")
    }
}

/**
 * Composable function that loads and remembers hold configuration.
 *
 * @param version Version identifier for the configuration to load (e.g., "v1", "v2")
 * @param onSuccess Optional callback when configuration loads successfully
 * @param onError Optional callback when configuration fails to load
 * @return State containing the configuration load result
 */
@Composable
fun rememberHoldConfiguration(
    version: String = DEFAULT_VERSION,
    onSuccess: ((HoldConfiguration) -> Unit)? = null,
    onError: ((String) -> Unit)? = null
): State<ConfigurationLoadResult> {
    val loadResult = remember { mutableStateOf<ConfigurationLoadResult>(ConfigurationLoadResult.Loading ) }

    LaunchedEffect(version) {
        val result = loadHoldConfiguration(version)
        loadResult.value = result

        when (result) {
            is ConfigurationLoadResult.Success -> onSuccess?.invoke(result.configuration)
            is ConfigurationLoadResult.Error -> onError?.invoke(result.message)
            ConfigurationLoadResult.Loading -> {}
        }
    }

    return loadResult
}


/**
 * Helper function to get the resource path for a versioned image.
 *
 * @param version Version identifier (e.g., "v1", "v2")
 * @param imageName Name of the image file (e.g., "wall.png", "empty.png")
 * @return Resource path for the image
 */
fun getVersionedImagePath(version: String, imageName: String): String {
    return "files/$version/$imageName"
}

/**
 * Loads an image from versioned resources as a Painter.
 *
 * @param version Version identifier (e.g., "v1", "v2")
 * @param imageName Name of the image file (e.g., "wall.png", "empty.png")
 * @return Painter for the image, or null if loading fails
 */
@OptIn(ExperimentalResourceApi::class)
suspend fun loadVersionedImage(version: String, imageName: String): Painter? {
    return try {
        val path = getVersionedImagePath(version, imageName)
        val bytes = Res.readBytes(path)
        val skiaImage = SkiaImage.makeFromEncoded(bytes)
        val imageBitmap = skiaImage.toComposeImageBitmap()
        BitmapPainter(imageBitmap)
    } catch (e: Exception) {
        null
    }
}

/**
 * Composable function to load and remember a versioned image.
 *
 * @param version Version identifier (e.g., "v1", "v2")
 * @param imageName Name of the image file (e.g., "wall.png", "empty.png")
 * @return State containing the loaded Painter, or null if not loaded
 */
@Composable
fun rememberVersionedImage(version: String, imageName: String): State<Painter?> =
    produceState(null) {
        value = loadVersionedImage(version, imageName)
    }

