package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.IPortalPage
import spp.protocol.portal.PortalConfiguration

/**
 * todo: description.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class ILogsPage : IPortalPage() {
    override lateinit var configuration: PortalConfiguration
}
