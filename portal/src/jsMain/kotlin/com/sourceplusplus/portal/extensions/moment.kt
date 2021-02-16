package com.sourceplusplus.portal.extensions

import com.sourceplusplus.portal.PortalBundle.translate
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
        postText = translate("w ago")
    } else if (weeks().toInt() > 0) {
        val weeks = weeks()
        val durationDiff = subtract(weeks, "weeks")
        prettyDuration = "${weeks}w " + (round((durationDiff.asDays().toDouble() * 10)) / 10).toFixed(decimalPlaces)
        postText = translate("d ago")
    } else if (days().toInt() > 0) {
        val days = days()
        val durationDiff = subtract(days, "days")
        prettyDuration = "${days}d " + (round((durationDiff.asHours().toDouble() * 10)) / 10).toFixed(decimalPlaces)
        postText = translate("h ago")
    } else if (hours().toInt() > 0) {
        val hours = hours()
        val durationDiff = subtract(hours, "hours")
        prettyDuration = "${hours}h " + (round((durationDiff.asMinutes().toDouble() * 10)) / 10).toFixed(decimalPlaces)
        postText = translate("m ago")
    } else if (minutes().toInt() > 0) {
        prettyDuration = "${minutes()}"
        postText = translate("m ago")
    } else if (seconds().toInt() > 0) {
        prettyDuration = (seconds().toString() + "")
        postText = translate("s ago")
    } else {
        prettyDuration = "1"
        postText = translate("s ago")
    }

    if (prettyDuration.endsWith(".0")) {
        prettyDuration = prettyDuration.substring(0, prettyDuration.length - 2)
    }
    return prettyDuration + postText
}

fun Double.toFixed(digits: Int): String = this.asDynamic().toFixed(digits) as String

fun Instant.toMoment(): moment.Moment = moment(toEpochMilliseconds(), "x")
