package com.sourceplusplus.protocol.view

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class LiveViewSubscription(
    val subscriptionId: String? = null,
    val entityId: String? = null,
    val artifactQualifiedName: String,
    val metricNames: List<String>,
    val type: ViewSubscriptionType,
    val refreshRateLimit: Int = 1000 //limit of once per X milliseconds
)
