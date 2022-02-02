package spp.jetbrains.marker.jvm.formatter

import com.intellij.ide.projectView.PresentationData
import spp.protocol.instrument.LiveVariable

interface ValueFormatter {
    fun format(variable: LiveVariable, presentation: PresentationData)
}
