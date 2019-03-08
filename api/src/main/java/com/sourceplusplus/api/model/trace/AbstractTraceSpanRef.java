package com.sourceplusplus.api.model.trace;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.1
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = TraceSpanRef.class)
@JsonDeserialize(as = TraceSpanRef.class)
public interface AbstractTraceSpanRef {

    @JsonAlias({"traceId", "trace_id"})
    String traceId();

    @JsonAlias({"parentSegmentId", "parent_segment_id"})
    String parentSegmentId();

    @JsonAlias({"parentSpanId", "parent_span_id"})
    long parentSpanId();

    String type();
}
