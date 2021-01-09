package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.IPortalPage
import com.sourceplusplus.protocol.artifact.endpoint.EndpointResult
import com.sourceplusplus.protocol.artifact.QueryTimeFrame

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface IOverviewPage : IPortalPage {

    fun displayEndpoints(endpointResult: EndpointResult)
    fun updateTime(interval: QueryTimeFrame)
}
