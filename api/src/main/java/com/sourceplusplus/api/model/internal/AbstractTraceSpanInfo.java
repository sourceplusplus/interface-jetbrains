package com.sourceplusplus.api.model.internal;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import com.sourceplusplus.api.model.trace.TraceSpan;
import org.immutables.value.Value;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.3
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = TraceSpanInfo.class)
@JsonDeserialize(as = TraceSpanInfo.class)
public interface AbstractTraceSpanInfo extends SourceMessage {

    TraceSpan span();

    String timeTook();

    String appUuid();

    String rootArtifactQualifiedName();

    String operationName();

    double totalTracePercent();
}
