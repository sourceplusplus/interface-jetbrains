package com.sourceplusplus.protocol.portal

import com.sourceplusplus.protocol.artifact.ArtifactType
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
    var darkMode: Boolean = false, //todo: can be page or status bar
    var external: Boolean = false,
    var visibleOverview: Boolean = true,
    var visibleActivity: Boolean = true,
    var visibleTraces: Boolean = true,
    var visibleLogs: Boolean = true,
    var visibleConfiguration: Boolean = false,
    var autoResolveEndpointNames: Boolean = false,
    var artifactType: ArtifactType? = null, //todo: allow multiple types? (endpoint + method)
    var statusBar: Boolean = false
) {
    fun isViewable(pageType: PageType): Boolean {
        return when (pageType) {
            PageType.OVERVIEW -> visibleOverview
            PageType.ACTIVITY -> visibleActivity
            PageType.TRACES -> visibleTraces
            PageType.LOGS -> visibleLogs
            PageType.CONFIGURATION -> visibleConfiguration
        }
    }
}
