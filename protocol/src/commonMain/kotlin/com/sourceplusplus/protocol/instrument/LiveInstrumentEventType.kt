package com.sourceplusplus.protocol.instrument

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class LiveInstrumentEventType {
    BREAKPOINT_ADDED,
    BREAKPOINT_APPLIED,
    BREAKPOINT_HIT,
    BREAKPOINT_REMOVED,

    LOG_ADDED,
    LOG_APPLIED,
    LOG_HIT,
    LOG_REMOVED
}