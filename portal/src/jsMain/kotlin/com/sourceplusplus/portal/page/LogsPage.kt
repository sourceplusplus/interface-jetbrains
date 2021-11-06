package com.sourceplusplus.portal.page

import com.bfergerson.vertx3.eventbus.EventBus
import com.sourceplusplus.portal.*
import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.extensions.toMoment
import com.sourceplusplus.portal.extensions.toPrettyDuration
import com.sourceplusplus.portal.model.LogTableType.*
import com.sourceplusplus.portal.template.*
import spp.protocol.ProtocolAddress.Global.ClickedDisplayLog
import spp.protocol.ProtocolAddress.Global.ClickedDisplayLogs
import spp.protocol.ProtocolAddress.Global.ClickedStackTraceElement
import spp.protocol.ProtocolAddress.Global.FetchMoreLogs
import spp.protocol.ProtocolAddress.Global.RefreshPortal
import spp.protocol.ProtocolAddress.Portal.DisplayLog
import spp.protocol.ProtocolAddress.Portal.DisplayLogs
import spp.protocol.artifact.exception.sourceAsLineNumber
import spp.protocol.artifact.log.Log
import spp.protocol.artifact.log.LogOrderType.NEWEST_LOGS
import spp.protocol.artifact.log.LogResult
import spp.protocol.artifact.log.LogViewType
import spp.protocol.artifact.trace.TraceOrderType.*
import spp.protocol.portal.PageType.*
import spp.protocol.portal.PortalConfiguration
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.removeClass
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.tr
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic
import moment
import org.w3c.dom.Element
import kotlin.collections.set
import kotlin.js.json

