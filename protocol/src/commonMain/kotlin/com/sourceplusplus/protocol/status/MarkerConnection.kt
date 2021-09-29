package com.sourceplusplus.protocol.status

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class MarkerConnection(
    var markerId: String,
    var connectionTime: Long,
    var hardwareId: String? = null
)
