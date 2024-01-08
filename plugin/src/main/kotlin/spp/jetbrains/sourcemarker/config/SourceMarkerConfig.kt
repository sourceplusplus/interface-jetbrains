/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.sourcemarker.config

/**
 * SourceMarker plugin configuration.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class SourceMarkerConfig(
    var autoResolveEndpointNames: Boolean = true,
    var localMentorEnabled: Boolean = true,
    var serviceHost: String? = null,
    var authorizationCode: String? = null,
    var certificatePins: List<String> = emptyList(),
    var accessToken: String? = null,
    var verifyHost: Boolean = true,
    val serviceName: String? = null,
    var override: Boolean = false,
    val portalConfig: PortalConfig = PortalConfig(),
    val commandConfig: Map<String, Map<String, Any>> = emptyMap(),
    var notifiedConnection: Boolean = false,
) {
    companion object {
        const val DEFAULT_SERVICE_PORT = 12800
    }
}

val SourceMarkerConfig.serviceHostNormalized: String
    get() {
        if (serviceHost == null) return "localhost"
        var serviceHost = serviceHost!!
            .substringAfter("https://").substringAfter("http://")
        if (serviceHost.contains(":")) {
            serviceHost = serviceHost.split(":")[0]
                .substringAfter("https://").substringAfter("http://")
        }
        return serviceHost
    }

fun SourceMarkerConfig.getServicePortNormalized(): Int {
    if (serviceHost == null) return SourceMarkerConfig.DEFAULT_SERVICE_PORT
    val hostStr = serviceHost!!.substringAfter("https://").substringAfter("http://")
    if (hostStr.contains(":")) {
        return hostStr.split(":")[1].toInt()
    }
    return SourceMarkerConfig.DEFAULT_SERVICE_PORT
}

fun SourceMarkerConfig.isSsl(): Boolean {
    return getServicePortNormalized() == 443 || serviceHost?.startsWith("https://") == true
}
