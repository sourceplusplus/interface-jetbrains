package com.sourceplusplus.protocol.instrument

enum class LiveInstrumentEventType {
    BREAKPOINT_ADDED,
    BREAKPOINT_HIT,
    BREAKPOINT_REMOVED,

    LOG_ADDED,
    LOG_REMOVED
}