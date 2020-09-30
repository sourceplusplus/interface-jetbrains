package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.extensions.eb
import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.extensions.moment
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
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpanInfoType.END_TIME
import com.sourceplusplus.protocol.artifact.trace.TraceSpanInfoType.START_TIME
import com.sourceplusplus.protocol.artifact.trace.TraceStackHeaderType.TIME_OCCURRED
import com.sourceplusplus.protocol.artifact.trace.TraceStackHeaderType.TRACE_ID
import com.sourceplusplus.protocol.artifact.trace.TraceTableType.*
import com.sourceplusplus.protocol.portal.PageType.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.dom.append
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.math.round
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
        console.log("Traces tab started")
        eb.onopen = {
            js("portalConnected()")
            eb.registerHandler(DisplayTraces(portalUuid)) { error: String, message: Any ->
                js("displayTraces(message.body)")
            }
            eb.registerHandler(DisplayInnerTraceStack(portalUuid)) { error: String, message: Any ->
                js("displayInnerTraces(message.body)")
            }
            eb.registerHandler(DisplayTraceStack(portalUuid)) { error: String, message: Any ->
                js("displayTraceStack(message.body)")
            }
            eb.registerHandler(DisplaySpanInfo(portalUuid)) { error: String, message: Any ->
                js("displaySpanInfo(message.body)")
            }

            eb.publish(TracesTabOpened,  "{'portal_uuid': '$portalUuid', 'trace_order_type': traceOrderType}")
        }

        setupUI()
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

    private fun setupUI() {
        if (js("hideOverviewTab") as Boolean) {
            jq("#overview_link").css("display", "none")
            jq("#sidebar_overview_link").css("display", "none")
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

        when (js("traceOrderType") as String) {
            "LATEST_TRACES" -> jq("#latest_traces_header_text").text("Latest Traces")
            "SLOWEST_TRACES" -> jq("#latest_traces_header_text").text("Slowest Traces")
            "FAILED_TRACES" -> jq("#latest_traces_header_text").text("Failed Traces")
        }

        jq("input[type='text']").on("click", fun() {
            jq(this).select()
        })

        window.setInterval(updateOccurredLabels(), 2000)
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

    fun displayTraces(traceResult: TraceResult) {
        console.log("""Displaying traces - Artifact: ${traceResult.artifactSimpleName} 
            - From: ${moment.unix(traceResult.start).format() as String} - To: ${moment.unix(traceResult.stop).format() as String} 
            - Order type: ${traceResult.orderType} - Amount: ${traceResult.traces.size}
        """)

        for (i in traceResult.traces.indices) {
            val trace = traceResult.traces[i];
            val globalTraceId = trace.traceIds[0];
            val htmlTraceId = globalTraceId.split(".").joinToString("");
            var operationName = trace.operationNames[0];
            if (operationName == traceResult.artifactQualifiedName) {
                operationName = traceResult.artifactSimpleName.toString();
            }

            var rowHtml = """<tr id="trace-${htmlTraceId}"><td onclick='clickedDisplayTraceStack("${traceResult.appUuid}","
                             ${traceResult.artifactQualifiedName}","$globalTraceId");' style="border-top: 0 !important; padding-left: 20px;">
                          """
            rowHtml += """<i style="font-size:1.5em;margin-right:5px" class="far fa-plus-square"></i>"""
            rowHtml += """<span style="vertical-align:top">"""
            rowHtml += operationName.replace("<", "&lt;").replace(">", "&gt;")
            rowHtml += "</span>"
            rowHtml += "</td>"

            val occurred = moment(trace.start);
            val now = moment()
            val timeOccurredDuration = moment.duration(now.diff(occurred))
            rowHtml += """<td class="trace_time collapsing" id="trace_time_$htmlTraceId" data-value="${trace.start}" style="text-align: center">
                          ${getPrettyDuration(timeOccurredDuration, 1)}</td>
                       """
            rowHtml += """<td class="collapsing">${trace.prettyDuration}</td>"""

            rowHtml += if (trace.error!!) {
                """<td class="collapsing" style="padding: 0; text-align: center; font-size: 20px"><i class="exclamation triangle red icon"></i></td></tr>"""
            } else {
                """<td class="collapsing" style="padding: 0; text-align: center; color:#808083; font-size: 20px"><i class="check icon"></i></td></tr>"""
            }

            val traceTable: dynamic = document.getElementById("trace_table")
            val tableRow = traceTable.rows[i];
            if (tableRow != null) {
                if (tableRow.id != "trace-$htmlTraceId") {
                    traceTable.rows[i].outerHTML = rowHtml;
                }
            } else {
                jq("#trace_table").append(rowHtml);
            }
        }

        updateOccurredLabels();
    }

    private fun updateOccurredLabels() {
        jq(".trace_time").each(fun(i: Int, traceTime: HTMLElement) {
            if (!traceTime.dataset["value"].isNullOrEmpty()) {
                val occurred = moment((traceTime.dataset["value"])?.toLong())
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
            prettyDuration = (months + "mo " + (round(((durationDiff.asWeeks() * 10) / 10) as Double)).toFixed(decimalPlaces)) as String
            postText = "w ago"
        } else if (duration.weeks() > 0) {
            val weeks = duration.weeks()
            val durationDiff = duration.subtract(weeks, "weeks")
            prettyDuration = (weeks + "w " + (round(((durationDiff.asDays() * 10) / 10) as Double)).toFixed(decimalPlaces)) as String
            postText = "d ago"
        } else if (duration.days() > 0) {
            val days = duration.days()
            val durationDiff = duration.subtract(days, "days")
            prettyDuration = (days + "d " + (round(((durationDiff.asHours() * 10) / 10) as Double)).toFixed(decimalPlaces)) as String
            postText = "h ago"
        } else if (duration.hours() > 0) {
            val hours = duration.hours()
            val durationDiff = duration.subtract(hours, "hours")
            prettyDuration = (hours + "h " + (round(((durationDiff.asMinutes() * 10) / 10) as Double)).toFixed(decimalPlaces)) as String
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
            "SpringRestTemplate" to "SpringMVC"
        )
    }
}

fun Double.toFixed(digits: Int): String = this.asDynamic().toFixed(digits) as String
