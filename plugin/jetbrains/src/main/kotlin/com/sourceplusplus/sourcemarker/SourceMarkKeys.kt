package com.sourceplusplus.sourcemarker

import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.sourcemarker.psi.EndpointDetector

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkKeys {
    val SOURCE_PORTAL = SourceKey<SourcePortal>("SOURCE_PORTAL")
    val ENDPOINT_DETECTOR = SourceKey<EndpointDetector>("ENDPOINT_DETECTOR")
    val ARTIFACT_ADVICE = SourceKey<MutableList<ArtifactAdvice>>("ARTIFACT_ADVICE")

    //todo: remove direct access and use endpoint detector
    val ENDPOINT_ID = SourceKey<String>("ENDPOINT_ID")
    val ENDPOINT_NAME = SourceKey<String>("ENDPOINT_NAME")
}