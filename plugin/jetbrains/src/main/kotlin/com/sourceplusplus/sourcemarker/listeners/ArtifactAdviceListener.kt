package com.sourceplusplus.sourcemarker.listeners

import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.protocol.advice.AdviceListener
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.sourcemarker.SourceMarkKeys.ARTIFACT_ADVICE
import com.sourceplusplus.sourcemarker.SourceMarkKeys.ENDPOINT_DETECTOR
import com.sourceplusplus.sourcemarker.SourceMarkKeys.SOURCE_PORTAL

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ArtifactAdviceListener : AdviceListener {

    //todo: pending advice

    override suspend fun advised(advice: ArtifactAdvice) {
        val sourceMark = if (advice.artifact.type == ArtifactType.ENDPOINT) {
            val operationName = advice.artifact.identifier
            SourceMarkerPlugin.getSourceMarks()
                .filterIsInstance<MethodSourceMark>()
                .firstOrNull {
                    val endpointName = it.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointName(it)
                    endpointName == operationName
                }
        } else {
            null //SourceMarkerPlugin.getSourceMark(advice.artifact.qualifiedFunctionName!!, SourceMark.Type.GUTTER)
        }
        if (sourceMark != null) {
            sourceMark.putUserData(ARTIFACT_ADVICE, mutableListOf())
            sourceMark.getUserData(ARTIFACT_ADVICE)!!.add(advice)
            sourceMark.getUserData(SOURCE_PORTAL)!!.advice.add(advice)
            println("added advice")
        }
    }
}