package spp.jetbrains.sourcemarker.settings

/**
 * SourceMarker plugin configuration.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class SourceMarkerConfig(
    val skywalkingOapUrl: String = "http://localhost:12800/graphql",
    var rootSourcePackages: List<String> = emptyList(),
    var autoResolveEndpointNames: Boolean = false,
    var localMentorEnabled: Boolean = true,
    var pluginConsoleEnabled: Boolean = false,
    var portalRefreshIntervalMs: Int = 5000,
    var serviceHost: String? = null,
    var accessToken: String? = null,
    var certificatePins: List<String> = emptyList(),
    var serviceToken: String? = null
)

val SourceMarkerConfig.serviceHostNormalized: String?
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

fun SourceMarkerConfig.getServicePortNormalized(defaultServicePort: Int?): Int? {
    if (serviceHost == null) return null
    val hostStr = serviceHost!!.substringAfter("https://").substringAfter("http://")
    if (hostStr.contains(":")) {
        return hostStr.split(":")[1].toInt()
    }
    return defaultServicePort
}

fun SourceMarkerConfig.isSsl(): Boolean {
    return getServicePortNormalized(null) == 443 || serviceHost?.startsWith("https://") == true
}
