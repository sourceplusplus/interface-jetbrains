package spp.jetbrains.marker.jvm.formatter

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import spp.protocol.instrument.LiveVariable
import java.time.OffsetTime
import java.time.ZoneOffset

class OffsetTimeFormatter : ValueFormatter {

    override fun format(variable: LiveVariable, presentation: PresentationData) {
        val values = (variable.value as List<Map<String, Any>>)
        var time: List<Map<String, Any>>? = null
        var offset: List<Map<String, Any>>? = null
        var hour: Int? = null
        var minute: Int? = null
        var second: Int? = null
        var nano: Int? = null
        var totalSeconds: Int? = null

        for (v in values) {
            if ("time" == v["name"]) {
                time = v["value"] as List<Map<String, Any>>
            } else if ("offset" == v["name"]) {
                offset = v["value"] as List<Map<String, Any>>
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

        offset?.let { totalSeconds = it[0]["value"] as Int}

        hour?.let {
            minute?.let { it1 ->
                second?.let { it2 ->
                    nano?.let { it3 ->
                        OffsetTime.of(it, it1,
                            it2, it3, totalSeconds?.let { it4 -> ZoneOffset.ofTotalSeconds(it4) })
                    }
                }
            }
        }.let { tt -> presentation.addText(" \"${tt}\"", SimpleTextAttributes.REGULAR_ATTRIBUTES) }
    }
}
