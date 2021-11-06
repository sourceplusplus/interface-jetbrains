package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.IPortalPage
import spp.protocol.artifact.QueryTimeFrame
import spp.protocol.artifact.endpoint.EndpointResult
import spp.protocol.portal.PortalConfiguration

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class IOverviewPage : IPortalPage() {
    override lateinit var configuration: PortalConfiguration

    abstract fun displayEndpoints(endpointResult: EndpointResult)
    abstract fun updateTime(interval: QueryTimeFrame)
}
