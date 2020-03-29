package com.sourceplusplus.api.model.metric;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.QueryTimeFrame;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.4
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = ArtifactMetricQuery.class)
@JsonDeserialize(as = ArtifactMetricQuery.class)
public interface AbstractArtifactMetricQuery extends SourceMessage {

    @Nullable
    String appUuid();

    @Nullable
    String artifactQualifiedName();

    @Nullable
    QueryTimeFrame timeFrame();

    List<MetricType> metricTypes();

    Instant start();

    Instant stop();

    String step();
}
