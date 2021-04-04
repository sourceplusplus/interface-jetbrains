package com.sourceplusplus.protocol

import com.sourceplusplus.protocol.service.logging.LogCountIndicatorService
import com.sourceplusplus.protocol.service.tracing.LocalTracingService
import com.sourceplusplus.protocol.service.tracing.ProductionDebuggerService

/**
 * todo: description.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkerServices {

    const val TRACING = "sm.tracing.service"
    const val LOGGING = "sm.logging.service"

    object Instance {
        object Tracing {
            var localTracing: LocalTracingService? = null
            var productionDebugger: ProductionDebuggerService? = null
        }

        object Logging {
            var logCountIndicator: LogCountIndicatorService? = null
        }
    }

    object Provider {
        object Tracing {
            const val LOCAL_TRACING = "$TRACING.local-tracing"
            const val PRODUCTION_DEBUGGER = "$TRACING.production-debugger"

            object Event {
                const val BREAKPOINT_HIT = "$TRACING.event.breakpoint-hit"
            }
        }

        object Logging {
            const val LOG_COUNT_INDICATOR = "$LOGGING.log-count-indicator"
        }
    }
}
