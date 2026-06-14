package com.wojtek.holds.utils

import kotlinx.coroutines.Deferred

fun <T> Deferred<T>.getCompletedOrNull(): T? =
    if (isCompleted) {
        getCompleted()
    } else {
        null
    }
