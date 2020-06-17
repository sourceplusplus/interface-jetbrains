package com.sourceplusplus.portal.model.overview

import groovy.transform.builder.Builder

import java.time.Instant

/**
 * Represents an artifact's spline series data.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.0
 */
@Builder
class SplineSeriesData {
    int seriesIndex
    List<Instant> times
    double[] values
}
