package com.sourceplusplus.protocol.artifact.metrics

import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class BarTrendCard(
    val timeFrame: QueryTimeFrame,
    val header: String,
    val meta: String,
    val barGraphData: List<Double>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BarTrendCard) return false
        if (timeFrame != other.timeFrame) return false
        if (header != other.header) return false
        if (meta != other.meta) return false
        if (barGraphData != other.barGraphData) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timeFrame.hashCode()
        result = 31 * result + header.hashCode()
        result = 31 * result + meta.hashCode()
        result = 31 * result + barGraphData.hashCode()
        return result
    }
}
