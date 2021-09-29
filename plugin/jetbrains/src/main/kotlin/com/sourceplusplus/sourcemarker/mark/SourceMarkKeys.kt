package com.sourceplusplus.sourcemarker.mark

import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.sourcemarker.psi.EndpointDetector
import com.sourceplusplus.sourcemarker.psi.LoggerDetector
import com.sourceplusplus.sourcemarker.service.InstrumentEventListener

/**
 * Used to associate custom data to [SourceMark]s.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkKeys {
    val SOURCE_PORTAL = SourceKey<SourcePortal>("SOURCE_PORTAL")
    val ENDPOINT_DETECTOR = SourceKey<EndpointDetector>("ENDPOINT_DETECTOR")
    val LOGGER_DETECTOR = SourceKey<LoggerDetector>("LOGGER_DETECTOR")
    val ARTIFACT_ADVICE = SourceKey<MutableList<ArtifactAdvice>>("ARTIFACT_ADVICE")
    val LOG_ID = SourceKey<String>("LOG_ID")
    val BREAKPOINT_ID = SourceKey<String>("BREAKPOINT_ID")
    val GROUPED_MARKS = SourceKey<List<SourceMark>>("GROUPED_MARKS")
    val INSTRUMENT_EVENT_LISTENERS = SourceKey<List<InstrumentEventListener>>("INSTRUMENT_EVENT_LISTENERS")
}
