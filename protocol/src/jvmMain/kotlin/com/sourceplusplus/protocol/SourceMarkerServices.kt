package com.sourceplusplus.protocol

import com.sourceplusplus.protocol.SourceMarkerServices.Utilize.LIVE_INSTRUMENT
import com.sourceplusplus.protocol.SourceMarkerServices.Utilize.LIVE_VIEW
import com.sourceplusplus.protocol.service.live.LiveInstrumentService
import com.sourceplusplus.protocol.service.live.LiveViewService
import com.sourceplusplus.protocol.service.logging.LogCountIndicatorService
import com.sourceplusplus.protocol.service.tracing.LocalTracingService

/**
 * todo: description.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkerServices {

    object Instance {
        var liveInstrument: LiveInstrumentService? = null
        var liveView: LiveViewService? = null
        var localTracing: LocalTracingService? = null
        var logCountIndicator: LogCountIndicatorService? = null
    }

    object Status {
        const val MARKER_CONNECTED = "sm.status.marker-connected"
    }

    object Utilize {
        const val LIVE_VIEW = "sm.service.live-view"
        const val LIVE_INSTRUMENT = "sm.service.live-instrument"
        const val LOCAL_TRACING = "sm.service.local-tracing"
        const val LOG_COUNT_INDICATOR = "sm.service.log-count-indicator"
    }

    object Provide {
        const val LIVE_INSTRUMENT_SUBSCRIBER = "$LIVE_INSTRUMENT.subscriber"
        const val LIVE_VIEW_SUBSCRIBER = "$LIVE_VIEW.subscriber"
    }
}
