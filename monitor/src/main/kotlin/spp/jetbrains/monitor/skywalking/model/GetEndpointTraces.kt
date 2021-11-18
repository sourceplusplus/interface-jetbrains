package spp.jetbrains.monitor.skywalking.model

import spp.protocol.artifact.trace.TraceOrderType

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class GetEndpointTraces(
    val artifactQualifiedName: String,
    val serviceId: String? = null,
    val serviceInstanceId: String? = null,
    val endpointId: String? = null,
    val endpointName: String? = null,
    val zonedDuration: ZonedDuration,
    val orderType: TraceOrderType = TraceOrderType.LATEST_TRACES,
    val pageNumber: Int = 1,
    val pageSize: Int = 10
)
