package com.sourceplusplus.protocol.artifact.log

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
enum class LogViewType(val description: String) {
    LIVE_TAIL("Live Logs"),
    INDIVIDUAL_LOG("Log");
}
