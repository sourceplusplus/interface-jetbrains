package spp.jetbrains.sourcemarker.service.breakpoint

import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.table.IconTableCellRenderer
import spp.protocol.artifact.exception.methodName
import spp.protocol.artifact.exception.qualifiedClassName
import spp.protocol.artifact.exception.shortQualifiedClassName
import spp.protocol.artifact.exception.sourceAsLineNumber
import spp.protocol.instrument.breakpoint.event.LiveBreakpointHit
import spp.jetbrains.sourcemarker.icons.SourceMarkerIcons
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.Icon
import javax.swing.table.TableCellRenderer

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class BreakpointEventColumnInfo(name: String) : ColumnInfo<LiveBreakpointHit, String>(name) {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")
        .withZone(ZoneId.systemDefault())

    override fun getColumnClass(): Class<*> {
        return when (name) {
            "Breakpoint Data" -> Icon::class.java
            else -> super.getColumnClass()
        }
    }

    override fun getCustomizedRenderer(o: LiveBreakpointHit, renderer: TableCellRenderer): TableCellRenderer {
        return when (name) {
            "Breakpoint Data" -> IconTableCellRenderer.create(SourceMarkerIcons.LIVE_BREAKPOINT_ACTIVE_ICON)
            else -> super.getCustomizedRenderer(o, renderer)
        }
    }

    override fun getComparator(): Comparator<LiveBreakpointHit>? {
        return when (name) {
            "Time" -> Comparator { t: LiveBreakpointHit, t2: LiveBreakpointHit ->
                t.occurredAt.compareTo(t2.occurredAt) //todo: could add line number too
            }
            "Class Name" -> Comparator { t: LiveBreakpointHit, t2: LiveBreakpointHit ->
                t.stackTrace.first().qualifiedClassName().compareTo(t2.stackTrace.first().qualifiedClassName())
            }
            "File Name" -> Comparator { t: LiveBreakpointHit, t2: LiveBreakpointHit ->
                t.stackTrace.first().source.compareTo(t2.stackTrace.first().source)
            }
            "Line No" -> Comparator { t: LiveBreakpointHit, t2: LiveBreakpointHit ->
                t.stackTrace.first().sourceAsLineNumber()!!.compareTo(t2.stackTrace.first().sourceAsLineNumber()!!)
            }
            else -> null
        }
    }

    override fun valueOf(item: LiveBreakpointHit): String {
        return when (name) {
            "Time" -> formatter.format(item.occurredAt.toJavaInstant())
            "Host Name" -> item.serviceInstance.substringAfter("@")
            "Service" -> item.service
            "Class Name" -> item.stackTrace.first().shortQualifiedClassName()
            "File Name" -> item.stackTrace.first().source
            "Method Name" -> item.stackTrace.first().methodName()
            "Line No" -> item.stackTrace.first().sourceAsLineNumber()!!.toString()
            "Breakpoint Data" -> "View Frames/Variables"
            else -> item.toString()
        }
    }
}
