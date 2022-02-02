package spp.jetbrains.marker.jvm

import com.intellij.ide.projectView.PresentationData
import spp.jetbrains.marker.jvm.formatter.*
import spp.protocol.instrument.LiveVariable
import java.time.*
import java.util.*
import kotlin.reflect.jvm.jvmName

object VariableValueFormatter {

    private val formatterMap: Map<String, ValueFormatter> = mapOf(

        Date::class.jvmName to DateFormatter(),
        Duration::class.jvmName to DurationFormatter(),
        Instant::class.jvmName to InstantFormatter(),
        LocalDate::class.jvmName to LocalDateFormatter(),
        LocalDateTime::class.jvmName to LocalDateTimeFormatter(),
        LocalTime::class.jvmName to LocalTimeFormatter(),
        OffsetDateTime::class.jvmName to OffsetDateTimeFormatter(),
        OffsetTime::class.jvmName to OffsetTimeFormatter(),
        ZonedDateTime::class.jvmName to ZonedDateTimeFormatter()
    )

    fun format(variable: LiveVariable, presentation: PresentationData) {
        formatterMap[variable.liveClazz]?.format(variable, presentation)
    }
}
