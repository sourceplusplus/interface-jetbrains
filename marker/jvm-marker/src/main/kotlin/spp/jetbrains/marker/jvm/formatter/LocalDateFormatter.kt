package spp.jetbrains.marker.jvm.formatter

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import spp.protocol.instrument.LiveVariable
import java.time.LocalDate

class LocalDateFormatter : ValueFormatter {

    override fun format(variable: LiveVariable, presentation: PresentationData) {
        val values = (variable.value as List<Map<String, Any>>)
        var year: Int? = null
        var month: Int? = null
        var day: Int? = null

        for (v in values) {
            if ("year" == v["name"]) {
                year = (v["value"] as Int)
            } else if ("month" == v["name"]) {
                month = (v["value"] as Int)
            } else if ("day" == v["name"]) {
                day = (v["value"] as Int)
            }
        }

        year?.let { month?.let { it1 -> day?.let { it2 -> LocalDate.of(it, it1, it2) } } }
            .let { tt -> presentation.addText(" \"${tt}\"", SimpleTextAttributes.REGULAR_ATTRIBUTES) }
    }
}
