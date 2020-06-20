package com.sourceplusplus.api.model.metric;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.sourceplusplus.api.model.QueryTimeFrame;
import com.sourceplusplus.api.model.SourceStyle;
import com.sourceplusplus.api.model.artifact.ArtifactSubscribeRequest;
import com.sourceplusplus.api.model.artifact.SourceArtifactSubscriptionType;
import org.immutables.value.Value;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Used to subscribe to artifact metrics.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonSerialize(as = ArtifactMetricSubscribeRequest.class)
@JsonDeserialize(as = ArtifactMetricSubscribeRequest.class)
public interface AbstractArtifactMetricSubscribeRequest extends ArtifactSubscribeRequest {

    QueryTimeFrame timeFrame();

    Set<MetricType> metricTypes();

    @Override
    @Value.Default
    default SourceArtifactSubscriptionType getType() {
        return SourceArtifactSubscriptionType.METRICS;
    }

    @Value.Check
    default void validate() {
        Preconditions.checkState(!metricTypes().isEmpty(),
                "Subscription must include at least one metric type");
    }

    @Value.Lazy
    default Set<TimeFramedMetricType> asTimeFramedMetricTypes() {
        return metricTypes().stream()
                .map(it -> TimeFramedMetricType.builder().metricType(it).timeFrame(timeFrame()).build())
                .collect(Collectors.toSet());
    }
}
