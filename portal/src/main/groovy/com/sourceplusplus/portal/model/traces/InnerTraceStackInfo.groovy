package com.sourceplusplus.portal.model.traces

import groovy.transform.builder.Builder
import io.vertx.core.json.JsonArray

/**
 * Represents an artifact's trace stack data.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.0
 */
@Builder
class InnerTraceStackInfo {
    int innerLevel
    JsonArray traceStack
}
