package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.IPortalPage
import com.sourceplusplus.protocol.artifact.metrics.ArtifactMetricResult
import com.sourceplusplus.protocol.portal.PortalConfiguration

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class IActivityPage : IPortalPage() {

    override lateinit var configuration: PortalConfiguration
    var tooltipMeasurement = "ms"
    val labelColor by lazy { if (configuration.darkMode) "grey" else "black" }
    val symbolColor by lazy { if (configuration.darkMode) "grey" else "#182d34" }

    abstract fun displayActivity(metricResult: ArtifactMetricResult)
}
