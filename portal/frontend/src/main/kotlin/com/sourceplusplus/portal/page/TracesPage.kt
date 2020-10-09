package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.extensions.eb
import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.extensions.moment
import com.sourceplusplus.portal.template.*
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClickedDisplaySpanInfo
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClickedDisplayTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClickedDisplayTraces
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.PortalLogger
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.TracesTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.DisplayInnerTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.DisplaySpanInfo
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.DisplayTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.DisplayTraces
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType.*
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpanInfo
import com.sourceplusplus.protocol.artifact.trace.TraceSpanInfoType.END_TIME
import com.sourceplusplus.protocol.artifact.trace.TraceSpanInfoType.START_TIME
import com.sourceplusplus.protocol.artifact.trace.TraceStackHeaderType.TIME_OCCURRED
import com.sourceplusplus.protocol.artifact.trace.TraceStackHeaderType.TRACE_ID
import com.sourceplusplus.protocol.artifact.trace.TraceTableType.*
import com.sourceplusplus.protocol.portal.PageType.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTableRowElement
import org.w3c.dom.get
import kotlin.js.json
import kotlin.math.round

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@ExperimentalSerializationApi
class TracesPage(
    private val portalUuid: String,
    private val externalPortal: Boolean,
    private val hideOverviewTab: Boolean,
    private val traceOrderType: TraceOrderType
) {
    init {
        console.log("Traces tab started")
        setupUI()
        eb.onopen = {
            js("portalConnected()")
            eb.registerHandler(DisplayTraces(portalUuid)) { error: String, message: dynamic ->
                val body: dynamic = message.body
                val traceResult: TraceResult = Json.decodeFromDynamic(body)
                displayTraces(traceResult)
            }
            eb.registerHandler(DisplayInnerTraceStack(portalUuid)) { error: String, message: dynamic ->
                js("displayInnerTraces(message.body)")
            }
            eb.registerHandler(DisplayTraceStack(portalUuid)) { error: String, message: dynamic ->
                val body: dynamic = message.body
                val traceStack: Array<TraceSpanInfo> = Json.decodeFromDynamic(body)
                displayTraceStack(*traceStack)

            }
            eb.registerHandler(DisplaySpanInfo(portalUuid)) { error: String, message: dynamic ->
                js("displaySpanInfo(message.body)")
            }

            eb.publish(TracesTabOpened, "{'portalUuid': '$portalUuid', 'traceOrderType': '$traceOrderType'}")
        }
    }

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

    private fun setupUI() {
        if (hideOverviewTab) {
            jq("#overview_link").css("display", "none")
            jq("#sidebar_overview_link").css("display", "none")
        }

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

        jq("input[type='text']").on("click") {
            jq(this).select()
        }

        window.setInterval({ updateOccurredLabels() }, 2000)
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

    fun clickedBackToTraces() {
        eb.send(
            ClickedDisplayTraces,
            json(
                "portalUuid" to portalUuid
            )
        )
    }

    fun clickedBackToTraceStack() {
        eb.send(
            ClickedDisplayTraceStack,
            json(
                "portalUuid" to portalUuid
            )
        )
    }

    private fun displayTraces(traceResult: TraceResult) {
        console.log(
            """>>>>>>>>>> Displaying traces - Artifact: ${traceResult.artifactSimpleName} 
            - From: ${moment(traceResult.start.toString(), "x").format() as String} - To: ${
                moment(traceResult.stop.toString(), "x").format() as String
            } 
            - Order type: ${traceResult.orderType} - Amount: ${traceResult.traces.size}
        """
        )

        when (traceOrderType) {
            LATEST_TRACES -> jq("#latest_traces_header_text").text("Latest Traces")
            SLOWEST_TRACES -> jq("#latest_traces_header_text").text("Slowest Traces")
            FAILED_TRACES -> jq("#latest_traces_header_text").text("Failed Traces")
        }

        for (i in traceResult.traces.indices) {
            val trace = traceResult.traces[i]
            val globalTraceId = trace.traceIds[0]
            val htmlTraceId = globalTraceId.split(".").joinToString("")
            var operationName = trace.operationNames[0]
            if (operationName == traceResult.artifactQualifiedName) {
                operationName = traceResult.artifactSimpleName!!
            }

            val rowHtml: HTMLTableRowElement = document.create.tr {
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

                val occurred = moment(trace.start.toString(), "x")
                val now = moment()
                val timeOccurredDuration = moment.duration(now.diff(occurred))
                td {
                    classes = setOf("trace_time", "collapsing")
                    id = "trace_time_$htmlTraceId"
                    attributes["data-value"] = trace.start.toString()
                    style = "text-align: center"
                    +getPrettyDuration(timeOccurredDuration, 1)
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
            } as HTMLTableRowElement
            console.log(">>>>>>>>>> rowHtml = $rowHtml")

            val traceTable: dynamic = document.getElementById("trace_table")
            val tableRow = traceTable.rows[i]
            if (tableRow != null) {
                if (tableRow.id != "trace-$htmlTraceId") {
                    traceTable.rows[i].outerHTML = rowHtml
                }
            } else {
                jq("#trace_table").append(rowHtml)
            }
        }

        updateOccurredLabels()
    }

    private fun displayTraceStack(vararg traceStack: TraceSpanInfo) { //todo-chess-equality: [traceStack: List<TraceSpanInfo>]
        portalLog(JSON.parse(JSON.stringify(traceStack)))

        //todo: move all this stuff to setupUI()
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

        when (traceOrderType) {
            LATEST_TRACES -> jq("#latest_traces_header_text").text("Latest Traces")
            SLOWEST_TRACES -> jq("#latest_traces_header_text").text("Slowest Traces")
            FAILED_TRACES -> jq("#latest_traces_header_text").text("Failed Traces")
        }

        jq("#trace_id_field").`val`(traceStack[0].span.traceId)
        jq("#time_occurred_field").`val`(moment(traceStack[0].span.startTime.toString(), "x").format())

        for (i in traceStack.indices) {
            val spanInfo = traceStack[i]
            val span = spanInfo.span

            val rowHtml: HTMLTableRowElement = document.create.tr {
                td {
                    onClickFunction = {
                        clickedDisplaySpanInfo(
                            spanInfo.appUuid,
                            spanInfo.rootArtifactQualifiedName,
                            span.traceId!!,
                            span.segmentId,
                            span.spanId!!
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
                        +spanInfo.operationName!!.replace("<", "&lt;").replace(">", "&gt;")
                    } else if (span.hasChildStack!! || (!externalPortal && !span.artifactQualifiedName.isNullOrEmpty() && i > 0)) {
                        i {
                            style = "font-size:1.5em;margin-right:5px;vertical-align:bottom"
                            classes = setOf("far", "fa-plus-square")
                        }
                        +spanInfo.operationName!!.replace("<", "&lt;").replace(">", "&gt;")
                    } else {
                        i {
                            style = "font-size:1.5em;margin-right:5px"
                            classes = setOf("far", "fa-info-square")
                        }
                        span {
                            style = "vertical-align:top"
                            +spanInfo.operationName!!.replace("<", "&lt;").replace(">", "&gt;")
                        }
                    }
                }
                td {
                    classes += "collapsing"
                    +spanInfo.timeTook
                }
                td {
                    div {
                        classes = setOf("ui", "red", "progress", "active")
                        id = "trace_bar_${i}"
                        attributes["data-percent"] = spanInfo.totalTracePercent.toString()
                        style = "margin: 0"
                        div {
                            classes += "bar"
                            style = "transition-duration: 300ms; display: block; width: ${spanInfo.totalTracePercent.toFixed(4)}%"
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
            } as HTMLTableRowElement

            jq("#stack_table").append(rowHtml)
        }
    }

    private fun updateOccurredLabels() {
        jq(".trace_time").each(fun(i: Int, traceTime: HTMLElement) {
            if (!traceTime.dataset["value"].isNullOrEmpty()) {
                val occurred = moment((traceTime.dataset["value"]), "x")
                val now = moment()
                val timeOccurredDuration = moment.duration(now.diff(occurred))
                traceTime.innerText = getPrettyDuration(timeOccurredDuration, 1)
            }
        })
    }

    private fun getPrettyDuration(duration: dynamic, decimalPlaces: Int): String {
        var prettyDuration: String
        val postText: String
        if (duration.months() > 0) {
            val months = duration.months()
            val durationDiff = duration.subtract(months, "months")
            prettyDuration = (months + "mo " + (round(((durationDiff.asWeeks() * 10) / 10) as Double)).toFixed(
                decimalPlaces
            )) as String
            postText = "w ago"
        } else if (duration.weeks() > 0) {
            val weeks = duration.weeks()
            val durationDiff = duration.subtract(weeks, "weeks")
            prettyDuration = (weeks + "w " + (round(((durationDiff.asDays() * 10) / 10) as Double)).toFixed(
                decimalPlaces
            )) as String
            postText = "d ago"
        } else if (duration.days() > 0) {
            val days = duration.days()
            val durationDiff = duration.subtract(days, "days")
            prettyDuration = (days + "d " + (round(((durationDiff.asHours() * 10) / 10) as Double)).toFixed(
                decimalPlaces
            )) as String
            postText = "h ago"
        } else if (duration.hours() > 0) {
            val hours = duration.hours()
            val durationDiff = duration.subtract(hours, "hours")
            prettyDuration = (hours + "h " + (round(((durationDiff.asMinutes() * 10) / 10) as Double)).toFixed(
                decimalPlaces
            )) as String
            postText = "m ago"
        } else if (duration.minutes() > 0) {
            val minutes = duration.minutes()
            val durationDiff = duration.subtract(minutes, "minutes")
            val seconds = durationDiff.seconds()
            if (seconds == 0) {
                prettyDuration = (minutes + "") as String
                postText = "m ago"
            } else {
                prettyDuration = (minutes + "m " + duration.seconds() + "") as String
                postText = "s ago"
            }
        } else if (duration.seconds() > 0) {
            prettyDuration = (duration.seconds() + "") as String
            postText = "s ago"
        } else {
            prettyDuration = (round(((duration.asSeconds() * 10) / 10) as Double)).toFixed(decimalPlaces)
            postText = "s ago"
        }

        if (prettyDuration.endsWith(".0")) {
            prettyDuration = prettyDuration.substring(0, prettyDuration.length - 2)
        }
        return prettyDuration + postText
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

fun Double.toFixed(digits: Int): String = this.asDynamic().toFixed(digits) as String

fun portalLog(message: kotlin.js.Json) {
    console.log("Displaying message: ${JSON.stringify(message)}")
    eb.send(PortalLogger, message)
}
