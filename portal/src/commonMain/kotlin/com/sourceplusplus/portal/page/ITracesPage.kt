package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.PortalPage
import com.sourceplusplus.protocol.artifact.trace.*

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface ITracesPage : PortalPage {
    var traceOrderType: TraceOrderType
    var traceDisplayType: TraceDisplayType
    val hideActivityTab: Boolean

    fun displayTraces(traceResult: TraceResult)
    fun displayTraceStack(vararg traceStack: TraceSpanInfo)
    fun displaySpanInfo(spanInfo: TraceSpan)
}
