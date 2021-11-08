package com.sourceplusplus.portal.page

import com.bfergerson.vertx3.eventbus.EventBus
import com.sourceplusplus.portal.*
import com.sourceplusplus.portal.PortalBundle.translate
import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.extensions.toMoment
import com.sourceplusplus.portal.extensions.toPrettyDuration
import com.sourceplusplus.portal.model.TraceDisplayType
import com.sourceplusplus.portal.model.TraceSpanInfoType.END_TIME
import com.sourceplusplus.portal.model.TraceSpanInfoType.START_TIME
import com.sourceplusplus.portal.model.TraceTableType.*
import com.sourceplusplus.portal.template.*
import spp.protocol.ProtocolAddress.Global.ClickedDisplayInnerTraceStack
import spp.protocol.ProtocolAddress.Global.ClickedDisplaySpanInfo
import spp.protocol.ProtocolAddress.Global.ClickedDisplayTraceStack
import spp.protocol.ProtocolAddress.Global.ClickedDisplayTraces
import spp.protocol.ProtocolAddress.Global.ClickedStackTraceElement
import spp.protocol.ProtocolAddress.Global.FetchMoreTraces
import spp.protocol.ProtocolAddress.Global.RefreshPortal
import spp.protocol.ProtocolAddress.Portal.DisplaySpanInfo
import spp.protocol.ProtocolAddress.Portal.DisplayTraceStack
import spp.protocol.ProtocolAddress.Portal.DisplayTraces
import spp.protocol.ProtocolAddress.Portal.UpdateTraceSpan
import spp.protocol.artifact.exception.LiveStackTrace
import spp.protocol.artifact.exception.sourceAsLineNumber
import spp.protocol.artifact.log.LogOrderType.NEWEST_LOGS
import spp.protocol.artifact.trace.*
import spp.protocol.artifact.trace.TraceOrderType.*
import spp.protocol.portal.PageType.*
import spp.protocol.portal.PortalConfiguration
import spp.protocol.utils.toPrettyDuration
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
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
import org.w3c.dom.Node
import kotlin.js.json

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TracesPage(
    override val portalUuid: String,
    private val eb: EventBus
) : ITracesPage() {

    override fun setupEventbus() {
        if (!setup) {
            setup = true
            eb.registerHandler(DisplayTraces(portalUuid)) { _: dynamic, message: dynamic ->
                displayTraces(Json.decodeFromDynamic(message.body))
            }
            eb.registerHandler(DisplayTraceStack(portalUuid)) { _: dynamic, message: dynamic ->
                displayTraceStack(*Json.decodeFromDynamic(message.body))
            }
            eb.registerHandler(DisplaySpanInfo(portalUuid)) { _: dynamic, message: dynamic ->
                displaySpanInfo(Json.decodeFromDynamic(message.body))
            }
            eb.registerHandler(UpdateTraceSpan(portalUuid)) { _: dynamic, message: dynamic ->
                updateTraceSpan(Json.decodeFromDynamic(message.body))
            }
        }
        eb.publish(RefreshPortal, portalUuid)
        eb.send(FetchMoreTraces, json("portalUuid" to portalUuid, "pageNumber" to 1))
    }

    override fun renderPage(portalConfiguration: PortalConfiguration) {
        console.log("Rending Traces page")
        this.configuration = portalConfiguration

        document.title = translate("Traces - SourceMarker")
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
                if (configuration.visibleTraces) navItem(TRACES, true, block = {
                    navSubItems(
                        PortalNavSubItem(LATEST_TRACES) { clickedTracesOrderType(eb, portalUuid, LATEST_TRACES) },
                        PortalNavSubItem(SLOWEST_TRACES) { clickedTracesOrderType(eb, portalUuid, SLOWEST_TRACES) },
                        PortalNavSubItem(FAILED_TRACES) { clickedTracesOrderType(eb, portalUuid, FAILED_TRACES) }
                    )
                })
                if (configuration.visibleLogs) navItem(LOGS, onClick = {
                    clickedLogsOrderType(eb, portalUuid, NEWEST_LOGS)
                })
                if (configuration.visibleConfiguration) navItem(CONFIGURATION, onClick = {
                    setCurrentPage(eb, portalUuid, CONFIGURATION)
                })
            }
            portalContent {
                navBar {
                    tracesHeader(
                        //TRACE_ID, TIME_OCCURRED,
                        onClickBackToTraces = { clickedBackToTraces() },
                        onClickBackToTraceStack = { clickedBackToTraceStack() }
                    )
                    rightAlign {
                        externalPortalButton { clickedViewAsExternalPortal(eb, portalUuid) }
                    }
                }
                wideColumn {
                    table(
                        "secondary_background_color no_top_margin",
                        "top_trace_table", "trace_table",
                        tableTypes = arrayOf(OPERATION, OCCURRED, EXEC)
                    )
                    table(
                        "trace_stack_table hidden_full_height",
                        "trace_stack_table", "stack_table",
                        "secondary_background_color", "stack_table_background",
                        tableTypes = arrayOf(OPERATION, COMPONENT, EXEC_PCT, EXEC)
                    )
                    spanInfoPanel(START_TIME, END_TIME)
                }
            }
        }

        setupUI()
    }

    fun updateTraceSpan(traceSpan: TraceSpan) {
        console.log("Updating trace: ${traceSpan.traceId}")
        val htmlTraceId = traceSpan.traceId.split(".").joinToString("")
        val operationNameSpan = document.getElementById("trace-${htmlTraceId}-operation-name")
        operationNameSpan!!.textContent = traceSpan.endpointName!!
            .replace("<", "&lt;").replace(">", "&gt;")
    }

    override fun displayTraces(traceResult: TraceResult) {
        console.log("Displaying ${traceResult.traces.size} traces - ${traceResult.orderType}")
        resetUI(TraceDisplayType.TRACES, traceResult.orderType)

        for (i in traceResult.traces.indices) {
            val trace = traceResult.traces[i]
            val globalTraceId = trace.traceIds[0]
            val htmlTraceId = globalTraceId.split(".").joinToString("")
            var operationName = trace.operationNames[0]
            if (operationName == traceResult.artifactQualifiedName) {
                operationName = traceResult.artifactSimpleName!!
            }

            val rowHtml = document.create.tr {
                id = "trace-${htmlTraceId}"
                if (trace.error == true) classes += "negative"

                td {
                    onClickFunction = {
                        clickedDisplayTraceStack(
                            traceResult.artifactQualifiedName,
                            globalTraceId
                        )
                    }
                    style = "border-top: 0 !important; padding-left: 20px"
                    i {
                        style = "font-size:1.5em;margin-right:5px"
                        classes = setOf(
                            "far",
                            "fa-globe-americas",
                            if (trace.error == true) "spp_red_color" else "spp_blue_color"
                        )
                    }
                    span {
                        id = "trace-${htmlTraceId}-operation-name"
                        style = "vertical-align:top"
                        +operationName.replace("<", "&lt;").replace(">", "&gt;")
                    }
                }

                val occurred = trace.start.toMoment()
                val now = moment(moment.now())
                val timeOccurredDuration = moment.duration(now.diff(occurred))
                td {
                    classes = setOf("trace_time", "collapsing")
                    id = "trace_time_$htmlTraceId"
                    attributes["data-sort-value"] = trace.start.toEpochMilliseconds().toString()
                    attributes["data-value"] = trace.start.toEpochMilliseconds().toString()
                    style = "text-align: center"
                    +timeOccurredDuration.toPrettyDuration(1)
                }
                td {
                    classes += "collapsing"
                    attributes["data-sort-value"] = trace.duration.toString()
                    +trace.duration.toPrettyDuration()
                }
            }

            val traceTable: dynamic = document.getElementById("trace_table")
            val tableRow = traceTable.rows[i]
            if (tableRow != null) {
                //update existing trace
                if (tableRow.id != "trace-$htmlTraceId") {
                    traceTable.rows[i].outerHTML = rowHtml
                }
            } else {
                //add new trace
                jq("#trace_table").append(rowHtml)
            }
        }

        //force AOT update
        updateOccurredLabels(".trace_time")
    }

    override fun displayTraceStack(traceStackPath: TraceStackPath) {
        console.log("Displaying trace stack")
        resetUI(TraceDisplayType.TRACE_STACK)

        if (!traceStackPath.localTracing && traceStackPath.getCurrentRoot() != null) {
            jq("#latest_traces_header_text").text(translate("Parent Stack"))
        } else {
            jq("#latest_traces_header_text").text(translate(traceStackPath.orderType.fullDescription))
        }

        for ((segIdx, segment) in traceStackPath.traceStack.filter {
            traceStackPath.getCurrentSegment() == null || it == traceStackPath.getCurrentSegment()
        }.withIndex()) {
            if (segIdx != 0) {
                jq("#stack_table").append(document.create.tr {
                    td { div("ui horizontal divider") }
                    td { div("ui horizontal divider") }
                    td { div("ui horizontal divider") }
                    td { div("ui horizontal divider") }
                })
            }

            displayTraceSegment(segment, traceStackPath)
        }
    }

    private fun displayTraceSegment(segment: TraceStack.Segment, traceStackPath: TraceStackPath) {
        val segmentSpans = segment.traceSpans
        val spans = if (traceStackPath.getCurrentRoot() != null) {
            listOf(traceStackPath.getCurrentRoot()!!) + segment.getChildren(traceStackPath.getCurrentRoot()!!)
        } else {
            listOf(segmentSpans[0]) + segment.getChildren(0)
        }
        jq("#trace_id_field").`val`(spans[0].traceId)
        jq("#time_occurred_field").`val`(spans[0].startTime.toMoment().format())

        for (i in spans.indices) {
            val span = spans[i]
            val rowHtml = document.create.tr {
                if (span.error == true) classes += "negative"

                td {
                    onClickFunction = {
                        if (segment.hasChildren(span) && span.spanId > 0 && span !== traceStackPath.getCurrentRoot()) {
                            clickedDisplayInnerTraceStack(
                                span.traceId,
                                span.segmentId,
                                span.spanId
                            )
                        } else {
                            clickedDisplaySpanInfo(
                                span.getMetaString("rootArtifactQualifiedName"),
                                span.traceId,
                                span.segmentId,
                                span.spanId
                            )
                        }
                    }
                    style = "border-top: 0 !important; padding-left: 20px"

                    if (span.type == "Entry") {
                        i {
                            style = "font-size:1.5em;margin-right:5px"
                            classes = setOf(
                                "fal",
                                "fa-share",
                                if (span.error == true) "spp_red_color" else "spp_blue_color"
                            )
                        }
                    } else if (span.type == "Exit") {
                        i {
                            style = "font-size:1.5em;margin-right:5px"
                            classes = setOf(
                                "fal",
                                "fa-reply",
                                if (span.error == true) "spp_red_color" else "spp_blue_color"
                            )
                        }
                    } else {
                        i {
                            style = "font-size:1.5em;margin-right:5px"
                            classes = setOf(
                                "fal",
                                "fa-chevron-double-right",
                                if (span.error == true) "spp_red_color" else "spp_blue_color"
                            )
                        }
                    }
                    span {
                        style = "vertical-align:top"
                        +span.getMetaString("operationName")
                            .replace("<", "&lt;")
                            .replace(">", "&gt;")
                    }
                }
                td {
                    classes += "collapsing"
                    if (span.component != "Unknown") {
                        var component = COMPONENT_MAPPINGS[span.component]
                        if (component == null) {
                            component = span.component
                        }
                        img {
                            width = "18px"
                            height = "18px"
                            src = "../themes/default/assets/components/${component?.toUpperCase()}.png"
                        }
                    } else {
                        i {
                            style = "font-size:1.5em;margin-right:5px"
                            classes = setOf("far", "fa-microchip")
                        }
                    }
                }
                td {
                    attributes["data-sort-value"] = span.getMetaDouble("totalTracePercent").toString()

                    div {
                        classes = setOf("ui", "spp_light_blue_bar", "progress")
                        id = "trace_bar_${i}"
                        attributes["data-percent"] = span.getMetaDouble("totalTracePercent").toString()
                        style = "margin: 0"
                        div {
                            classes += "bar"
                            style = "transition-duration: 300ms; display: block; width: ${
                                span.getMetaDouble("totalTracePercent")
                            }%"
                        }
                    }
                }
                td {
                    classes += "collapsing"
                    val duration = (span.endTime.toEpochMilliseconds() - span.startTime.toEpochMilliseconds())
                    attributes["data-sort-value"] = duration.toString()
                    +duration.toPrettyDuration()
                }
            }

            jq("#stack_table").append(rowHtml)
        }
    }

    override fun displaySpanInfo(spanInfo: TraceSpan) {
        console.log("Displaying span info")
        resetUI(TraceDisplayType.SPAN_INFO)

        jq("#span_info_start_trace_time").attr("data-value", spanInfo.startTime.toEpochMilliseconds())
        jq("#span_info_start_time").text(spanInfo.startTime.toMoment().format("h:mm:ss a"))
        jq("#span_info_end_trace_time").attr("data-value", spanInfo.endTime.toEpochMilliseconds())
        jq("#span_info_end_time").text(spanInfo.endTime.toMoment().format("h:mm:ss a"))
        jq("#segment_id_field").valueOf(spanInfo.segmentId)

        var gotTags = false
        spanInfo.tags.forEach {
            gotTags = true
            val value = spanInfo.tags[it.key]
            if (value != "") {
                val rowHtml = document.create.tr {
                    td {
                        +it.key
                    }
                    td {
                        +it.value
                    }
                }
                jq("#tag_table").append(rowHtml)
            }
        }
        if (gotTags) {
            jq("#span_tag_div").removeClass("displaynone")
        } else {
            jq("#span_tag_div").addClass("displaynone")
        }

        jq("#log_table").empty()
        var gotLogs = false
        for ((logIndex, log) in spanInfo.logs.withIndex()) {
            gotLogs = true
            document.getElementById("log_table")!!.append {
                tr {
                    td {
                        p { id = "log_data_$logIndex" }
                    }
                }
            }

            var logData: Node = document.getElementById("log_data_$logIndex")!!
            val stackTrace = LiveStackTrace.fromString(log.data)
            if (stackTrace != null) {
                logData.appendChild(document.create.span {
                    h5("ui top attached header") {
                        span("spp_red_color") { +stackTrace.exceptionType }
                        if (stackTrace.message != null) {
                            br
                            +stackTrace.message!!
                        }
                    }
                })
                logData = logData.appendChild(document.create.div("ui attached segment") {
                    style = "word-break: break-word"
                })

                for ((i, el) in stackTrace.getElements(true).withIndex()) {
                    logData.appendChild(document.create.span {
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
                    })
                }
            } else {
                logData.appendChild(document.create.span {
                    +log.data
                })
            }
        }

        val spanLogDiv = document.getElementById("span_log_div")!!
        if (gotLogs) {
            spanLogDiv.removeClass("displaynone")
        } else {
            spanLogDiv.addClass("displaynone")
        }
    }

    private fun setupUI() {
        resetUI(TraceDisplayType.TRACES)

        @Suppress("UNUSED_VARIABLE") val traceScrollConfig = json(
            "once" to false, "observeChanges" to true,
            "onTopVisible" to {
                if (jq("#top_trace_table")[0].rows.length >= 10) {
                    eb.send(FetchMoreTraces, json("portalUuid" to portalUuid, "pageNumber" to 1))
                }
            },
            "onBottomVisible" to {
                if (jq("#top_trace_table")[0].rows.length >= 10) {
                    eb.send(FetchMoreTraces, json("portalUuid" to portalUuid))
                }
            }
        )
        js("\$('#top_trace_table').visibility(traceScrollConfig)")
        js("\$('#latest_traces_header').dropdown({on: null})")
        js("\$('#trace_stack_header').dropdown({on: 'hover'})")

        if (!configuration.visibleActivity) {
            jq("#activity_link").css("display", "none")
            jq("#sidebar_activity_link").css("display", "none")
        }

        jq("input[type='text']").on("click") {
            jq(this).select()
        }

        window.setInterval({ updateOccurredLabels(".trace_time") }, 2000)
    }

    //todo: clean up
    private fun resetUI(traceDisplayType: TraceDisplayType, traceOrderType: TraceOrderType? = null) {
        when (traceDisplayType) {
            TraceDisplayType.TRACES -> {
                if (traceOrderType != null) {
                    jq("#latest_traces_header_text").text(translate(traceOrderType.fullDescription))
                }
                jq("#span_info_panel").css("display", "none")
                jq("#latest_traces_header").addClass("active_sub_tab")
                    .removeClass("inactive_tab")
                jq("#top_trace_table").css("display", "")
                jq("#trace_stack_table").css("visibility", "hidden")
                jq("#traces_span").css("display", "unset")
                jq("#trace_stack_span").css("display", "none")
                jq("#segment_id_span").css("display", "none")

                jq("#trace_stack_header").addClass("inactive_tab")
                    .removeClass("active_sub_tab")
                    .css("visibility", "hidden")

                jq("#span_info_header").addClass("inactive_tab")
                    .removeClass("active_sub_tab")
                    .css("visibility", "hidden")

                jq("#trace_table").empty()
            }
            TraceDisplayType.TRACE_STACK -> {
                jq("#latest_traces_header").removeClass("active")
                jq("#span_info_panel").css("display", "none")
                jq("#top_trace_table").css("display", "none")
                jq("#trace_stack_table").css("display", "")
                    .css("visibility", "visible")
                jq("#segment_id_span").css("display", "none")
                jq("#trace_stack_span").css("display", "unset")

                jq("#trace_stack_header").removeClass("inactive_tab")
                    .addClass("active_sub_tab")
                    .css("visibility", "visible")

                jq("#span_info_header").removeClass("active")
                    .css("visibility", "hidden")

                jq("#trace_stack_table").css("visibility", "visible")
                jq("#traces_span").css("display", "none")

                jq("#latest_traces_header").removeClass("active_sub_tab")
                    .addClass("inactive_tab")

                jq("#trace_stack_header").addClass("active_sub_tab")
                    .removeClass("inactive_tab")
                    .css("visibility", "visible")

                jq("#span_info_header").removeClass("active_sub_tab")
                    .css("visibility", "hidden")

                jq("#stack_table tr").remove()


                jq("#traces_span").css("display", "none")
            }
            TraceDisplayType.SPAN_INFO -> {
                jq("#top_trace_table").css("display", "none")
                jq("#trace_stack_table").css("visibility", "visible")
                jq("#segment_id_span").css("display", "unset")
                jq("#trace_stack_span").css("display", "none")

                jq("#trace_stack_header").css("visibility", "visible")
                    .removeClass("active_sub_tab")
                    .addClass("inactive_tab")
                jq("#latest_traces_header").removeClass("active_sub_tab")
                    .addClass("inactive_tab")

                jq("#span_info_header").addClass("active_sub_tab")
                    .removeClass("inactive_tab")
                    .css("visibility", "visible")
                jq("#trace_stack_table").css("display", "none")
                    .css("visibility", "hidden")
                jq("#span_info_panel").css("display", "")
                    .css("visibility", "visible")

                jq("#tag_table tr").remove()
            }
        }
    }

    private fun clickedBackToTraces() {
        eb.send(ClickedDisplayTraces, json("portalUuid" to portalUuid))
    }

    private fun clickedBackToTraceStack() {
        eb.send(ClickedDisplayTraceStack, json("portalUuid" to portalUuid))
    }

    private fun clickedDisplayTraceStack(artifactQualifiedName: String, globalTraceId: String) {
        eb.send(
            ClickedDisplayTraceStack,
            json(
                "portalUuid" to portalUuid,
                "artifactQualifiedName" to artifactQualifiedName,
                "traceId" to globalTraceId
            )
        )
    }

    private fun clickedDisplayInnerTraceStack(traceId: String, segmentId: String, spanId: Int) {
        eb.send(
            ClickedDisplayInnerTraceStack,
            json(
                "portalUuid" to portalUuid,
                "traceId" to traceId,
                "segmentId" to segmentId,
                "spanId" to spanId
            )
        )
    }

    private fun clickedDisplaySpanInfo(
        rootArtifactQualifiedName: String,
        traceId: String,
        segmentId: String,
        spanId: Int
    ) {
        eb.send(
            ClickedDisplaySpanInfo,
            json(
                "portalUuid" to portalUuid,
                "artifactQualifiedName" to rootArtifactQualifiedName,
                "traceId" to traceId,
                "segmentId" to segmentId,
                "spanId" to spanId
            )
        )
    }

    companion object {
        val COMPONENT_MAPPINGS = mapOf(
            "mongodb-driver" to "MongoDB",
            "rocketMQ-producer" to "RocketMQ",
            "rocketMQ-consumer" to "RocketMQ",
            "kafka-producer" to "Kafka",
            "kafka-consumer" to "Kafka",
            "activemq-producer" to "ActiveMQ",
            "activemq-consumer" to "ActiveMQ",
            "postgresql-jdbc-driver" to "PostgreSQL",
            "Xmemcached" to "Memcached",
            "Spymemcached" to "Memcached",
            "h2-jdbc-driver" to "H2",
            "mysql-connector-java" to "Mysql",
            "Jedis" to "Redis",
            "Redisson" to "Redis",
            "Lettuce" to "Redis",
            "Zookeeper" to "Zookeeper",
            "StackExchange.Redis" to "Redis",
            "SqlClient" to "SqlServer",
            "Npgsql" to "PostgreSQL",
            "MySqlConnector" to "Mysql",
            "EntityFrameworkCore.InMemory" to "InMemoryDatabase",
            "EntityFrameworkCore.SqlServer" to "SqlServer",
            "EntityFrameworkCore.Sqlite" to "SQLite",
            "Pomelo.EntityFrameworkCore.MySql" to "Mysql",
            "Npgsql.EntityFrameworkCore.PostgreSQL" to "PostgreSQL",
            "transport-client" to "Elasticsearch",
            "rest-high-level-client" to "Elasticsearch",
            "SolrJ" to "Solr",
            "cassandra-java-driver" to "Cassandra",
            "mariadb-jdbc" to "Mariadb",

            //Custom added mappings
            "SpringRestTemplate" to "SpringMVC"
        )
    }
}
