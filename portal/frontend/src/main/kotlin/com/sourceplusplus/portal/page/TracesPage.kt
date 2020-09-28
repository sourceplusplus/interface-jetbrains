package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.extensions.eb
import com.sourceplusplus.portal.template.*
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClickedDisplaySpanInfo
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClickedDisplayTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClickedDisplayTraces
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.TracesTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.DisplayInnerTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.DisplaySpanInfo
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.DisplayTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.DisplayTraces
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
import kotlin.js.json

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TracesPage {
    private val portalUuid = "null"
    init {
        console.log("Traces tab started");
        eb.onopen = {
            js("portalConnected();")
            eb.registerHandler(DisplayTraces(portalUuid)) { error: String, message: Any ->
                js("displayTraces(message.body);")
            }
            eb.registerHandler(DisplayInnerTraceStack(portalUuid)) { error: String, message: Any ->
                js("displayInnerTraces(message.body);")
            }
            eb.registerHandler(DisplayTraceStack(portalUuid)) { error: String, message: Any ->
                js("displayTraceStack(message.body);")
            }
            eb.registerHandler(DisplaySpanInfo(portalUuid)) { error: String, message: Any ->
                js("displaySpanInfo(message.body);")
            }

            eb.publish(TracesTabOpened, "{'portal_uuid': '$portalUuid', 'trace_order_type': traceOrderType}")
        };
    }

    fun renderPage() {
        println("Rending Traces page")
        val root: Element = document.getElementById("root")!!
        root.innerHTML = ""

        root.append {
            portalNav {
                navItem(OVERVIEW)
                navItem(TRACES, isActive = true) {
                    navSubItem(LATEST_TRACES, SLOWEST_TRACES, FAILED_TRACES)
                }
                navItem(CONFIGURATION)
            }
            tracesContent {
                navBar {
                    tracesHeader(TRACE_ID, TIME_OCCURRED)
                    rightAlign {
                        externalPortalButton()
                    }
                }
                tracesTable {
                    topTraceTable(OPERATION, OCCURRED, EXEC, STATUS)
                    traceStackTable(OPERATION, EXEC, EXEC_PCT, STATUS)
                    spanInfoPanel(START_TIME, END_TIME)
                }
            }
        }
    }

    fun clickedDisplayTraceStack(appUuid: String, artifactQualifiedName: String, globalTraceId: String) {
        eb.send(
            ClickedDisplayTraceStack,
            json(
                "portal_uuid" to portalUuid,
                "app_uuid" to appUuid,
                "artifact_qualified_name" to artifactQualifiedName,
                "trace_id" to globalTraceId
            )
        )
    }

    fun clickedDisplaySpanInfo(appUuid: String, rootArtifactQualifiedName: String, traceId: String, segmentId: String, spanId: Int) {
        eb.send(
            ClickedDisplaySpanInfo,
            json(
                "portal_uuid" to portalUuid,
                "app_uuid" to appUuid,
                "artifact_qualified_name" to rootArtifactQualifiedName,
                "trace_id" to traceId,
                "segment_id" to segmentId,
                "span_id" to spanId
            )
        )
    }

    fun clickedBackToTraces() {
        eb.send(
            ClickedDisplayTraces,
            json(
                "portal_uuid" to portalUuid
            )
        )
    }

    fun clickedBackToTraceStack() {
        eb.send(
            ClickedDisplayTraceStack,
            json(
                "portal_uuid" to portalUuid
            )
        )
    }
}
