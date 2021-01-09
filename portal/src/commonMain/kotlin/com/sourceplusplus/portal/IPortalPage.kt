package com.sourceplusplus.portal

import com.sourceplusplus.protocol.portal.PortalConfiguration

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface IPortalPage {
    val portalUuid: String

    fun setupEventbus()
    fun renderPage(portalConfiguration: PortalConfiguration)
}
