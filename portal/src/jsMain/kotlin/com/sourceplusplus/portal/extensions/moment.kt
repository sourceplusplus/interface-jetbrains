package com.sourceplusplus.portal.extensions

import kotlinx.datetime.Instant
import moment
import moment.Duration
import kotlin.math.round

fun Duration.toPrettyDuration(decimalPlaces: Int): String {
    var prettyDuration: String
    val postText: String
    if (months().toInt() > 0) {
        val months = months()
        val durationDiff = subtract(months, "months")
        prettyDuration = "${months}mo " + (round((durationDiff.asWeeks().toDouble() * 10)) / 10).toFixed(decimalPlaces)
        postText = "w ago"
    } else if (weeks().toInt() > 0) {
        val weeks = weeks()
        val durationDiff = subtract(weeks, "weeks")
        prettyDuration = "${weeks}w " + (round((durationDiff.asDays().toDouble() * 10)) / 10).toFixed(decimalPlaces)
        postText = "d ago"
    } else if (days().toInt() > 0) {
        val days = days()
        val durationDiff = subtract(days, "days")
        prettyDuration = "${days}d " + (round((durationDiff.asHours().toDouble() * 10)) / 10).toFixed(decimalPlaces)
        postText = "h ago"
    } else if (hours().toInt() > 0) {
        val hours = hours()
        val durationDiff = subtract(hours, "hours")
        prettyDuration = "${hours}h " + (round((durationDiff.asMinutes().toDouble() * 10)) / 10).toFixed(decimalPlaces)
        postText = "m ago"
    } else if (minutes().toInt() > 0) {
        val minutes = minutes()
        val durationDiff = subtract(minutes, "minutes")
        val seconds = durationDiff.seconds()
        if (seconds == 0) {
            prettyDuration = (minutes.toString() + "")
            postText = "m ago"
        } else {
            prettyDuration = (minutes.toString() + "m " + seconds() + "")
            postText = "s ago"
        }
    } else if (seconds().toInt() > 0) {
        prettyDuration = (seconds().toString() + "")
        postText = "s ago"
    } else {
        prettyDuration = (round((asSeconds().toDouble() * 10)) / 10).toFixed(decimalPlaces)
        postText = "s ago"
    }

    if (prettyDuration.endsWith(".0")) {
        prettyDuration = prettyDuration.substring(0, prettyDuration.length - 2)
    }
    return prettyDuration + postText
}

fun Double.toFixed(digits: Int): String = this.asDynamic().toFixed(digits) as String

fun Instant.toMoment(): moment.Moment = moment(toEpochMilliseconds(), "x")
