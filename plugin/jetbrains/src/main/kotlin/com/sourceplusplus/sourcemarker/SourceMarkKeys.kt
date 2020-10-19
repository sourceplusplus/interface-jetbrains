package com.sourceplusplus.sourcemarker

import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.sourcemarker.psi.EndpointDetector

/**
 * Used to associate custom data to [SourceMark]s.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkKeys {
    val SOURCE_PORTAL = SourceKey<SourcePortal>("SOURCE_PORTAL")
    val ENDPOINT_DETECTOR = SourceKey<EndpointDetector>("ENDPOINT_DETECTOR")
    val ARTIFACT_ADVICE = SourceKey<MutableList<ArtifactAdvice>>("ARTIFACT_ADVICE")
}
