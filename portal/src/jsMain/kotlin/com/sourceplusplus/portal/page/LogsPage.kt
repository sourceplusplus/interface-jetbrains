package com.sourceplusplus.portal.page

import com.bfergerson.vertx3.eventbus.EventBus
import com.sourceplusplus.portal.*
import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.extensions.toMoment
import com.sourceplusplus.portal.extensions.toPrettyDuration
import com.sourceplusplus.portal.model.*
import com.sourceplusplus.portal.template.*
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshPortal
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayLogs
import com.sourceplusplus.protocol.artifact.log.LogOrderType.*
import com.sourceplusplus.protocol.artifact.log.LogResult
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType.*
import com.sourceplusplus.protocol.portal.PageType.*
import com.sourceplusplus.protocol.portal.PortalConfiguration
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.removeClass
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.tr
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic
import moment
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.get

/**
 * todo: description.
 *
 * @since 0.1.2
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
        }
        eb.send(RefreshPortal, portalUuid)
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
                if (configuration.visibleLogs) navItem(LOGS, true, block = {
                    navSubItems(
                        PortalNavSubItem(NEWEST_LOGS) { clickedLogsOrderType(eb, portalUuid, NEWEST_LOGS) },
                        PortalNavSubItem(OLDEST_LOGS) { clickedLogsOrderType(eb, portalUuid, OLDEST_LOGS) }
                    )
                })
                if (configuration.visibleConfiguration) navItem(CONFIGURATION, onClick = {
                    setCurrentPage(eb, portalUuid, CONFIGURATION)
                })
            }
            portalContent {
                navBar {
                    rightAlign {
                        externalPortalButton { clickedViewAsExternalPortal(eb, portalUuid) }
                    }
                }
                wideColumn {
                    table(
                        "secondary_background_color no_top_margin",
                        "log_pattern_table", "log_pattern_table_body",
                        tableTypes = arrayOf(LogTableType.PATTERN, LogTableType.OCCURRED)
                    )
                    table(
                        "secondary_background_color no_top_margin hidden_full_height",
                        "log_operation_table", "log_operation_table_body",
                        tableTypes = arrayOf(LogTableType.OPERATION, LogTableType.OCCURRED)
                    )
                }
            }
        }

        setupUI()
    }

    fun setupUI() {
        window.setInterval({ updateOccurredLabels() }, 2000)
    }

    fun displayLogs(logResult: LogResult) {
        console.log("Displaying ${logResult.logs.size} logs")

        jq("#log_pattern_table tbody tr").remove()
        for (i in logResult.logs.indices) {
            val trace = logResult.logs[i]
            val rowHtml = document.create.tr {
                td {
                    +trace.content
                }

                val occurred = trace.timestamp.toMoment()
                val now = moment(moment.now())
                val timeOccurredDuration = moment.duration(now.diff(occurred))
                td {
                    classes = setOf("log_time", "collapsing")
                    attributes["data-sort-value"] = trace.timestamp.toEpochMilliseconds().toString()
                    attributes["data-value"] = trace.timestamp.toEpochMilliseconds().toString()
                    style = "text-align: center"
                    +timeOccurredDuration.toPrettyDuration(1)
                }
            }
            jq("#log_pattern_table").append(rowHtml)
        }

        //force AOT update
        updateOccurredLabels()
    }

    private fun updateOccurredLabels() {
        jq(".log_time").each(fun(_: Int, traceTime: HTMLElement) {
            if (!traceTime.dataset["value"].isNullOrEmpty()) {
                val occurred = moment(traceTime.dataset["value"]!!, "x")
                val now = moment(moment.now())
                val timeOccurredDuration = moment.duration(now.diff(occurred))
                traceTime.innerText = timeOccurredDuration.toPrettyDuration(1)
            }
        })
    }
}
