package spp.jetbrains.marker.jvm

import com.intellij.ide.projectView.PresentationData
import spp.jetbrains.marker.jvm.formatter.InstantFormatter
import spp.jetbrains.marker.jvm.formatter.ValueFormatter
import spp.protocol.instrument.LiveVariable

object VariableValueFormatter {

    private val formatterMap: Map<String, ValueFormatter> = mapOf("java.time.Instant" to InstantFormatter())

    fun format(variable: LiveVariable, presentation: PresentationData) {
        formatterMap[variable.liveClazz]?.format(variable, presentation)
    }
}
