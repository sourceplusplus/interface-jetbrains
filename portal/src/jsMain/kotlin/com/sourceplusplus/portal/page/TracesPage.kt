package com.sourceplusplus.portal.page

import com.bfergerson.vertx3.eventbus.EventBus
import com.sourceplusplus.portal.clickedViewAsExternalPortal
import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.extensions.toFixed
import com.sourceplusplus.portal.extensions.toMoment
import com.sourceplusplus.portal.extensions.toPrettyDuration
import com.sourceplusplus.portal.model.PageType.*
import com.sourceplusplus.portal.model.TraceDisplayType
import com.sourceplusplus.portal.model.TraceSpanInfoType.END_TIME
import com.sourceplusplus.portal.model.TraceSpanInfoType.START_TIME
import com.sourceplusplus.portal.model.TraceStackHeaderType.TIME_OCCURRED
import com.sourceplusplus.portal.model.TraceStackHeaderType.TRACE_ID
import com.sourceplusplus.portal.model.TraceTableType.*
import com.sourceplusplus.portal.template.*
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedDisplaySpanInfo
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedDisplayTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedDisplayTraces
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedStackTraceElement
import com.sourceplusplus.protocol.ProtocolAddress.Global.TracesTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayInnerTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplaySpanInfo
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayTraces
import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType.*
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpan
import com.sourceplusplus.protocol.artifact.trace.TraceSpanInfo
import com.sourceplusplus.protocol.portal.PortalConfiguration
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
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.get
import kotlin.js.json

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TracesPage(
    override val portalUuid: String,
    private val eb: EventBus,
    override var traceOrderType: TraceOrderType = LATEST_TRACES,
    override var traceDisplayType: TraceDisplayType = TraceDisplayType.TRACES
) : ITracesPage {

    private lateinit var configuration: PortalConfiguration

    override fun setupEventbus() {
        eb.registerHandler(DisplayTraces(portalUuid)) { _: dynamic, message: dynamic ->
            displayTraces(Json.decodeFromDynamic(message.body))
        }
        eb.registerHandler(DisplayInnerTraceStack(portalUuid)) { _: dynamic, message: dynamic ->
            displayTraceStack(*Json.decodeFromDynamic(message.body))
        }
        eb.registerHandler(DisplayTraceStack(portalUuid)) { _: dynamic, message: dynamic ->
            displayTraceStack(*Json.decodeFromDynamic(message.body))
        }
        eb.registerHandler(DisplaySpanInfo(portalUuid)) { _: dynamic, message: dynamic ->
            displaySpanInfo(Json.decodeFromDynamic(message.body))
        }
        eb.publish(TracesTabOpened, json("portalUuid" to portalUuid, "traceOrderType" to traceOrderType.name))
    }

    override fun renderPage(portalConfiguration: PortalConfiguration) {
        console.log("Rending Traces page")
        this.configuration = portalConfiguration

        val root: Element = document.getElementById("root")!!
        root.innerHTML = ""
        root.append {
            portalNav {
                if (configuration.visibleOverview) navItem(OVERVIEW)
                if (configuration.visibleActivity) navItem(ACTIVITY)
                if (configuration.visibleTraces) navItem(TRACES, isActive = true) {
                    navSubItem(LATEST_TRACES, SLOWEST_TRACES, FAILED_TRACES)
                }
                if (configuration.visibleConfiguration) navItem(CONFIGURATION, isActive = true)
            }
            pusherContent {
                navBar {
                    tracesHeader(
                        TRACE_ID, TIME_OCCURRED,
                        onClickBackToTraces = { clickedBackToTraces() },
                        onClickBackToTraceStack = { clickedBackToTraceStack() }
                    )
                    rightAlign {
                        externalPortalButton { clickedViewAsExternalPortal(eb) }
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

        setupUI()
    }

    override fun displayTraces(traceResult: TraceResult) {
        console.log("Displaying ${traceResult.total} traces")
        traceDisplayType = TraceDisplayType.TRACES
        resetUI()

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
                td {
                    onClickFunction = {
                        clickedDisplayTraceStack(
                            traceResult.appUuid,
                            traceResult.artifactQualifiedName,
                            globalTraceId
                        )
                    }
                    style = "border-top: 0 !important; padding-left: 20px"
                    i {
                        style = "font-size:1.5em;margin-right:5px"
                        classes = setOf("far", "fa-plus-square")
                    }
                    span {
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
                    attributes["data-value"] = trace.start.toEpochMilliseconds().toString()
                    style = "text-align: center"
                    +timeOccurredDuration.toPrettyDuration(1)
                }
                td {
                    classes += "collapsing"
                    +trace.prettyDuration!!
                }
                td {
                    classes += "collapsing"
                    style = "padding: 0; text-align: center; font-size: 20px"
                    if (trace.error!!) {
                        i {
                            classes = setOf("exclamation", "triangle", "red", "icon")
                        }
                    } else {
                        style.plus("color: #808083")
                        i {
                            classes = setOf("check", "icon")
                        }
                    }
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
        updateOccurredLabels()
    }

    override fun displayTraceStack(vararg traceStack: TraceSpanInfo) {
        console.log("Displaying trace stack")
        traceDisplayType = TraceDisplayType.TRACE_STACK
        resetUI()

        if (traceStack[0].innerLevel > 0) {
            jq("#latest_traces_header_text").text("Parent Stack")
        } else {
            when (traceOrderType) {
                LATEST_TRACES -> jq("#latest_traces_header_text").text("Latest Traces")
                SLOWEST_TRACES -> jq("#latest_traces_header_text").text("Slowest Traces")
                FAILED_TRACES -> jq("#latest_traces_header_text").text("Failed Traces")
            }
        }

        jq("#trace_id_field").`val`(traceStack[0].span.traceId)
        jq("#time_occurred_field").`val`(traceStack[0].span.startTime.toMoment().format())

        for (i in traceStack.indices) {
            val spanInfo = traceStack[i]
            val span = spanInfo.span

            val rowHtml = document.create.tr {
                td {
                    onClickFunction = {
                        clickedDisplaySpanInfo(
                            spanInfo.appUuid,
                            spanInfo.rootArtifactQualifiedName,
                            span.traceId,
                            span.segmentId,
                            span.spanId
                        )
                    }
                    style = "border-top: 0 !important; padding-left: 20px"
                    if (!COMPONENT_MAPPINGS[span.component].isNullOrEmpty() || span.component != "Unknown") {
                        var component = COMPONENT_MAPPINGS[span.component]
                        if (component == null) {
                            component = span.component
                        }
                        img {
                            style = "margin-right:5px;vertical-align:bottom"
                            width = "18px"
                            height = "18px"
                            src = "../themes/default/assets/components/${component?.toUpperCase()}.png"
                        }
                        +spanInfo.operationName.replace("<", "&lt;").replace(">", "&gt;")
                    } else if (span.hasChildStack!! || (!configuration.external && !span.artifactQualifiedName.isNullOrEmpty() && i > 0)) {
                        i {
                            style = "font-size:1.5em;margin-right:5px;vertical-align:bottom"
                            classes = setOf("far", "fa-plus-square")
                        }
                        +spanInfo.operationName.replace("<", "&lt;").replace(">", "&gt;")
                    } else {
                        i {
                            style = "font-size:1.5em;margin-right:5px"
                            classes = setOf("far", "fa-info-square")
                        }
                        span {
                            style = "vertical-align:top"
                            +spanInfo.operationName.replace("<", "&lt;").replace(">", "&gt;")
                        }
                    }
                }
                td {
                    classes += "collapsing"
                    +spanInfo.timeTook
                }
                td {
                    div {
                        classes = setOf("ui", "red", "progress")
                        id = "trace_bar_${i}"
                        attributes["data-percent"] = spanInfo.totalTracePercent.toString()
                        style = "margin: 0"
                        div {
                            classes += "bar"
                            style = "transition-duration: 300ms; display: block; width: ${
                                spanInfo.totalTracePercent.toFixed(4) //todo: toFixed needed?
                            }%"
                        }
                    }
                }
                td {
                    classes += "collapsing"
                    style = "padding: 0; text-align: center; font-size: 20px"
                    when {
                        span.error!! -> {
                            i {
                                classes = setOf("skull", "crossbones", "red", "icon")
                            }
                        }
                        span.childError && i > 0 -> {
                            i {
                                classes = setOf("exclamation", "triangle", "red", "icon")
                            }
                        }
                        else -> {
                            style.plus("color: #808083")
                            i {
                                classes = setOf("check", "icon")
                            }
                        }
                    }
                }
            }

            jq("#stack_table").append(rowHtml)
        }
    }

    override fun displaySpanInfo(spanInfo: TraceSpan) {
        console.log("Displaying span info")
        traceDisplayType = TraceDisplayType.SPAN_INFO
        resetUI()

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
            val stackTrace = JvmStackTrace.fromString(log.data)
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
                    style = "white-space: break-word"
                })

                for ((i, el) in stackTrace.elements.withIndex()) {
                    logData.appendChild(document.create.span {
                        if (i > 0) br
                        unsafe { +"&emsp;" }
                        if (el.sourceAsLineNumber != null) {
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
        resetUI()

        js("\$('#latest_traces_header').dropdown({on: null})")
        js("\$('#trace_stack_header').dropdown({on: 'hover'})")

        if (!configuration.visibleActivity) {
            jq("#activity_link").css("display", "none")
            jq("#sidebar_activity_link").css("display", "none")
        }

        jq("input[type='text']").on("click") {
            jq(this).select()
        }

        window.setInterval({ updateOccurredLabels() }, 2000)
    }

    //todo: clean up
    private fun resetUI() {
        when (traceDisplayType) {
            TraceDisplayType.TRACES -> {
                when (traceOrderType) {
                    LATEST_TRACES -> jq("#latest_traces_header_text").text("Latest Traces")
                    SLOWEST_TRACES -> jq("#latest_traces_header_text").text("Slowest Traces")
                    FAILED_TRACES -> jq("#latest_traces_header_text").text("Failed Traces")
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

                jq("#top_trace_table").css("display", "none")
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

    private fun updateOccurredLabels() {
        jq(".trace_time").each(fun(_: Int, traceTime: HTMLElement) {
            if (!traceTime.dataset["value"].isNullOrEmpty()) {
                val occurred = moment(traceTime.dataset["value"]!!, "x")
                val now = moment(moment.now())
                val timeOccurredDuration = moment.duration(now.diff(occurred))
                traceTime.innerText = timeOccurredDuration.toPrettyDuration(1)
            }
        })
    }

    private fun clickedBackToTraces() {
        eb.send(ClickedDisplayTraces, json("portalUuid" to portalUuid))
    }

    private fun clickedBackToTraceStack() {
        eb.send(ClickedDisplayTraceStack, json("portalUuid" to portalUuid))
    }

    private fun clickedDisplayTraceStack(appUuid: String, artifactQualifiedName: String, globalTraceId: String) {
        eb.send(
            ClickedDisplayTraceStack,
            json(
                "portalUuid" to portalUuid,
                "appUuid" to appUuid,
                "artifactQualifiedName" to artifactQualifiedName,
                "traceId" to globalTraceId
            )
        )
    }

    private fun clickedDisplaySpanInfo(
        appUuid: String,
        rootArtifactQualifiedName: String,
        traceId: String,
        segmentId: String,
        spanId: Int
    ) {
        eb.send(
            ClickedDisplaySpanInfo,
            json(
                "portalUuid" to portalUuid,
                "appUuid" to appUuid,
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
