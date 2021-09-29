package com.sourceplusplus.protocol.view

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class LiveViewEvent(
    val entityId: String,
    val artifactQualifiedName: String,
    val timeBucket: String,
    val viewConfig: LiveViewConfig,
    val metricsData: String, //todo: type out
)
