package com.sourceplusplus.protocol.artifact.trace

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class TraceStackHeaderType(val id: String, val icon: String) {
    TRACE_ID("trace_id", "crosshairs"),
    TIME_OCCURRED("time_occurred", "clock outline")
}
