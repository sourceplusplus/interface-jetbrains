package com.sourceplusplus.portal.template

import spp.protocol.artifact.OrderType
import spp.protocol.portal.PageType
import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.div
import kotlinx.html.style
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

class PortalNavigationConfiguration(private val flowContent: FlowContent) {

    internal enum class ModeType {
        MENU,
        TAB
    }

    val menuItems = ArrayList<() -> Unit>()
    val tabItems = ArrayList<() -> Unit>()
    private var mode = ModeType.MENU

    fun setMenuMode() {
        mode = ModeType.MENU
    }

    fun setTabMode() {
        mode = ModeType.TAB
    }

    fun navItem(
        pageType: PageType,
        isActive: Boolean = false,
        onClick: ((Event) -> Unit)? = null,
        block: (FlowContent.() -> Unit)? = null
    ) {
        menuItems.add {
            flowContent.menuItem(pageType, isActive, onClick, block)
        }
        tabItems.add {
            flowContent.tabItem(pageType, isActive, onClick, block)
        }
    }

    fun navSubItems(vararg subItems: PortalNavSubItem) = when (mode) {
        ModeType.MENU -> subItems.forEach { flowContent.subMenuItem(it.orderType, it.onClick) }
        ModeType.TAB -> flowContent.subTabItem(*subItems)
    }
}

data class PortalNavSubItem(val orderType: OrderType, val onClick: (Event) -> Unit)

fun TagConsumer<HTMLElement>.portalNav(block: PortalNavigationConfiguration.() -> Unit) {
    div("ui sidebar vertical left menu overlay visible very thin icon spp_blue webkit_transition") {
        style = "overflow: visible !important;"
        val portalNavigation = PortalNavigationConfiguration(this).apply(block)
        portalNavigation.setMenuMode()
        menu {
            portalNavigation.menuItems.forEach {
                it.invoke()
            }
        }
        portalNavigation.setTabMode()
        tabs {
            portalNavigation.tabItems.forEach {
                it.invoke()
            }
        }
    }
}
