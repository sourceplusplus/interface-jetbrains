package com.sourceplusplus.portal.model.traces

import com.sourceplusplus.api.model.trace.TraceSpan
import groovy.transform.builder.Builder

/**
 * Represents an artifact's trace data.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.3.0
 * @since 0.1.0
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
