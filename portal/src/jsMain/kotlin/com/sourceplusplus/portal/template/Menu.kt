package com.sourceplusplus.portal.template

import com.sourceplusplus.portal.PortalBundle.translate
import com.sourceplusplus.portal.toggleSidebar
import spp.protocol.artifact.OrderType
import spp.protocol.portal.PageType
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.i
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

fun FlowContent.menuItem(
    pageType: PageType,
    isActive: Boolean,
    onClick: ((Event) -> Unit)?,
    block: (FlowContent.() -> Unit)? = null
) {
    if (pageType.hasChildren) {
        var activeClass = "active_tab"
        if (!isActive) {
            activeClass = "inactive_tab"
        }
        div("title item $activeClass") {
            i("dropdown icon")
            +translate(pageType.title)
        }
        div("content") {
            block?.let { it() }
        }
    } else {
        if (isActive) {
            a(classes = "item active_tab") { +translate(pageType.title) }
        } else {
            a(classes = "item inactive_tab") {
                +translate(pageType.title)
                if (onClick != null) onClickFunction = onClick
            }
        }
    }
}

fun FlowContent.subMenuItem(orderType: OrderType, onClick: (Event) -> Unit) {
    a(classes = "item sidebar_sub_text_color") {
        onClickFunction = {
            toggleSidebar()
            onClick.invoke(it)
        }
        +translate(orderType.description)
    }
}
