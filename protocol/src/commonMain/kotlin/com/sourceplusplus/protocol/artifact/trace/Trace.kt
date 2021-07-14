package com.sourceplusplus.protocol.artifact.trace

import com.sourceplusplus.protocol.Serializers
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.1.0
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
    val partial: Boolean = false,
    val segmentId: String? = null,
    val meta: Map<String, String> = mutableMapOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Trace) return false
        if (traceIds != other.traceIds) return false
        return true
    }

    override fun hashCode(): Int {
        return traceIds.hashCode()
    }
}
