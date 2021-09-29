package com.sourceplusplus.protocol.developer

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class Developer(
    val id: String,
    val accessToken: String? = null
)
