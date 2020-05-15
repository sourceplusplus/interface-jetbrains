package com.sourceplusplus.api.model.metric;

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
 * Used to unsubscribe to artifact metrics.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.5
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonSerialize(as = ArtifactMetricUnsubscribeRequest.class)
@JsonDeserialize(as = ArtifactMetricUnsubscribeRequest.class)
public interface AbstractArtifactMetricUnsubscribeRequest extends SourceMessage {

    @Nullable
    String appUuid();

    @Nullable
    String artifactQualifiedName();

    @Value.Default
    default String getSubscriberClientId() {
        return SourceClient.CLIENT_ID;
    }

    List<TimeFramedMetricType> removeTimeFramedMetricTypes();

    List<QueryTimeFrame> removeTimeFrames();

    List<MetricType> removeMetricTypes();

    @Value.Default
    default boolean removeAllArtifactMetricSubscriptions() {
        return false;
    }

    @Value.Check
    default void validate() {
        Preconditions.checkState(!(!removeAllArtifactMetricSubscriptions() &&
                        removeTimeFrames().isEmpty() && removeMetricTypes().isEmpty()),
                "Must either remove all metrics subscriptions, remove by time frame, or remove by metric type");
    }
}
