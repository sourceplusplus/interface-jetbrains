package com.sourceplusplus.portal

import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.page.ConfigurationPage
import com.sourceplusplus.portal.page.OverviewPage
import com.sourceplusplus.portal.page.TracesPage
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import kotlinx.browser.window

fun main() {
    jq().ready {
        val queryParams = getQueryMap()
        val portalUuid = queryParams.getOrElse("portal_uuid", { "null" })
        when (window.location.pathname) {
            "/traces" -> {
                val externalPortal = queryParams.getOrElse("external", { "false" }).toBoolean()
                val hideOverviewTab = queryParams.getOrElse("hide_overview_tab", { "false" }).toBoolean()
                val traceOrderType = TraceOrderType.valueOf(
                    queryParams.getOrElse("order_type", { "LATEST_TRACES" }).toUpperCase()
                )
                TracesPage(portalUuid, externalPortal, hideOverviewTab, traceOrderType).renderPage()
            }
            "/configuration" -> ConfigurationPage(portalUuid).renderPage()
            else -> OverviewPage(portalUuid).renderPage()
        }

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
