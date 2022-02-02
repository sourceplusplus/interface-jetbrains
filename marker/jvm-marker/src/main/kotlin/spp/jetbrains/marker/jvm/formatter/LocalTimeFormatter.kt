package spp.jetbrains.marker.jvm.formatter

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import spp.protocol.instrument.LiveVariable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class LocalTimeFormatter : ValueFormatter {

    override fun format(variable: LiveVariable, presentation: PresentationData) {
        val values = (variable.value as List<Map<String, Any>>)
        var hour: Int? = null
        var minute: Int? = null
        var second: Int? = null
        var nano: Int? = null

        for (v in values) {
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

        hour?.let { it ->
            minute?.let { it1 ->
                second?.let { it2 ->
                    nano?.let { it3 ->
                        LocalTime.of(it, it1, it2, it3)
                    }
                }
            }
        }.let { tt -> presentation.addText(" \"${tt}\"", SimpleTextAttributes.REGULAR_ATTRIBUTES) }
    }
}
