package com.sourceplusplus.portal.template

import com.sourceplusplus.portal.PortalBundle.translate
import com.sourceplusplus.portal.model.TableType
import com.sourceplusplus.portal.model.TraceSpanInfoType
import kotlinx.html.*
import org.w3c.dom.HTMLElement

fun TagConsumer<HTMLElement>.portalContent(block: FlowContent.() -> Unit) {
    div("pusher background_color") {
        block()
    }
}

fun TagConsumer<HTMLElement>.wideColumn(block: FlowContent.() -> Unit) {
    div("wide column marginlefting") {
        block()
    }
}

fun TagConsumer<HTMLElement>.table(
    tableClasses: String = "", //todo: generify and remove
    tableId: String, tableBodyId: String,
    tableBackground: String = "",
    tableBodyBackground: String = "",
    vararg tableTypes: TableType = arrayOf()
) {
    this@table.table("ui sortable celled striped table unstackable $tableClasses") {
        id = tableId
        thead(tableBackground) {
            tr {
                for (tableType in tableTypes) {
                    th(classes = "secondary_background_color ${if (tableType.isCentered) "trace_th_center" else "trace_th"}") {
                        + translate(tableType.description)
                    }
                }
            }
        }
        tbody(tableBodyBackground) {
            id = tableBodyId
        }
    }
}

fun TagConsumer<HTMLElement>.spanInfoPanel(vararg traceSpanInfoTypes: TraceSpanInfoType = arrayOf()) {
    div("visibility_hidden") {
        id = "span_info_panel"
        div("ui segments") {
            div("ui segment span_segment_background") {
                for (traceSpanInfoType in traceSpanInfoTypes) {
                    p {
                        + "${translate(traceSpanInfoType.description)}:"
                        span {
                            id = "span_info_${traceSpanInfoType.id1}"
                        }
                        +"("
                        span("trace_time") {
                            id = "span_info_${traceSpanInfoType.id2}"
                        }
                        +")"
                    }
                }
            }
            div("ui segment displaynone no_padding") {
                id = "span_tag_div"
                table("ui celled striped table unstackable") {
                    id = "span_tag_table"
                    thead {
                        tr {
                            th { + translate("Tag") }
                            th { + translate("Value") }
                        }
                    }
                    tbody {
                        id = "tag_table"
                    }
                }
            }
            div("ui segment displaynone no_padding_auto") {
                id = "span_log_div"
                table("ui celled striped table unstackable") {
                    id = "log_tag_table"
                    thead {
                        tr {
                            th { + translate("Trace Logs") }
                        }
                    }
                    tbody {
                        id = "log_table"
                    }
                }
            }
        }
    }
}
