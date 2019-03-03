package com.sourceplusplus.api.model.trace;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.1
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = TraceQuery.class)
@JsonDeserialize(as = TraceQuery.class)
public interface AbstractTraceQuery {

    @Nullable
    String appUuid();

    @Nullable
    String artifactQualifiedName();

    @Nullable
    String serviceId();

    @Nullable
    String traceId();

    @Nullable
    String endpointId();

    @Nullable
    String endpointName();

    @Nullable
    Integer minTraceDuration();

    @Nullable
    Integer maxTraceDuration();

    @Nullable
    String traceState();

    @Nullable
    String queryOrder();

    @Nullable
    String pagination();

    @Nullable
    Instant durationStart();

    @Nullable
    Instant durationStop();

    @Nullable
    String durationStep();
}
