package com.wojtek.holds.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import com.wojtek.holds.database.Problem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class Base64Image(val base64: String)

@OptIn(ExperimentalEncodingApi::class)
fun ImageBitmap.toBase64(): Base64Image {
    val encodedBytes = Image.makeFromBitmap(this.asSkiaBitmap())
        .encodeToData(EncodedImageFormat.PNG, 80)
        ?.bytes
    return Base64Image(Base64.encode(encodedBytes!!))
}

fun Base64Image.toImageBitmap(): ImageBitmap {
    val decodedBytes = Base64.decode(base64)
    val skiaImage = Image.makeFromEncoded(decodedBytes)
    return skiaImage.toComposeImageBitmap()
}

val ProblemNavType = object : NavType<Problem>(isNullableAllowed = false) {
    override fun put(
        bundle: SavedState,
        key: String,
        value: Problem
    ) {
        bundle.write {
            val jsonString = Json.encodeToString(value)
            putString(key, jsonString)
        }
    }

    override fun get(bundle: SavedState, key: String): Problem? {
        val jsonString = bundle.read { getString(key) }
        return jsonString?.let { Json.decodeFromString(it) }
    }

    override fun parseValue(value: String): Problem {
        return Json.decodeFromString(value)
    }

    override fun serializeAsValue(value: Problem): String {
        val jsonString = Json.encodeToString(value)
        return jsonString
    }
}