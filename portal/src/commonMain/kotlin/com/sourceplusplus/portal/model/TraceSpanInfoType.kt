package com.sourceplusplus.portal.model

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class TraceSpanInfoType(val id1: String, val id2: String, val description: String) {
    START_TIME("start_time", "start_trace_time", "Start time"),
    END_TIME("end_time", "end_trace_time", "End time")
}
