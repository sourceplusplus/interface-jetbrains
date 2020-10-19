package com.sourceplusplus.sourcemarker.listeners

import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.marker.source.SourceMarkerUtils
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.protocol.advice.AdviceListener
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.sourcemarker.GutterMarkIcons
import com.sourceplusplus.sourcemarker.SourceMarkKeys.ARTIFACT_ADVICE
import com.sourceplusplus.sourcemarker.SourceMarkKeys.ENDPOINT_DETECTOR
import com.sourceplusplus.sourcemarker.SourceMarkKeys.SOURCE_PORTAL
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ArtifactAdviceListener : AdviceListener, SourceMarkEventListener {

    companion object {
        private val log = LoggerFactory.getLogger(ArtifactAdviceListener::class.java)
    }

    private val pendingAdvice = mutableSetOf<ArtifactAdvice>()

    override suspend fun advised(advice: ArtifactAdvice) {
        when (advice.artifact.type) {
            ArtifactType.ENDPOINT -> createEndpointAdvice(advice)
            ArtifactType.STATEMENT -> runReadAction { createExpressionAdvice(advice) }
            else -> TODO("impl")
        }
    }

    private suspend fun createEndpointAdvice(advice: ArtifactAdvice) {
        val operationName = advice.artifact.identifier
        val sourceMark = SourceMarker.getSourceMarks()
            .filterIsInstance<MethodSourceMark>()
            .firstOrNull { it.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointName(it) == operationName }
        if (sourceMark != null) {
            addAdviceData(sourceMark, advice)
        } else {
            updateOrAddAdvice(pendingAdvice, advice)
        }
    }

    private fun createExpressionAdvice(advice: ArtifactAdvice) {
        val qualifiedClassName = advice.artifact.identifier
            .substring(0, advice.artifact.identifier.lastIndexOf("."))
        val fileMarker = SourceMarker.getSourceFileMarker(qualifiedClassName)
        if (fileMarker != null) {
            val gutterMark = SourceMarkerUtils.getOrCreateExpressionGutterMark(
                fileMarker, advice.artifact.lineNumber!!
            )!!
            if (!fileMarker.containsSourceMark(gutterMark)) {
                gutterMark.configuration.icon = GutterMarkIcons.exclamationTriangle

                addAdviceData(gutterMark, advice)
                gutterMark.apply()

                //todo: instead should have a method for getting expression/inlay marks inside of a specified method
//                val containingMethod = gutterMark.getPsiElement().getContainingMethod()
//                if (containingMethod != null) {
//                    val methodGutterMark = getOrCreateMethodGutterMark(fileMarker, containingMethod)!!
//                    addAdviceData(methodGutterMark, advice)
//                }
            } else {
                if (gutterMark.getUserData(ARTIFACT_ADVICE) == null) {
                    gutterMark.putUserData(ARTIFACT_ADVICE, mutableListOf())
                }
                val expressionAdvice = gutterMark.getUserData(ARTIFACT_ADVICE)!!
                updateOrAddAdvice(expressionAdvice, advice)
            }
        } else {
            updateOrAddAdvice(pendingAdvice, advice)
        }
    }

    //todo: argument could be made mentor should handle updating advice
    // maybe even pending advice via checking if advice was consumed
    private fun updateOrAddAdvice(adviceList: MutableCollection<ArtifactAdvice>, advice: ArtifactAdvice) {
        val updatedAdvice = adviceList.any {
            if (it.isSameArtifactAdvice(advice)) {
                pendingAdvice.remove(advice)
                if (it !== advice) { //todo: how does the same advice get here?
                    it.updateArtifactAdvice(advice)
                }
                true
            } else {
                false
            }
        }
        if (!updatedAdvice) {
            adviceList.add(advice)
        }
    }

    override fun handleEvent(event: SourceMarkEvent) {
        if (event.eventCode == SourceMarkEventCode.MARK_ADDED) {
            GlobalScope.launch(SourceMarkerPlugin.vertx.dispatcher()) {
                pendingAdvice.toList().forEach {
                    advised(it)
                }
            }
        }
    }

    private fun addAdviceData(sourceMark: SourceMark, advice: ArtifactAdvice) {
        pendingAdvice.remove(advice)
        if (sourceMark.getUserData(ARTIFACT_ADVICE) == null) {
            sourceMark.putUserData(ARTIFACT_ADVICE, mutableListOf())
        }
        sourceMark.getUserData(ARTIFACT_ADVICE)!!.add(advice)
        sourceMark.getUserData(SOURCE_PORTAL)?.advice?.add(advice)
        log.info("Added artifact advice: $advice")
    }
}
