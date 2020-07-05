package com.sourceplusplus.portal.model.traces

import groovy.transform.builder.Builder
import io.vertx.core.json.JsonArray

/**
 * Represents an artifact's trace stack data.
 *
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Builder
class InnerTraceStackInfo {
    int innerLevel
    JsonArray traceStack
}
