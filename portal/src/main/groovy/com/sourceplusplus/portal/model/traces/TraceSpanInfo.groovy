package com.sourceplusplus.portal.model.traces

import com.sourceplusplus.api.model.trace.TraceSpan
import groovy.transform.builder.Builder

/**
 * Represents an artifact's trace data.
 *
 * @version 0.3.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Builder
class TraceSpanInfo {
    TraceSpan span
    String timeTook
    String appUuid
    String rootArtifactQualifiedName
    String operationName
    double totalTracePercent
}
