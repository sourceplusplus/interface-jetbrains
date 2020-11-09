package com.sourceplusplus.protocol.utils

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

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

@OptIn(ExperimentalTime::class)
fun Duration.humanReadable(): String {
    if (inSeconds < 1) {
        return "${toLongMilliseconds()}ms"
    }
    return toString().substring(2)
        .replace("(\\d[HMS])(?!$)", "$1 ")
        .toLowerCase()
}

@OptIn(ExperimentalTime::class)
fun Duration.toPrettyDuration(): String {
    toComponents { days, hours, minutes, seconds, _ ->
        var prettyDuration = ""
        if (days > 0) {
            prettyDuration += "${days}d"
        }
        if (hours > 0) {
            if (prettyDuration.isNotEmpty()) prettyDuration += " "
            prettyDuration += "${hours}h"
        }
        if (minutes > 0) {
            if (prettyDuration.isNotEmpty()) prettyDuration += " "
            prettyDuration += "${minutes}m"
        } else if (seconds > 0) {
            if (prettyDuration.isNotEmpty()) prettyDuration += " "
            prettyDuration += "${seconds}s"
        } else if (days == 0 && hours == 0 && minutes == 0) {
            prettyDuration = "moments"
        }
        return prettyDuration
    }
}
