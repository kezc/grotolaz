package com.wojtek.holds.utils

import androidx.compose.runtime.*
import com.wojtek.holds.model.HoldConfiguration
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import holds.composeapp.generated.resources.Res

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
 * @param resourcePath Path to the JSON configuration file in resources
 * @return ConfigurationLoadResult with the loaded configuration or error
 */
@OptIn(ExperimentalResourceApi::class)
suspend fun loadHoldConfiguration(
    resourcePath: String = "files/holds.json"
): ConfigurationLoadResult {
    return try {
        val configText = Res.readBytes(resourcePath).decodeToString()
        val config = Json.decodeFromString<HoldConfiguration>(configText)
        ConfigurationLoadResult.Success(config)
    } catch (e: Exception) {
        ConfigurationLoadResult.Error("Failed to load holds configuration: ${e.message}")
    }
}

/**
 * Composable function that loads and remembers hold configuration.
 *
 * @param resourcePath Path to the JSON configuration file in resources
 * @param onSuccess Optional callback when configuration loads successfully
 * @param onError Optional callback when configuration fails to load
 * @return State containing the configuration load result
 */
@Composable
fun rememberHoldConfiguration(
    resourcePath: String = "files/holds.json",
    onSuccess: ((HoldConfiguration) -> Unit)? = null,
    onError: ((String) -> Unit)? = null
): State<ConfigurationLoadResult> {
    val loadResult = remember { mutableStateOf<ConfigurationLoadResult>(ConfigurationLoadResult.Loading) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(resourcePath) {
        scope.launch {
            val result = loadHoldConfiguration(resourcePath)
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
 * @param resourcePath Path to the JSON configuration file in resources
 * @return Triple of (configuration, isLoading, errorMessage)
 */
@Composable
fun rememberHoldConfigurationState(
    resourcePath: String = "files/holds.json"
): Triple<HoldConfiguration?, Boolean, String?> {
    var configuration by remember { mutableStateOf<HoldConfiguration?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(resourcePath) {
        scope.launch {
            isLoading = true
            errorMessage = null

            when (val result = loadHoldConfiguration(resourcePath)) {
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
