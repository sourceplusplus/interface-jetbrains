package spp.jetbrains.marker.jvm.formatter

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import org.joor.Reflect
import spp.protocol.instrument.LiveVariable
import java.time.LocalDate

class LocalDateFormatter : ValueFormatter {

    override fun format(variable: LiveVariable, presentation: PresentationData) {
        val values = (variable.value as List<Map<String, Any>>)
        val localDate = Reflect.on(LocalDate.now())
        values.forEach {
            localDate.set(it["name"] as String, asSmallestObject(it["value"]))
        }
        presentation.addText(" \"${localDate}\"", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }

    //todo: this should be done by processor
    private fun asSmallestObject(value: Any?): Any? {
        if (value is Number) {
            when (value.toLong()) {
                in Byte.MIN_VALUE..Byte.MAX_VALUE -> return value.toByte()
                in Int.MIN_VALUE..Int.MAX_VALUE -> return value.toInt()
                in Long.MIN_VALUE..Long.MAX_VALUE -> return value.toLong()
            }
        }
        return value
    }
}
