package com.sourceplusplus.api.model.metric;

/**
 * Different metric order types core can be queried by.
 *
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
public enum MetricType {

    Throughput_Average,
    ResponseTime_Average,
    ServiceLevelAgreement_Average,
    ResponseTime_99Percentile,
    ResponseTime_95Percentile,
    ResponseTime_90Percentile,
    ResponseTime_75Percentile,
    ResponseTime_50Percentile
}
