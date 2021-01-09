package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.IPortalPage
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpan
import com.sourceplusplus.protocol.artifact.trace.TraceStackPath
import com.sourceplusplus.protocol.portal.PortalConfiguration

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class ITracesPage : IPortalPage {
    override lateinit var configuration: PortalConfiguration

    abstract fun displayTraces(traceResult: TraceResult)
    abstract fun displayTraceStack(traceStackPath: TraceStackPath)
    abstract fun displaySpanInfo(spanInfo: TraceSpan)
}
