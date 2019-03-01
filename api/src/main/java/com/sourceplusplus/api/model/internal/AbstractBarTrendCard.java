package com.sourceplusplus.api.model.internal;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import com.sourceplusplus.api.model.QueryTimeFrame;
import org.immutables.value.Value;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.0
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = BarTrendCard.class)
@JsonDeserialize(as = BarTrendCard.class)
public interface AbstractBarTrendCard extends SourceMessage {

    QueryTimeFrame timeFrame();

    String header();

    String meta();

    double[] barGraphData();
}
