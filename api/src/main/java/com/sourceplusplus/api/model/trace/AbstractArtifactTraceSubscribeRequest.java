package com.sourceplusplus.api.model.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.sourceplusplus.api.model.QueryTimeFrame;
import com.sourceplusplus.api.model.SourceStyle;
import com.sourceplusplus.api.model.artifact.ArtifactSubscribeRequest;
import com.sourceplusplus.api.model.artifact.SourceArtifactSubscriptionType;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Used to subscribe to artifact traces.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonSerialize(as = ArtifactTraceSubscribeRequest.class)
@JsonDeserialize(as = ArtifactTraceSubscribeRequest.class)
public interface AbstractArtifactTraceSubscribeRequest extends ArtifactSubscribeRequest {

    @Nullable //todo: remove
    QueryTimeFrame timeFrame();

    List<TraceOrderType> orderTypes();

    @Override
    @Value.Default
    default SourceArtifactSubscriptionType getType() {
        return SourceArtifactSubscriptionType.TRACES;
    }

    @Value.Check
    default void validate() {
        Preconditions.checkState(!orderTypes().isEmpty(),
                "Subscription must include at least one trace order type");
    }
}
