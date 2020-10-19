package com.sourceplusplus.portal.model

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class TraceStackHeaderType(val id: String, val icon: String) {
    TRACE_ID("trace_id", "crosshairs"),
    TIME_OCCURRED("time_occurred", "clock outline")
}
