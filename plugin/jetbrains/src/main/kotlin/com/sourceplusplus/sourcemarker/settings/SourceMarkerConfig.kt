package com.sourceplusplus.sourcemarker.settings

/**
 * SourceMarker plugin configuration.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class SourceMarkerConfig(
    val skywalkingOapUrl: String = "http://localhost:12800/graphql",
    var rootSourcePackage: String? = null
)