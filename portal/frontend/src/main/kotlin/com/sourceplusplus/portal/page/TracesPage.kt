package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.template.*
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType.*
import com.sourceplusplus.protocol.artifact.trace.TraceSpanInfoType.END_TIME
import com.sourceplusplus.protocol.artifact.trace.TraceSpanInfoType.START_TIME
import com.sourceplusplus.protocol.artifact.trace.TraceStackHeaderType.TIME_OCCURRED
import com.sourceplusplus.protocol.artifact.trace.TraceStackHeaderType.TRACE_ID
import com.sourceplusplus.protocol.artifact.trace.TraceTableType.*
import com.sourceplusplus.protocol.portal.PageType.*
import kotlinx.browser.document
import kotlinx.html.dom.append
import org.w3c.dom.Element

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TracesPage(
    private val portalUuid: String,
    private val traceOrderType: TraceOrderType
) {

    fun renderPage() {
        println("Rending Traces page")
        val root: Element = document.getElementById("root")!!
        root.innerHTML = ""

        root.append {
            portalNav {
                navItem(REAL_OVERVIEW)
                navItem(OVERVIEW)
                navItem(TRACES, isActive = true) {
                    navSubItem(LATEST_TRACES, SLOWEST_TRACES, FAILED_TRACES)
                }
                navItem(CONFIGURATION)
            }
            pusherContent {
                navBar {
                    tracesHeader(TRACE_ID, TIME_OCCURRED)
                    rightAlign {
                        externalPortalButton()
                    }
                }
                wideColumn {
                    table(
                        "secondary_background_color no_top_margin",
                        "top_trace_table", "trace_table",
                        tableTypes = arrayOf(OPERATION, OCCURRED, EXEC, STATUS)
                    )
                    table(
                        "trace_stack_table hidden_full_height",
                        "trace_stack_table", "stack_table",
                        "secondary_background_color", "stack_table_background",
                        tableTypes = arrayOf(OPERATION, EXEC, EXEC_PCT, STATUS)
                    )
                    spanInfoPanel(START_TIME, END_TIME)
                }
            }
        }
    }
}
