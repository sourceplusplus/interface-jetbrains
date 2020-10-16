package com.sourceplusplus.portal.template

import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.portal.PageType
import com.sourceplusplus.protocol.portal.PageType.*
import kotlinx.html.*

fun FlowContent.menu(block: FlowContent.() -> Unit) {
    div("ui accordion displaynone") {
        a(classes = "item openbtn openbtn_background_white") {
            +"Close menu"
        }
        block()
    }
    div("ui dropdown item openbtn") {
        i("icon demo-icon content white_color")
    }
}

fun FlowContent.menuItem(pageType: PageType, isActive: Boolean, block: (FlowContent.() -> Unit)? = null) {
    when (pageType) {
        OVERVIEW, ACTIVITY, CONFIGURATION -> apply {
            if (isActive) {
                a(classes = "item active_tab") { +pageType.title }
            } else {
                a(classes = "item inactive_tab") {
                    id = "sidebar_${pageType.name.toLowerCase()}_link"
                    href = pageType.location
                    +pageType.title
                }
            }
        }
        TRACES -> apply {
            var activeClass = "active_tab"
            if (!isActive) {
                activeClass = "inactive_tab"
            }
            div("title item $activeClass") {
                i("dropdown icon")
                +"Traces"
            }
            div("content") {
                block?.let { it() }
            }
        }
    }
}

fun FlowContent.subMenuItem(vararg traceOrderTypes: TraceOrderType = arrayOf()) {
    for (traceType in traceOrderTypes) {
        a(classes = "item sidebar_sub_text_color") {
            id = "sidebar_traces_link_${traceType.id}"
            href = traceType.id
            +traceType.description
        }
    }
}
