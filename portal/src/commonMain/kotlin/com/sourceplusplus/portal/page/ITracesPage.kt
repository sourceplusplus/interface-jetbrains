package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.PortalPage
import com.sourceplusplus.portal.model.TraceDisplayType
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpan
import com.sourceplusplus.protocol.artifact.trace.TraceSpanInfo

/**
 * todo: description.
 *
 * @since 0.1.0
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
