package com.wojtek.holds.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.serialization.Serializable
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