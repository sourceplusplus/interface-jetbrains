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
        val externalPortal = queryParams.getOrElse("external", { "false" }).toBoolean()

        when (window.location.pathname) {
            "/activity", "/activity.html" -> ActivityPage(portalUuid, externalPortal).renderPage()
            "/traces", "/traces.html" -> {
                val hideActivityTab = queryParams.getOrElse("hide_activity_tab", { "false" }).toBoolean()
                val traceOrderType = TraceOrderType.valueOf(
                    queryParams.getOrElse("order_type", { "LATEST_TRACES" }).toUpperCase()
                )
                TracesPage(portalUuid, externalPortal, hideActivityTab, traceOrderType).renderPage()
            }
            "/configuration", "/configuration.html" -> ConfigurationPage(portalUuid, externalPortal).renderPage()
            else -> OverviewPage(portalUuid, externalPortal).renderPage()
        }
        //todo: portals should have ability to cache pages so they don't need re-init

        js("loadTheme();")
    }
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
