package com.sourceplusplus.portal.model

import com.sourceplusplus.protocol.artifact.trace.Trace

/**
 * Represents the layers of a [Trace].
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class TraceDisplayType {
    TRACES,
    TRACE_STACK,
    SPAN_INFO
}
