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
 * @version 0.2.2
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = ArtifactMetricResult.class)
@JsonDeserialize(as = ArtifactMetricResult.class)
public interface AbstractArtifactMetricResult extends SourceMessage {

    @Nullable
    String appUuid();

    @Nullable
    String artifactQualifiedName();

    QueryTimeFrame timeFrame();

    Instant start();

    Instant stop();

    String step();

    List<ArtifactMetrics> artifactMetrics();
}
