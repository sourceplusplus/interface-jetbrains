package com.sourceplusplus.api.model.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.sourceplusplus.api.client.SourceClient;
import com.sourceplusplus.api.model.QueryTimeFrame;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.0
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonSerialize(as = ArtifactTraceUnsubscribeRequest.class)
@JsonDeserialize(as = ArtifactTraceUnsubscribeRequest.class)
public interface AbstractArtifactTraceUnsubscribeRequest extends SourceMessage {

    @Nullable
    String appUuid();

    @Nullable
    String artifactQualifiedName();

    @Value.Default
    default String getSubscriberClientId() {
        return SourceClient.CLIENT_ID;
    }

    List<QueryTimeFrame> removeTimeFrames();

    List<TraceOrderType> removeOrderTypes();

    @Value.Default
    default boolean removeAllArtifactTraceSubscriptions() {
        return false;
    }

    @Value.Check
    default void validate() {
        Preconditions.checkState(!(!removeAllArtifactTraceSubscriptions() &&
                        removeTimeFrames().isEmpty() && removeOrderTypes().isEmpty()),
                "Must either remove all traces subscriptions, remove by time frame, or remove by order type");
    }
}
