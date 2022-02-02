package spp.jetbrains.marker.jvm.formatter

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import spp.protocol.instrument.LiveVariable
import java.time.Instant

class InstantFormatter : ValueFormatter {

    override fun format(variable: LiveVariable, presentation: PresentationData) {
        val values = (variable.value as List<Map<String, Any>>)
        var seconds: Long? = null
        var nanos: Long? = null

        for (v in values) {
            if ("seconds" == v["name"]) {
                seconds = (v["value"] as Int).toLong()
            } else if ("nanos" == v["name"]) {
                nanos = (v["value"] as Int).toLong()
            }
        }

        seconds?.let { nanos?.let { it1 -> Instant.ofEpochSecond(it, it1) } }
            .let { tt -> presentation.addText(" \"${tt}\"", SimpleTextAttributes.REGULAR_ATTRIBUTES) }
    }
}
