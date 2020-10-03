package com.sourceplusplus.portal

import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.page.ConfigurationPage
import com.sourceplusplus.portal.page.OverviewPage
import com.sourceplusplus.portal.page.RealOverviewPage
import com.sourceplusplus.portal.page.TracesPage
import kotlinx.browser.window

fun main() {
    jq().ready {
        when (window.location.pathname) {
            "/overview" -> OverviewPage().renderPage()
            "/traces" -> TracesPage().renderPage()
            "/configuration" -> ConfigurationPage().renderPage()
            else -> RealOverviewPage().renderPage()
        }

        js("loadTheme();")
    }

//    jq().ready {
//        var inc = 0
//        window.setInterval({
//            inc++
//
//            jq("#card_throughput_average_header").text("Updated from Kotlin (JQuery) - $inc")
//
//            document.getElementById("card_responsetime_average_header")
//                ?.textContent = "Updated from Kotlin (DOM) - $inc"
//
//            val slaCard = document.getElementById("card_servicelevelagreement_average_header")!!
//            slaCard.textContent = "Updated from Kotlin (DOM) - $inc"
//        }, 1000)
//    }
}