/**
 * todo: description.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LogsPage(
    override val portalUuid: String,
    private val eb: EventBus
) : ILogsPage() {

    override fun setupEventbus() {
        if (!setup) {
            setup = true
            eb.registerHandler(DisplayLogs(portalUuid)) { _: dynamic, message: dynamic ->
                displayLogs(Json.decodeFromDynamic(message.body))
            }
            eb.registerHandler(DisplayLog(portalUuid)) { _: dynamic, message: dynamic ->
                displayLog(Json.decodeFromDynamic(message.body))
            }
        }
        eb.publish(RefreshPortal, portalUuid)
    }

    override fun renderPage(portalConfiguration: PortalConfiguration) {
        console.log("Rendering Logs page")
        this.configuration = portalConfiguration

        document.title = PortalBundle.translate("Logs - SourceMarker")
        val root: Element = document.getElementById("root")!!
        root.removeClass("overflow_y_hidden")
        root.innerHTML = ""
        root.append {
            portalNav {
                if (configuration.visibleOverview) navItem(OVERVIEW, onClick = {
                    setCurrentPage(eb, portalUuid, OVERVIEW)
                })
                if (configuration.visibleActivity) navItem(ACTIVITY, onClick = {
                    setCurrentPage(eb, portalUuid, ACTIVITY)
                })
                if (configuration.visibleTraces) navItem(TRACES, block = {
                    navSubItems(
                        PortalNavSubItem(LATEST_TRACES) { clickedTracesOrderType(eb, portalUuid, LATEST_TRACES) },
                        PortalNavSubItem(SLOWEST_TRACES) { clickedTracesOrderType(eb, portalUuid, SLOWEST_TRACES) },
                        PortalNavSubItem(FAILED_TRACES) { clickedTracesOrderType(eb, portalUuid, FAILED_TRACES) }
                    )
                })
                if (configuration.visibleLogs) navItem(LOGS, true, onClick = {
                    clickedLogsOrderType(eb, portalUuid, NEWEST_LOGS)
                })
                if (configuration.visibleConfiguration) navItem(CONFIGURATION, onClick = {
                    setCurrentPage(eb, portalUuid, CONFIGURATION)
                })
            }
            portalContent {
                navBar {
                    a(classes = "marginlefting ui item dropdown active_sub_tab") {
                        id = "logs_view_header"
                        onClickFunction = {
                            eb.send(ClickedDisplayLogs, json("portalUuid" to portalUuid))
                        }
                        span {
                            id = "logs_view_header_text"
                            +PortalBundle.translate(LogViewType.LIVE_TAIL.description)
                        }
                    }
                    rightAlign {
                        externalPortalButton { clickedViewAsExternalPortal(eb, portalUuid) }
                    }
                }
                wideColumn {
//                    table(
//                        "secondary_background_color no_top_margin",
//                        "log_pattern_table", "log_pattern_table_body",
//                        tableTypes = arrayOf(PATTERN, LEVEL, OCCURRED)
//                    )
                    table(
                        "secondary_background_color no_top_margin",
                        "log_operation_table", "log_operation_table_body",
                        tableTypes = arrayOf(OPERATION, LEVEL, OCCURRED)
                    )
                    div("visibility_hidden") {
                        id = "log_info_panel"
                    }
                }
            }
        }

        setupUI()
    }

    fun setupUI() {
        resetUI(LogViewType.LIVE_TAIL)

        @Suppress("UNUSED_VARIABLE") val logScrollConfig = json(
            "once" to false, "observeChanges" to true,
            "onTopVisible" to {
                if (jq("#log_operation_table")[0].rows.length >= 10) {
                    eb.send(FetchMoreLogs, json("portalUuid" to portalUuid, "pageNumber" to 1))
                }
            },
            "onBottomVisible" to {
                if (jq("#log_operation_table")[0].rows.length >= 10) {
                    eb.send(FetchMoreLogs, json("portalUuid" to portalUuid))
                }
            }
        )
        js("\$('#log_operation_table').visibility(logScrollConfig)")

        window.setInterval({ updateOccurredLabels(".log_time") }, 2000)
    }

    fun displayLogs(logResult: LogResult) {
        console.log("Displaying ${logResult.logs.size} logs")
        resetUI(LogViewType.LIVE_TAIL)

        jq("#log_operation_table tbody tr").remove()
        for (i in logResult.logs.indices) {
            val log = logResult.logs[i]

            var arg = 0
            var formattedMessage = log.content
            while (formattedMessage.contains("{}")) {
                formattedMessage = formattedMessage.replaceFirst(
                    "{}",
                    "<b class='spp_red_color'>" + log.arguments[arg++] + "</b>"
                )
            }

            val rowHtml = document.create.tr {
                onClickFunction = {
                    clickedDisplayLog(log)
                }

                if (log.exception != null) classes += "negative"
                td {
                    unsafe {
                        +formattedMessage
                    }
                }
                td {
                    classes += "collapsing"
                    if (log.level == "WARN" || log.level == "ERROR" || log.level == "FATAL") {
                        classes += "spp_red_color"
                    }
                    +log.level
                }

                val occurred = log.timestamp.toMoment()
                val now = moment(moment.now())
                val timeOccurredDuration = moment.duration(now.diff(occurred))
                td {
                    classes = setOf("log_time", "collapsing")
                    attributes["data-sort-value"] = log.timestamp.toEpochMilliseconds().toString()
                    attributes["data-value"] = log.timestamp.toEpochMilliseconds().toString()
                    style = "text-align: center"
                    +timeOccurredDuration.toPrettyDuration(1)
                }
            }
            jq("#log_operation_table").append(rowHtml)
        }

        //force AOT update
        updateOccurredLabels(".log_time")
    }

    private fun displayLog(log: Log) {
        console.log("Displaying log")
        resetUI(LogViewType.INDIVIDUAL_LOG)

        val logInfo: Element = document.getElementById("log_info_panel")!!
        logInfo.innerHTML = ""
        logInfo.append {
            div("ui segments") {
                div("ui segment span_segment_background") {
                    p {
                        b { +"Message: ${log.getFormattedMessage()}" }
                        br
                    }
                    p {
                        span {
                            +("Level: " + log.level)
                            br
                            +("Logger: " + log.logger)
                            br
                            +("Thread: " + log.thread)
                        }
                    }
                }

                if (log.exception != null) {
                    br
                    val stackTrace = log.exception!!
                    span {
                        h5("ui top attached header") {
                            span("spp_red_color") { +stackTrace.exceptionType }
                            if (stackTrace.message != null) {
                                br
                                +stackTrace.message!!
                            }
                        }
                    }
                    div("ui attached segment") {
                        style = "word-break: break-word"

                        for ((i, el) in stackTrace.getElements(true).withIndex()) {
                            span {
                                if (i > 0) br
                                unsafe { +"&emsp;" }
                                if (el.sourceAsLineNumber() != null) {
                                    a {
                                        onClickFunction = {
                                            eb.send(
                                                ClickedStackTraceElement, json(
                                                    "portalUuid" to portalUuid,
                                                    "stackTraceElement" to el
                                                )
                                            )
                                        }
                                        href = "javascript:void(0);"
                                        +el.toString(true)
                                    }
                                } else {
                                    +el.toString(true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun clickedDisplayLog(log: Log) {
        eb.send(ClickedDisplayLog, json("portalUuid" to portalUuid, "log" to log))
    }

    private fun resetUI(viewType: LogViewType) {
        when (viewType) {
            LogViewType.LIVE_TAIL -> {
                jq("#log_operation_table").css("display", "")
                jq("#log_info_panel").css("display", "none")
                    .css("visibility", "hidden")

                jq("#logs_view_header").addClass("active_sub_tab")
                    .removeClass("inactive_tab")
            }
            LogViewType.INDIVIDUAL_LOG -> {
                jq("#log_operation_table").css("display", "none")
                jq("#log_info_panel").css("display", "")
                    .css("visibility", "visible")

                jq("#logs_view_header").removeClass("active_sub_tab")
                    .addClass("inactive_tab")
            }
        }
    }
}
