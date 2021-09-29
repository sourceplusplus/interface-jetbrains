package com.sourceplusplus.protocol.utils

fun Int.toPrettyDuration(): String {
    val days = this / 86400000.0
    if (days > 1) {
        return "${days.toInt()}dys"
    }
    val hours = this / 3600000.0
    if (hours > 1) {
        return "${hours.toInt()}hrs"
    }
    val minutes = this / 60000.0
    if (minutes > 1) {
        return "${minutes.toInt()}mins"
    }
    val seconds = this / 1000.0
    if (seconds > 1) {
        return "${seconds.toInt()}secs"
    }
    return "${this}ms"
}

fun Double.fromPerSecondToPrettyFrequency(): String {
    return when {
        this > 1000000.0 -> "${this / 1000000.0.toInt()}M/sec"
        this > 1000.0 -> "${this / 1000.0.toInt()}K/sec"
        this > 1.0 -> "${this.toInt()}/sec"
        else -> "${(this * 60.0).toInt()}/min"
    }
}

fun Long.toPrettyDuration(): String {
    val days = this / 86400000.0
    if (days > 1) {
        return "${days.toInt()}d"
    }
    val hours = this / 3600000.0
    if (hours > 1) {
        return "${hours.toInt()}h"
    }
    val minutes = this / 60000.0
    if (minutes > 1) {
        return "${minutes.toInt()}m"
    }
    val seconds = this / 1000.0
    if (seconds > 1) {
        return "${seconds.toInt()}s"
    }
    return "${this}ms"
}
