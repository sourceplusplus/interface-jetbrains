package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.PortalPage
import com.sourceplusplus.portal.model.TraceDisplayType
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpan
import com.sourceplusplus.protocol.artifact.trace.TraceStackPath

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface ITracesPage : PortalPage {
    var traceOrderType: TraceOrderType
    var traceDisplayType: TraceDisplayType

    fun displayTraces(traceResult: TraceResult)
    fun displayTraceStack(traceStackPath: TraceStackPath)
    fun displaySpanInfo(spanInfo: TraceSpan)
}
