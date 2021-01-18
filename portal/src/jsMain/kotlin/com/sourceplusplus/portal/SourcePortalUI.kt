package com.sourceplusplus.portal

import com.bfergerson.vertx3.eventbus.EventBus
import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.page.ActivityPage
import com.sourceplusplus.portal.page.ConfigurationPage
import com.sourceplusplus.portal.page.OverviewPage
import com.sourceplusplus.portal.page.TracesPage
import com.sourceplusplus.protocol.ProtocolAddress
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedViewAsExternalPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.GetPortalConfiguration
import com.sourceplusplus.protocol.ProtocolAddress.Global.SetCurrentPage
import com.sourceplusplus.protocol.ProtocolAddress.Portal.RenderPage
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.portal.PageType
import com.sourceplusplus.protocol.portal.PortalConfiguration
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.dom.append
import kotlinx.html.js.link
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic
import org.w3c.dom.get
import kotlin.js.json

fun main() {
    jq().ready {
        val queryParams = getQueryMap()
        val portalUuid = queryParams.getOrElse("portalUuid", { "null" })
        val eb = EventBus("http://localhost:8888/eventbus")

        val overviewPage = OverviewPage(portalUuid, eb)
        val activityPage = ActivityPage(portalUuid, eb)
        val tracesPage = TracesPage(portalUuid, eb)
        val configurationPage = ConfigurationPage(portalUuid, eb)

        console.log("Connecting portal")
        eb.onopen = {
            console.log("Portal connected")
            eb.send(GetPortalConfiguration, portalUuid) { _, message: dynamic ->
                console.log("Received portal configuration. Portal: $portalUuid")
                val portalConfiguration = Json.decodeFromDynamic<PortalConfiguration>(message.body)
                val renderPage = when (portalConfiguration.currentPage) {
                    PageType.OVERVIEW -> overviewPage
                    PageType.ACTIVITY -> activityPage
                    PageType.TRACES -> tracesPage
                    PageType.CONFIGURATION -> configurationPage
                }
                document.getElementsByTagName("head")[0]!!.append {
                    link {
                        rel = "stylesheet"
                        type = "text/css"
                        href = "css/" + if (portalConfiguration.darkMode) "dark_style.css" else "style.css"
                    }
                }
                renderPage.renderPage(portalConfiguration)
                loadTheme()

                renderPage.setupEventbus()
            }

            eb.registerHandler(RenderPage(portalUuid)) { _: dynamic, message: dynamic ->
                val portalConfiguration = Json.decodeFromDynamic<PortalConfiguration>(message.body)
                val renderPage = when (portalConfiguration.currentPage) {
                    PageType.OVERVIEW -> overviewPage
                    PageType.ACTIVITY -> activityPage
                    PageType.TRACES -> tracesPage
                    PageType.CONFIGURATION -> configurationPage
                }
                document.getElementsByTagName("head")[0]!!.append {
                    link {
                        rel = "stylesheet"
                        type = "text/css"
                        href = "css/" + if (portalConfiguration.darkMode) "dark_style.css" else "style.css"
                    }
                }
                renderPage.renderPage(portalConfiguration)
                loadTheme()

                renderPage.setupEventbus()
            }
        }

        Unit //todo: why is this necessary
    }
}

fun setCurrentPage(eb: EventBus, portalUuid: String, pageType: PageType) {
    eb.send(SetCurrentPage, json("portalUuid" to portalUuid, "pageType" to pageType.name)) { _, message: dynamic ->
        eb.send(RenderPage(portalUuid), message.body)
    }
}

fun clickedViewAsExternalPortal(eb: EventBus, portalUuid: String) {
    eb.send(ClickedViewAsExternalPortal, json("portalUuid" to portalUuid), fun(_, message: dynamic) {
        window.open(
            "${window.location.href.split('?')[0]}?portalUuid=${message.body.portalUuid}",
            "_blank"
        )
    })
}

fun loadTheme() {
    js("\$('.ui.calendar').calendar()")
    js("\$('.ui.dropdown').dropdown()")
    js("\$('.ui.sidebar').sidebar('setting', 'transition', 'overlay')")
    js("\$('.ui.progress').progress()")
    js("\$('.ui.table').tablesort()")

    jq(".openbtn").on("click", fun() {
        toggleSidebar()
    })
    js("\$('.ui.accordion').accordion({selector: {}})")
}

fun toggleSidebar() {
    jq(".ui.sidebar").toggleClass("very thin icon")
    jq(".asd").toggleClass("marginlefting")
    jq(".sidebar z").toggleClass("displaynone")
    jq(".ui.accordion").toggleClass("displaynone")
    jq(".ui.dropdown.item").toggleClass("displaynone")
    jq(".hide_on_toggle").toggleClass("displaynone")
    jq(".pusher").toggleClass("dimmed")

    jq(".logo").find("img").toggle()
}

fun getQueryMap(): Map<String, String> {
    val queryPairs: MutableMap<String, String> = LinkedHashMap()
    if (window.location.search.isNotEmpty()) {
        val query: String = window.location.search.substring(1)
        val pairs = query.split("&").toTypedArray()
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            queryPairs[decodeURIComponent(pair.substring(0, idx))] =
                decodeURIComponent(pair.substring(idx + 1))
        }
    }
    return queryPairs
}

fun clickedTracesOrderType(eb: EventBus, portalUuid: String, traceOrderType: TraceOrderType) {
    eb.publish(
        ProtocolAddress.Global.SetTraceOrderType,
        json("portalUuid" to portalUuid, "traceOrderType" to traceOrderType.name)
    )
}

fun setActiveTime(interval: QueryTimeFrame) {
    jq("#last_5_minutes_time").removeClass("active")
    jq("#last_15_minutes_time").removeClass("active")
    jq("#last_30_minutes_time").removeClass("active")
    jq("#last_hour_time").removeClass("active")
    jq("#last_3_hours_time").removeClass("active")
    jq("#" + interval.name.toLowerCase() + "_time").addClass("active")
}

external fun decodeURIComponent(encodedURI: String): String

//todo: impl
/*
val requiresRegistration = queryParams.getOrElse("requires_registration", { "false" }).toBoolean()
function portalConnected() {
    console.log("Portal successfully connected. Portal UUID: " + portalUuid);
    if (requiresRegistration) {
        eb.send("REGISTER_PORTAL", {
            'appUuid': findGetParameter("appUuid"),
            'artifactQualifiedName': findGetParameter("artifactQualifiedName")
        }, function (error, message) {
            window.open(window.location.href.split('?')[0] + '?portalUuid=' + message.body.portalUuid, '_self');
        });
    } else if (externalPortal) {
        let keepAliveInterval = window.setInterval(function () {
            portalLog("Sent portal keep alive request. Portal UUID: " + portalUuid);
            eb.send('KeepAlivePortal', {'portalUuid': portalUuid}, function (error, message) {
                if (error) {
                    clearInterval(keepAliveInterval);
                }
            });
        }, 30000);
    }
}
 */
