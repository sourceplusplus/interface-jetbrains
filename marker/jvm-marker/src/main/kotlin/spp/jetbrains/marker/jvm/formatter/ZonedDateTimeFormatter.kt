package spp.jetbrains.marker.jvm.formatter

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import spp.protocol.instrument.LiveVariable
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ZonedDateTimeFormatter : ValueFormatter {

    override fun format(variable: LiveVariable, presentation: PresentationData) {
        val values = (variable.value as List<Map<String, Any>>)
        var dateTime: List<Map<String, Any>>? = null
        var date: List<Map<String, Any>>? = null
        var time: List<Map<String, Any>>? = null
        var offset: List<Map<String, Any>>? = null
        var zone: List<Map<String, Any>>? = null
        var year: Int? = null
        var month: Int? = null
        var day: Int? = null
        var hour: Int? = null
        var minute: Int? = null
        var second: Int? = null
        var nano: Int? = null
        var totalSeconds: Int? = null
        var zoneId: String? = null

        for (v in values) {
            if ("dateTime" == v["name"]) {
                dateTime = v["value"] as List<Map<String, Any>>
            } else if ("offset" == v["name"]) {
                offset = v["value"] as List<Map<String, Any>>
            } else if ("zone" == v["name"]) {
                zone = v["value"] as List<Map<String, Any>>
            }
        }

        offset?.let { totalSeconds = it[0]["value"] as Int }

        zone?.let { zoneId = it[0]["value"] as String }

        dateTime?.let {
            for (v in it) {
                if ("date" == v["name"]) {
                    date = v["value"] as List<Map<String, Any>>
                } else if ("time" == v["name"]) {
                    time = v["value"] as List<Map<String, Any>>
                }
            }
        }

        date?.let {
            for (v in it) {
                if ("year" == v["name"]) {
                    year = (v["value"] as Int).toInt()
                } else if ("month" == v["name"]) {
                    month = (v["value"] as Int).toInt()
                } else if ("day" == v["name"]) {
                    day = (v["value"] as Int).toInt()
                }
            }
        }

        time?.let {
            for (v in it) {
                if ("hour" == v["name"]) {
                    hour = (v["value"] as Int).toInt()
                } else if ("minute" == v["name"]) {
                    minute = (v["value"] as Int).toInt()
                } else if ("second" == v["name"]) {
                    second = (v["value"] as Int).toInt()
                } else if ("nano" == v["name"]) {
                    nano = (v["value"] as Int).toInt()
                }
            }
        }

        year?.let {
            month?.let { it1 ->
                day?.let { it2 ->
                    hour?.let { it3 ->
                        minute?.let { it4 ->
                            second?.let { it5 ->
                                nano?.let { it6 ->
                                    ZonedDateTime.ofLocal(
                                        LocalDateTime.of(it, it1, it2, it3, it4, it5, it6),
                                        zoneId.let { ZoneId.of(it) },
                                        totalSeconds?.let { ZoneOffset.ofTotalSeconds(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }.let { tt -> presentation.addText(" \"${tt}\"", SimpleTextAttributes.REGULAR_ATTRIBUTES) }
    }
}
