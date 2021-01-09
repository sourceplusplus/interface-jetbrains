package com.sourceplusplus.portal.template

import com.sourceplusplus.portal.model.PageType
import com.sourceplusplus.portal.model.PageType.*
import com.sourceplusplus.portal.toggleSidebar
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import kotlinx.html.*
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event

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

fun FlowContent.subMenuItem(traceOrderType: TraceOrderType, onClick: (Event) -> Unit) {
    a(classes = "item sidebar_sub_text_color") {
        onClickFunction = {
            toggleSidebar()
            onClick.invoke(it)
        }
        +traceOrderType.description
    }
}
