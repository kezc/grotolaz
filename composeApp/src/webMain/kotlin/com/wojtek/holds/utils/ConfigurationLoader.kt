package com.wojtek.holds.utils

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.wojtek.holds.Constants.DEFAULT_VERSION
import com.wojtek.holds.model.HoldConfiguration
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import holds.composeapp.generated.resources.Res
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
        ConfigurationLoadResult.Error("Failed to load holds configuration for version '$version': ${e.message}")
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
    val loadResult = remember { mutableStateOf<ConfigurationLoadResult>(ConfigurationLoadResult.Loading) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(version) {
        scope.launch {
            val result = loadHoldConfiguration(version)
            loadResult.value = result

            when (result) {
                is ConfigurationLoadResult.Success -> onSuccess?.invoke(result.configuration)
                is ConfigurationLoadResult.Error -> onError?.invoke(result.message)
                ConfigurationLoadResult.Loading -> {}
            }
        }
    }

    return loadResult
}

/**
 * Composable function for loading configuration with separate state values.
 *
 * @param version Version identifier for the configuration to load (e.g., "v1", "v2")
 * @return Triple of (configuration, isLoading, errorMessage)
 */
@Composable
fun rememberHoldConfigurationState(
    version: String = DEFAULT_VERSION
): Triple<HoldConfiguration?, Boolean, String?> {
    var configuration by remember { mutableStateOf<HoldConfiguration?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(version) {
        scope.launch {
            isLoading = true
            errorMessage = null

            when (val result = loadHoldConfiguration(version)) {
                is ConfigurationLoadResult.Success -> {
                    configuration = result.configuration
                    isLoading = false
                }
                is ConfigurationLoadResult.Error -> {
                    errorMessage = result.message
                    isLoading = false
                }
                ConfigurationLoadResult.Loading -> {
                    // Should not happen
                }
            }
        }
    }

    return Triple(configuration, isLoading, errorMessage)
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
fun rememberVersionedImage(version: String, imageName: String): State<Painter?> {
    val painterState = remember { mutableStateOf<Painter?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(version, imageName) {
        scope.launch {
            painterState.value = loadVersionedImage(version, imageName)
        }
    }

    return painterState
}
