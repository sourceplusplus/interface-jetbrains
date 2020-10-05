package com.sourceplusplus.sourcemarker.listeners

import com.sourceplusplus.marker.MarkerUtils
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.protocol.advice.AdviceListener
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.sourcemarker.GutterMarkIcons
import com.sourceplusplus.sourcemarker.SourceMarkKeys.ARTIFACT_ADVICE
import com.sourceplusplus.sourcemarker.SourceMarkKeys.ENDPOINT_DETECTOR
import com.sourceplusplus.sourcemarker.SourceMarkKeys.SOURCE_PORTAL
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.j2k.getContainingMethod

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ArtifactAdviceListener : AdviceListener, SourceMarkEventListener {

    private val pendingAdvice = mutableListOf<ArtifactAdvice>()

    override suspend fun advised(advice: ArtifactAdvice) {
        when (advice.artifact.type) {
            ArtifactType.ENDPOINT -> createEndpointAdvice(advice)
            ArtifactType.STATEMENT -> runReadAction { createExpressionAdvice(advice) }
            else -> TODO("impl")
        }
    }

    private suspend fun createEndpointAdvice(advice: ArtifactAdvice) {
        val operationName = advice.artifact.identifier
        val sourceMark = SourceMarkerPlugin.getSourceMarks()
            .filterIsInstance<MethodSourceMark>()
            .firstOrNull { it.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointName(it) == operationName }
        if (sourceMark != null) {
            addAdviceData(sourceMark, advice)
        } else {
            pendingAdvice.add(advice)
        }
    }

    private fun createExpressionAdvice(advice: ArtifactAdvice) {
        val qualifiedClassName = advice.artifact.identifier
            .substring(0, advice.artifact.identifier.lastIndexOf("."))
        val fileMarker = SourceMarkerPlugin.getSourceFileMarker(qualifiedClassName)
        if (fileMarker != null) {
            val gutterMark = MarkerUtils.getOrCreateExpressionGutterMark(
                fileMarker, advice.artifact.lineNumber!!
            )!!
            if (!fileMarker.containsSourceMark(gutterMark)) {
                gutterMark.configuration.icon = GutterMarkIcons.exclamationTriangle

                addAdviceData(gutterMark, advice)
                gutterMark.apply()

                val containingMethod = gutterMark.getPsiElement().getContainingMethod()
                if (containingMethod != null) {
                    val methodIdentifier = containingMethod.nameIdentifier!!
                    val methodGutterMark = methodIdentifier.getUserData(SourceKey.GutterMark)!!
                    addAdviceData(methodGutterMark, advice)
                }
            }
        } else {
            pendingAdvice.add(advice)
        }
    }

    override fun handleEvent(event: SourceMarkEvent) {
        TODO("Not yet implemented")
    }

    private fun addAdviceData(sourceMark: SourceMark, advice: ArtifactAdvice) {
        sourceMark.putUserData(ARTIFACT_ADVICE, mutableListOf())
        sourceMark.getUserData(ARTIFACT_ADVICE)!!.add(advice)
        sourceMark.getUserData(SOURCE_PORTAL)?.advice?.add(advice)
        println("added advice data")
    }
}