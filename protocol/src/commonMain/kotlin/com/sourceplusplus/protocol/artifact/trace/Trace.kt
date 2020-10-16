package com.sourceplusplus.protocol.artifact.trace

import com.sourceplusplus.protocol.Serializers
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class Trace(
    val key: String? = null,
    val operationNames: List<String>,
    val duration: Int,
    @Serializable(with = Serializers.InstantKSerializer::class)
    val start: Instant,
    val error: Boolean? = null,
    val traceIds: List<String>,
    val prettyDuration: String? = null,
    val partial: Boolean = false,
    val segmentId: String? = null
)
