package com.sourceplusplus.protocol.portal

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class BarTrendCard(
    val timeFrame: QueryTimeFrame? = null,
    val header: String,
    val meta: String,
    val barGraphData: DoubleArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BarTrendCard) return false

        if (timeFrame != other.timeFrame) return false
        if (header != other.header) return false
        if (meta != other.meta) return false
        if (!barGraphData.contentEquals(other.barGraphData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timeFrame.hashCode()
        result = 31 * result + header.hashCode()
        result = 31 * result + meta.hashCode()
        result = 31 * result + barGraphData.contentHashCode()
        return result
    }
}
