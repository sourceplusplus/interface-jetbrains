package com.sourceplusplus.portal

import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.page.ActivityPage
import com.sourceplusplus.portal.page.ConfigurationPage
import com.sourceplusplus.portal.page.OverviewPage
import com.sourceplusplus.portal.page.TracesPage
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import kotlinx.browser.window

fun main() {
    jq().ready {
        val queryParams = getQueryMap()
        val portalUuid = queryParams.getOrElse("portalUuid", { "null" })
        val requiresRegistration = queryParams.getOrElse("requires_registration", { "false" }).toBoolean()
        val externalPortal = queryParams.getOrElse("external", { "false" }).toBoolean()
        val hideActivityTab = queryParams.getOrElse("hide_activity_tab", { "false" }).toBoolean()
        val darkMode = queryParams.getOrElse("dark_mode", { "false" }).toBoolean()

        when (window.location.pathname) {
            "/activity", "/activity.html" -> ActivityPage(portalUuid, externalPortal, darkMode).renderPage()
            "/traces", "/traces.html" -> {
                val traceOrderType = TraceOrderType.valueOf(
                    queryParams.getOrElse("order_type", { "LATEST_TRACES" }).toUpperCase()
                )
                TracesPage(
                    portalUuid,
                    externalPortal,
                    hideActivityTab,
                    traceOrderType,
                    darkMode = darkMode
                ).renderPage()
            }
            "/configuration", "/configuration.html" ->
                ConfigurationPage(portalUuid, externalPortal, hideActivityTab, darkMode).renderPage()
            else -> OverviewPage(portalUuid, externalPortal, darkMode).renderPage()
        }
        //todo: portals should have ability to cache pages so they don't need re-init

        var mainGetQuery = "?portalUuid=$portalUuid"
        var mainGetQueryWithoutPortalUuid = ""
        if (externalPortal) mainGetQueryWithoutPortalUuid += "&external=true"
        if (darkMode) mainGetQueryWithoutPortalUuid += "&dark_mode=true"
        if (hideActivityTab) mainGetQueryWithoutPortalUuid += "&hide_activity_tab=true"
        mainGetQuery += mainGetQueryWithoutPortalUuid

        loadTheme(mainGetQuery)
    }
}

//todo: impl
/*
function clickedViewAsExternalPortal() {
    eb.send('ClickedViewAsExternalPortal', {
        'portalUuid': portalUuid
    }, function (error, message) {
        window.open(window.location.href.split('?')[0] + '?portalUuid=' + message.body.portalUuid
            + '&external=true' + mainGetQueryWithoutPortalUuid, '_blank');
    });
}

function portalConnected() {
    console.log("Portal successfully connected. Portal UUID: " + portalUuid);
    if (requiresRegistration) {
        eb.send("REGISTER_PORTAL", {
            'appUuid': findGetParameter("appUuid"),
            'artifactQualifiedName': findGetParameter("artifactQualifiedName")
        }, function (error, message) {
            window.open(window.location.href.split('?')[0] + '?portalUuid=' + message.body.portalUuid
                + mainGetQueryWithoutPortalUuid, '_self');
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

fun loadTheme(mainGetQuery: String) {
    js("\$('.ui.calendar').calendar()")
    js("\$('.ui.dropdown').dropdown()")
    js("\$('.ui.sidebar').sidebar('setting', 'transition', 'overlay')")
    js("\$('.ui.progress').progress()")

    jq(".openbtn").on("click", fun() {
        jq(".ui.sidebar").toggleClass("very thin icon")
        jq(".asd").toggleClass("marginlefting")
        jq(".sidebar z").toggleClass("displaynone")
        jq(".ui.accordion").toggleClass("displaynone")
        jq(".ui.dropdown.item").toggleClass("displaynone")
        jq(".hide_on_toggle").toggleClass("displaynone")
        jq(".pusher").toggleClass("dimmed")

        jq(".logo").find("img").toggle()
    })
    js("\$('.ui.accordion').accordion({selector: {}})")

    jq("#overview_link").attr("href", "overview.html$mainGetQuery")
    jq("#sidebar_overview_link").attr("href", "overview.html$mainGetQuery")

    jq("#activity_link").attr("href", "activity.html$mainGetQuery")
    jq("#sidebar_activity_link").attr("href", "activity.html$mainGetQuery")

    jq("#traces_link_latest").attr("href", "traces.html$mainGetQuery&order_type=latest_traces")
    jq("#traces_link_slowest").attr("href", "traces.html$mainGetQuery&order_type=slowest_traces")
    jq("#traces_link_failed").attr("href", "traces.html$mainGetQuery&order_type=failed_traces")
    jq("#sidebar_traces_link_latest").attr("href", "traces.html$mainGetQuery&order_type=latest_traces")
    jq("#sidebar_traces_link_slowest").attr("href", "traces.html$mainGetQuery&order_type=slowest_traces")
    jq("#sidebar_traces_link_failed").attr("href", "traces.html$mainGetQuery&order_type=failed_traces")

    jq("#configuration_link").attr("href", "configuration.html$mainGetQuery")
    jq("#sidebar_configuration_link").attr("href", "configuration.html$mainGetQuery")
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

external fun decodeURIComponent(encodedURI: String): String
