package com.sourceplusplus.api.model.internal;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import io.vertx.core.json.JsonArray;
import org.immutables.value.Value;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.3
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = InnerTraceStackInfo.class)
@JsonDeserialize(as = InnerTraceStackInfo.class)
public interface AbstractInnerTraceStackInfo extends SourceMessage {

    int innerLevel();

    JsonArray traceStack();
}
