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
    val entityIds: List<String>,
    val artifactQualifiedName: String,
    val liveViewConfig: LiveViewConfig
)
