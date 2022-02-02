package spp.jetbrains.marker.jvm.formatter

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import spp.protocol.instrument.LiveVariable

class DateFormatter : ValueFormatter {

    override fun format(variable: LiveVariable, presentation: PresentationData) {
        presentation.addText(" \"${variable.value}\"", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
}
