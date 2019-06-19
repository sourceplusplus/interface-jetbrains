package com.sourceplusplus.api.model.trace;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.1
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = TraceSpanStackQuery.class)
@JsonDeserialize(as = TraceSpanStackQuery.class)
public interface AbstractTraceSpanStackQuery {

    boolean oneLevelDeep();

    String traceId();

    @Nullable
    String segmentId();

    @Nullable
    Long spanId();

    @Value.Default
    default boolean followExit() {
        return false;
    }

    //todo: spanId and segmentId can't be null at same time
}
