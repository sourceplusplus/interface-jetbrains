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
data class TraceSpan(
    val traceId: String,
    val segmentId: String,
    val spanId: Int,
    val parentSpanId: Int,
    val refs: List<TraceSpanRef> = emptyList(),
    val serviceCode: String,
    val serviceInstanceName: String? = null,
    @Serializable(with = Serializers.InstantKSerializer::class)
    val startTime: Instant,
    @Serializable(with = Serializers.InstantKSerializer::class)
    val endTime: Instant,
    val endpointName: String? = null,
    val artifactQualifiedName: String? = null,
    val type: String,
    val peer: String? = null,
    val component: String? = null,
    val error: Boolean? = null,
    val childError: Boolean = false,
    val hasChildStack: Boolean? = null,
    val layer: String? = null,
    val tags: Map<String, String> = emptyMap(),
    val logs: List<TraceSpanLogEntry> = emptyList(),
    val meta: MutableMap<String, String> = mutableMapOf()
) {
    fun putMetaInt(tag: String, value: Int) {
        meta[tag] = value.toString()
    }

    fun putMetaLong(tag: String, value: Long) {
        meta[tag] = value.toString()
    }

    fun putMetaDouble(tag: String, value: Double) {
        meta[tag] = value.toString()
    }

    fun putMetaString(tag: String, value: String) {
        meta[tag] = value
    }

    fun putMetaInt(tag: String): Int? {
        return meta[tag]?.toIntOrNull()
    }

    fun getMetaLong(tag: String): Long {
        return meta[tag]!!.toLong()
    }

    fun getMetaDouble(tag: String): Double {
        return meta[tag]!!.toDouble()
    }

    fun getMetaString(tag: String): String {
        return meta[tag]!!
    }
}
