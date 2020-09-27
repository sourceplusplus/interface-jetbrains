package com.sourceplusplus.portal.template

import com.sourceplusplus.protocol.artifact.trace.TraceStackHeaderType
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import kotlinx.html.*
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLElement

fun TagConsumer<HTMLElement>.navBar(attached: Boolean = true, block: FlowContent.() -> Unit) {
    div("ui menu top ${if (attached) "attached" else ""} background_color") {
        block()
    }
}

fun TagConsumer<HTMLElement>.timeDropdown(
    vararg timeFrames: QueryTimeFrame = arrayOf(),
    updateTimeFrame: (QueryTimeFrame) -> Unit
) {
    div("first_menu_button_margin align_content_center") {
        div("ui icon basic button top left pointing dropdown") {
            i("clock outline icon spp_red_color")
            div("menu secondary_background_color no_top_margin") {
                for (timeFrame in timeFrames) {
                    div("item") {
                        id = "last_${timeFrame.id}_time"
                        onClickFunction = { updateTimeFrame(timeFrame) }
                        span("menu_tooltip_text") { +"LAST ${timeFrame.description}" }
                    }
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.calendar() {
    div("ui calendar align_content_center") {
        id = "button_calendar"
        button(classes = "ui icon basic button spp_blue_color") {
            i("icon calendar")
        }
    }
}

fun TagConsumer<HTMLElement>.tracesHeader(vararg traceStackHeaderTypes: TraceStackHeaderType = arrayOf()) {
    a(classes = "marginlefting ui item dropdown active_sub_tab") {
        id = "latest_traces_header"
        onClick = "clickedBackToTraces()"
        span {
            id = "latest_traces_header_text"
            +"Latest Traces"
        }
    }
    a(classes = "ui item dropdown visibility_hidden") {
        id = "trace_stack_header"
        onClick = "clickedBackToTraceStack()"
        span {
            id = "trace_stack_header_text"
            +"Trace Stack"
        }
        div("menu") {
            id = "trace_stack_menu"
            for (traceStackHeaderType in traceStackHeaderTypes) {
                div("ui input item") {
                    i("icon no_padding_top ${traceStackHeaderType.icon}")
                    input {
                        classes = setOf("input_width")
                        id = "${traceStackHeaderType.id}_field"
                        type = InputType.text
                        readonly = true
                    }
                }
            }
        }
    }
    a(classes = "ui item dropdown visibility_hidden") {
        id = "span_info_header"
        span {
            id = "span_info_header_text"
            +"Span Info"
        }
    }
}

fun TagConsumer<HTMLElement>.externalPortalButton() {
    div("first_menu_button_margin align_content_center") {
        div("ui icon basic button") {
            onClick = "clickedViewAsExternalPortal()"
            i("icon external link spp_red_color")
        }
    }
}

fun TagConsumer<HTMLElement>.rightAlign(block: FlowContent.() -> Unit) {
    div("right menu align_content_center") {
        block()
    }
}
