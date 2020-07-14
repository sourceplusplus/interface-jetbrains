package com.sourceplusplus.api.model.metric;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.QueryTimeFrame;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

/**
 * MetricTypes by a given QueryTimeFrame.
 *
 * @version 0.3.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = TimeFramedMetricType.class)
@JsonDeserialize(as = TimeFramedMetricType.class)
public interface AbstractTimeFramedMetricType {

    MetricType metricType();

    QueryTimeFrame timeFrame();
}
