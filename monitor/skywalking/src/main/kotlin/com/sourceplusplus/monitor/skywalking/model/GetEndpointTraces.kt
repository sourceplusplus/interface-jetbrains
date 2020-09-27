package com.sourceplusplus.monitor.skywalking.model

import com.sourceplusplus.protocol.artifact.trace.TraceOrderType

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class GetEndpointTraces(
    val appUuid: String,
    val artifactQualifiedName: String,
    val serviceId: String? = null,
    val serviceInstanceId: String? = null,
    val endpointId: String? = null,
    val zonedDuration: ZonedDuration,
    val orderType: TraceOrderType = TraceOrderType.LATEST_TRACES,
    val pageNumber: Int = 1,
    val pageSize: Int = 10
)
