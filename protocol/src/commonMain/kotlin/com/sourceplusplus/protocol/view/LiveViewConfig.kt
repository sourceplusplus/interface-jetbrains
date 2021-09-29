package com.sourceplusplus.protocol.view

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class LiveViewConfig(
    val viewName: String,
    val userDefined: Boolean,
    val viewMetrics: List<String>,
    val refreshRateLimit: Int = 1000 //limit of once per X milliseconds
)
