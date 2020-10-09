package com.sourceplusplus.protocol.artifact.endpoint

import com.sourceplusplus.protocol.portal.SplineChart

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class EndpointResult(
    val appUuid: String,
    val start: Long, //todo: Instant
    val stop: Long, //todo: Instant
    val charts: List<SplineChart>
)
