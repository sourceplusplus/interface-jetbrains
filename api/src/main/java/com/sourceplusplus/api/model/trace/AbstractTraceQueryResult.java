package com.sourceplusplus.api.model.trace;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import java.util.List;

/**
 * Result of a trace query.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = TraceQueryResult.class)
@JsonDeserialize(as = TraceQueryResult.class)
public interface AbstractTraceQueryResult {

    List<Trace> traces();

    int total();
}
