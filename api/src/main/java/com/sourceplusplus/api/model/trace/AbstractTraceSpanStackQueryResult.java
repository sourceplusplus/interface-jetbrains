package com.sourceplusplus.api.model.trace;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import java.util.List;

/**
 * Result of a trace span stack query.
 *
 * @version 0.3.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = TraceSpanStackQueryResult.class)
@JsonDeserialize(as = TraceSpanStackQueryResult.class)
public interface AbstractTraceSpanStackQueryResult {

    List<TraceSpan> traceSpans();

    int total();
}
