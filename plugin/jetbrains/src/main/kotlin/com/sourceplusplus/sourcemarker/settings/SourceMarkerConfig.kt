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
    var portalRefreshIntervalMs: Int = 5000,
    var serviceHost: String? = null,
    var serviceCertificate: String? = null,
    var serviceToken: String? = null
) {
    val serviceHostNormalized: String?
        get() {
            if (serviceHost == null) return null
            var serviceHost = serviceHost!!
                .substringAfter("https://").substringAfter("http://")
            if (serviceHost.contains(":")) {
                serviceHost = serviceHost.split(":")[0]
                    .substringAfter("https://").substringAfter("http://")
            }
            return serviceHost
        }

    fun getServicePortNormalized(defaultServicePort: Int?): Int? {
        if (serviceHost == null) return null
        if (serviceHost!!.contains(":")) {
            return serviceHost!!.split(":")[1].toInt()
        }
        return defaultServicePort
    }
}
