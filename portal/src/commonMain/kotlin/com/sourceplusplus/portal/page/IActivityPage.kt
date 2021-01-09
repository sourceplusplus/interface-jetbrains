package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.IPortalPage
import com.sourceplusplus.protocol.artifact.metrics.BarTrendCard
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import com.sourceplusplus.protocol.artifact.metrics.SplineChart

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface IActivityPage : IPortalPage {

    fun displayCard(card: BarTrendCard)
    fun updateChart(chartData: SplineChart)
    fun updateTime(interval: QueryTimeFrame)
}
