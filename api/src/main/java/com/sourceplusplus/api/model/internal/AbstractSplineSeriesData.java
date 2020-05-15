package com.sourceplusplus.api.model.internal;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.List;

/**
 * Represents an artifact's spline series data.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.5
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = SplineSeriesData.class)
@JsonDeserialize(as = SplineSeriesData.class)
public interface AbstractSplineSeriesData extends SourceMessage {

    int seriesIndex();

    List<Instant> times();

    double[] values();
}
