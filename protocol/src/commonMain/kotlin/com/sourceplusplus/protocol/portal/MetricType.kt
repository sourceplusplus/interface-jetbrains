package com.sourceplusplus.protocol.portal

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class MetricType {
    Throughput_Average,
    ResponseTime_Average,
    ServiceLevelAgreement_Average,
    ResponseTime_99Percentile,
    ResponseTime_95Percentile,
    ResponseTime_90Percentile,
    ResponseTime_75Percentile,
    ResponseTime_50Percentile;

    val responseTimePercentile: Boolean
        get() = this == ResponseTime_99Percentile
                || this == ResponseTime_95Percentile
                || this == ResponseTime_90Percentile
                || this == ResponseTime_75Percentile
                || this == ResponseTime_50Percentile

    companion object {
        //todo: remove
        fun realValueOf(name: String): MetricType {
            return (values().find { it.name == name }
                ?: when (name) {
                    "endpoint_cpm" -> Throughput_Average
                    "endpoint_avg" -> ResponseTime_Average
                    "endpoint_sla" -> ServiceLevelAgreement_Average
                    "endpoint_percentile" -> ResponseTime_99Percentile
                    else -> throw UnsupportedOperationException(name)
                })
        }
    }
}
