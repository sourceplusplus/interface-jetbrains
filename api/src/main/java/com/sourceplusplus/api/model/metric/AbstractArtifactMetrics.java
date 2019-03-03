package com.sourceplusplus.api.model.metric;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import java.util.List;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.1
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = ArtifactMetrics.class)
@JsonDeserialize(as = ArtifactMetrics.class)
public interface AbstractArtifactMetrics extends SourceMessage {

    MetricType metricType();

    List<Integer> values();
}
