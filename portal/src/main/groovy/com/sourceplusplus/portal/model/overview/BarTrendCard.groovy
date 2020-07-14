package com.sourceplusplus.portal.model.overview

import com.sourceplusplus.api.model.QueryTimeFrame
import groovy.transform.builder.Builder

/**
 * Represents an artifact's bar trend data.
 *
 * @version 0.3.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Builder
class BarTrendCard {
    QueryTimeFrame timeFrame
    String header
    String meta
    double[] barGraphData
}
