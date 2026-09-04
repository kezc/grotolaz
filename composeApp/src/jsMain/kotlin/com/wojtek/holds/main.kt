@file:OptIn(ExperimentalComposeUiApi::class)

package com.wojtek.holds

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

fun main() {
    ComposeViewport {
        App()
    }
}
