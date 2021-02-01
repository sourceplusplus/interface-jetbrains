package com.sourceplusplus.protocol.portal

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class PortalConfiguration(
    var currentPage: PageType = PageType.ACTIVITY,
    var darkMode: Boolean = false,
    var external: Boolean = false,
    var visibleOverview: Boolean = true,
    var visibleActivity: Boolean = true,
    var visibleTraces: Boolean = true,
    var visibleLogs: Boolean = true,
    var visibleConfiguration: Boolean = false
)
