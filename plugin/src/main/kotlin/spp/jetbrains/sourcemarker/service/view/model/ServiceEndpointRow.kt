package spp.jetbrains.sourcemarker.service.view.model

import spp.protocol.platform.general.ServiceEndpoint

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class ServiceEndpointRow(
    val endpoint: ServiceEndpoint,
    var cpm: Int = 0,
    var sla: Double = 0.0,
    var respTimeAvg: Int = 0
)
