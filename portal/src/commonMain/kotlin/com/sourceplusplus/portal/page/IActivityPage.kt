package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.PortalPage
import com.sourceplusplus.protocol.portal.BarTrendCard
import com.sourceplusplus.protocol.portal.MetricType
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import com.sourceplusplus.protocol.portal.SplineChart

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface IActivityPage : PortalPage {
    var currentMetricType: MetricType
    var currentTimeFrame: QueryTimeFrame

    fun displayCard(card: BarTrendCard)
    fun updateChart(chartData: SplineChart)
    fun updateTime(interval: QueryTimeFrame)
}
