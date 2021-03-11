package com.sourceplusplus.sourcemarker.settings

/**
 * SourceMarker plugin configuration.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class SourceMarkerConfig(
    val skywalkingOapUrl: String = "http://localhost:12800/graphql",
    var rootSourcePackage: String? = null,
    var autoResolveEndpointNames: Boolean = false,
    var localMentorEnabled: Boolean = true,
    var pluginConsoleEnabled: Boolean = false,
    var portalRefreshIntervalMs: Int = 5000
)
