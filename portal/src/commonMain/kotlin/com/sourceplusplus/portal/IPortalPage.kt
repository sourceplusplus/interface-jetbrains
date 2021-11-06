package com.sourceplusplus.portal

import spp.protocol.portal.PortalConfiguration

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class IPortalPage {
    var setup = false
    abstract val portalUuid: String
    abstract var configuration: PortalConfiguration

    abstract fun setupEventbus()
    abstract fun renderPage(portalConfiguration: PortalConfiguration)
}
