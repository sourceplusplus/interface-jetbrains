package com.sourceplusplus.protocol.artifact.metrics

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
data class SplineSeriesData(
    val seriesIndex: Int,
    val times: List<@Serializable(with = Serializers.InstantKSerializer::class) Instant>,
    val values: List<Double>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplineSeriesData) return false
        if (seriesIndex != other.seriesIndex) return false
        if (times != other.times) return false
        if (values != other.values) return false
        return true
    }

    override fun hashCode(): Int {
        var result = seriesIndex
        result = 31 * result + times.hashCode()
        result = 31 * result + values.hashCode()
        return result
    }
}
