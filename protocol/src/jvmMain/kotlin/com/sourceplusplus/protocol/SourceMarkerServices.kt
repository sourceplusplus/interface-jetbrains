package com.sourceplusplus.protocol

import com.sourceplusplus.protocol.SourceMarkerServices.Namespace.LOGGING
import com.sourceplusplus.protocol.SourceMarkerServices.Namespace.TRACING
import com.sourceplusplus.protocol.SourceMarkerServices.Utilize.Tracing.HINDSIGHT_DEBUGGER
import com.sourceplusplus.protocol.service.logging.LogCountIndicatorService
import com.sourceplusplus.protocol.service.tracing.HindsightDebuggerService
import com.sourceplusplus.protocol.service.tracing.LocalTracingService

/**
 * todo: description.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkerServices {

    object Instance {
        object Tracing {
            var localTracing: LocalTracingService? = null
            var hindsightDebugger: HindsightDebuggerService? = null
        }

        object Logging {
            var logCountIndicator: LogCountIndicatorService? = null
        }
    }

    object Namespace {
        const val TRACING = "sm.tracing"
        const val LOGGING = "sm.logging"
        const val ALERTING = "sm.alerting"
    }

    object Utilize {
        object Tracing {
            const val LOCAL_TRACING = "$TRACING.local-tracing"
            const val HINDSIGHT_DEBUGGER = "$TRACING.hindsight-debugger"
        }

        object Logging {
            const val LOG_COUNT_INDICATOR = "$LOGGING.log-count-indicator"
        }
    }

    object Provide {
        object Tracing {
            const val HINDSIGHT_BREAKPOINT_SUBSCRIBER = "$HINDSIGHT_DEBUGGER.hindsight-breakpoint-subscriber"
        }
    }
}
