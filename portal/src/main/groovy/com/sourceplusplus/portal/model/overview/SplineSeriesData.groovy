package com.sourceplusplus.portal.model.overview

import groovy.transform.builder.Builder

import java.time.Instant

/**
 * Represents an artifact's spline series data.
 *
 * @version 0.3.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Builder
class SplineSeriesData {
    int seriesIndex
    List<Instant> times
    double[] values
}
