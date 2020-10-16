package com.sourceplusplus.portal.template

import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.portal.PageType
import com.sourceplusplus.protocol.portal.PageType.*
import kotlinx.html.*

fun FlowContent.tabs(block: FlowContent.() -> Unit) {
    block()
}

fun FlowContent.tabItem(pageType: PageType, isActive: Boolean, block: (FlowContent.() -> Unit)? = null) {
    when (pageType) {
        OVERVIEW, ACTIVITY, CONFIGURATION -> apply {
            if (isActive) {
                a(classes = "ui dropdown item active_tab") {
                    i(pageType.icon)
                }
            } else {
                a(classes = "ui item hide_on_toggle") {
                    id = "${pageType.name.toLowerCase()}_link"
                    href = pageType.location
                    i("${pageType.icon} inactive_tab")
                }
            }
        }
        TRACES -> apply {
            var activeClass = "active_tab"
            if (!isActive) {
                activeClass = "inactive_tab"
            }
            div("ui dropdown item $activeClass") {
                unsafe {
                    +"""<z class="displaynone">Traces</z>"""
                }
                i(pageType.icon)
                block?.let { it() }
            }
        }
    }
}

fun FlowContent.subTabItem(vararg traceOrderTypes: TraceOrderType = arrayOf()) {
    div("menu secondary_background_color") {
        for (traceType in traceOrderTypes) {
            a(classes = "item") {
                id = "traces_link_${traceType.id}"
                href = traceType.id
                span("menu_tooltip_text") { +traceType.description }
            }
        }
    }
}
