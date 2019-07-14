package com.sourceplusplus.api.model.internal;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.QueryTimeFrame;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import com.sourceplusplus.api.model.metric.MetricType;
import org.immutables.value.Value;

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
@JsonSerialize(as = SplineChart.class)
@JsonDeserialize(as = SplineChart.class)
public interface AbstractSplineChart extends SourceMessage {

    MetricType metricType();

    QueryTimeFrame timeFrame();

    List<SplineSeriesData> seriesData();
}
