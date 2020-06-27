package com.sourceplusplus.api.model.metric;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.sourceplusplus.api.model.QueryTimeFrame;
import com.sourceplusplus.api.model.SourceStyle;
import com.sourceplusplus.api.model.artifact.ArtifactUnsubscribeRequest;
import com.sourceplusplus.api.model.artifact.SourceArtifactSubscriptionType;
import org.immutables.value.Value;

import java.util.Set;

/**
 * Used to unsubscribe to artifact metrics.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.3.0
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonSerialize(as = ArtifactMetricUnsubscribeRequest.class)
@JsonDeserialize(as = ArtifactMetricUnsubscribeRequest.class)
public interface AbstractArtifactMetricUnsubscribeRequest extends ArtifactUnsubscribeRequest {

    Set<TimeFramedMetricType> removeTimeFramedMetricTypes();

    Set<QueryTimeFrame> removeTimeFrames();

    Set<MetricType> removeMetricTypes();

    @Value.Default
    default boolean removeAllArtifactMetricSubscriptions() {
        return false;
    }

    @Override
    @Value.Default
    default SourceArtifactSubscriptionType getType() {
        return SourceArtifactSubscriptionType.METRICS;
    }

    @Value.Check
    default void validate() {
        Preconditions.checkState(!(!removeAllArtifactMetricSubscriptions() &&
                        removeTimeFrames().isEmpty() && removeMetricTypes().isEmpty() &&
                        removeTimeFramedMetricTypes().isEmpty()),
                "Unsubscription will have no effect if processed");
    }
}
