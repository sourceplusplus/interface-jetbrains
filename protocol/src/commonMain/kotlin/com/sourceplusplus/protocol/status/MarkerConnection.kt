package com.sourceplusplus.protocol.status

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class MarkerConnection(
    var pluginId: String? = null,
    var connectionTime: Long = 0,
    var hardwareId: String? = null
)
