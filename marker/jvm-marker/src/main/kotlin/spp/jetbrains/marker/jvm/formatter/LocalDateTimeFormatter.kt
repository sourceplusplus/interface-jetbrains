package spp.jetbrains.marker.jvm.formatter

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import spp.protocol.instrument.LiveVariable
import java.time.LocalDateTime

class LocalDateTimeFormatter : ValueFormatter {

    override fun format(variable: LiveVariable, presentation: PresentationData) {
        val values = (variable.value as List<Map<String, Any>>)
        var date: List<Map<String, Any>>? = null
        var time: List<Map<String, Any>>? = null
        var year: Int? = null
        var month: Int? = null
        var day: Int? = null
        var hour: Int? = null
        var minute: Int? = null
        var second: Int? = null
        var nano: Int? = null

        for (v in values) {
            if ("date" == v["name"]) {
                date = v["value"] as List<Map<String, Any>>
            } else if ("time" == v["name"]) {
                time = v["value"] as List<Map<String, Any>>
            }
        }
        date?.let {
            for (v in it) {
                if ("year" == v["name"]) {
                    year = v["value"] as Int
                } else if ("month" == v["name"]) {
                    month = v["value"] as Int
                } else if ("day" == v["name"]) {
                    day = v["value"] as Int
                }
            }
        }

        time?.let {
            for (v in it) {
                if ("hour" == v["name"]) {
                    hour = v["value"] as Int
                } else if ("minute" == v["name"]) {
                    minute = v["value"] as Int
                } else if ("second" == v["name"]) {
                    second = v["value"] as Int
                } else if ("nano" == v["name"]) {
                    nano = v["value"] as Int
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
                                    LocalDateTime.of(
                                        it, it1,
                                        it2, it3, it4, it5, it6
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
