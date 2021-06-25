package com.sourceplusplus.protocol.view

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class LiveViewEvent(
    val entityId: String,
    val artifactQualifiedName: String,
    val timeBucket: String,
    val metricName: String,
    val metricsData: String, //todo: type out
    val type: ViewSubscriptionType
)
