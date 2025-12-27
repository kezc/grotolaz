package com.wojtek.holds

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform