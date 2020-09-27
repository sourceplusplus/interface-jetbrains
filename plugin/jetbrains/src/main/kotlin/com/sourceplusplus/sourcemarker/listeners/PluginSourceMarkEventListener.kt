package com.sourceplusplus.sourcemarker.listeners

import com.sourceplusplus.marker.MarkerUtils
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.mentor.SourceMentor
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PluginSourceMarkEventListener(private val sourceMentor: SourceMentor) : SourceMarkEventListener {

    override fun handleEvent(event: SourceMarkEvent) {
        if (event.eventCode == SourceMarkEventCode.MARK_ADDED) {
            if (event.sourceMark is MethodSourceMark) {
                val methodMark = event.sourceMark as MethodSourceMark
                sourceMentor.getAllMethodAdvice(
                    ArtifactQualifiedName(
                        methodMark.artifactQualifiedName,
                        "", //todo: commitId
                        ArtifactType.METHOD
                    )
                ).forEach {
//                    MarkerUtils.getOrCreateMethodGutterMark()
//                    methodMark.sourceFileMarker.applySourceMark()
                }
                //todo: gather and display markings
                //todo: gather and display advice
            }
        }
    }
}
