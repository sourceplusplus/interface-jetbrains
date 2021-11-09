package com.sourceplusplus.portal.template

import com.sourceplusplus.portal.PortalBundle.translate
import spp.protocol.portal.PageType
import kotlinx.html.*
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event

fun FlowContent.tabs(block: FlowContent.() -> Unit) {
    block()
}

fun FlowContent.tabItem(
    pageType: PageType,
    isActive: Boolean,
    onClick: ((Event) -> Unit)?,
    block: (FlowContent.() -> Unit)? = null
) {
    if (pageType.hasChildren) {
        val activeClass = if (isActive) "active_tab" else "inactive_tab"
        div("ui dropdown item $activeClass") {
            unsafe {
                +"""<z class="displaynone">Traces</z>"""
            }

            i(pageType.icon + " $activeClass")
            block?.let { it() }
        }
    } else {
        if (isActive) {
            a(classes = "ui dropdown item active_tab") {
                i(pageType.icon)
            }
        } else {
            a(classes = "ui item hide_on_toggle") {
                if (onClick != null) onClickFunction = onClick
                i("${pageType.icon} inactive_tab")
            }
        }
    }
}

fun FlowContent.subTabItem(vararg subItems: PortalNavSubItem) {
    div("menu secondary_background_color") {
        for (item in subItems) {
            a(classes = "item") {
                onClickFunction = item.onClick
                span("menu_tooltip_text") { +translate(item.orderType.description) }
            }
        }
    }
}
