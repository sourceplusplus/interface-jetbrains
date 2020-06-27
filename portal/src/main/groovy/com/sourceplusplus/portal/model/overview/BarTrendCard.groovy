package com.sourceplusplus.portal.model.overview

import com.sourceplusplus.api.model.QueryTimeFrame
import groovy.transform.builder.Builder

/**
 * Represents an artifact's bar trend data.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.3.0
 * @since 0.1.0
 */
@Builder
class BarTrendCard {
    QueryTimeFrame timeFrame
    String header
    String meta
    double[] barGraphData
}
