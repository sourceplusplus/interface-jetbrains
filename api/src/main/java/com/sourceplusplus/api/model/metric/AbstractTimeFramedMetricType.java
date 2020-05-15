package com.sourceplusplus.api.model.metric;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.QueryTimeFrame;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

/**
 * MetricTypes by a given QueryTimeFrame.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = TimeFramedMetricType.class)
@JsonDeserialize(as = TimeFramedMetricType.class)
public interface AbstractTimeFramedMetricType {

    MetricType metricType();

    QueryTimeFrame timeFrame();
}
