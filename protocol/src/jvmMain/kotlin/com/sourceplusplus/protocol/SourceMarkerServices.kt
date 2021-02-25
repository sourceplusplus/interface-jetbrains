package com.sourceplusplus.protocol

object SourceMarkerServices {
    const val TRACING_SERVICES = "sm.tracing.service"
    const val LOGGING_SERVICES = "sm.logging.service"

    object Provider {
        const val LOG_COUNT_INDICATOR = "$LOGGING_SERVICES.log-count-indicator"
        const val LOCAL_TRACING = "$TRACING_SERVICES.local-tracing"
    }
}
