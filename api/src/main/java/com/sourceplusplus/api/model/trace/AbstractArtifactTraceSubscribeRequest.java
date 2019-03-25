package com.sourceplusplus.api.model.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.QueryTimeFrame;
import com.sourceplusplus.api.model.SourceStyle;
import com.sourceplusplus.api.model.artifact.ArtifactSubscribeRequest;
import com.sourceplusplus.api.model.artifact.SourceArtifactSubscriptionType;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.3
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonSerialize(as = ArtifactTraceSubscribeRequest.class)
@JsonDeserialize(as = ArtifactTraceSubscribeRequest.class)
public interface AbstractArtifactTraceSubscribeRequest extends ArtifactSubscribeRequest {

    @Nullable
    QueryTimeFrame timeFrame();

    TraceOrderType orderType();

    @Override
    @Value.Default
    default SourceArtifactSubscriptionType getType() {
        return SourceArtifactSubscriptionType.TRACES;
    }
}
