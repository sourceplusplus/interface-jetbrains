package com.sourceplusplus.sourcemarker.listeners

import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.protocol.advice.AdviceListener
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import com.sourceplusplus.sourcemarker.mark.SourceMarkConstructor
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys.ARTIFACT_ADVICE
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys.ENDPOINT_DETECTOR
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys.SOURCE_PORTAL
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

    override fun handleEvent(event: SourceMarkEvent) {
        if (event.eventCode == SourceMarkEventCode.MARK_ADDED) {
            GlobalScope.launch(SourceMarkerPlugin.vertx.dispatcher()) {
                pendingAdvice.toList().forEach {
                    advised(it)
                }
            }
        }
    }

    private suspend fun createEndpointAdvice(advice: ArtifactAdvice) {
        val operationName = advice.artifact.identifier
        val sourceMark = SourceMarker.getSourceMarks()
            .filterIsInstance<MethodSourceMark>()
            .firstOrNull { it.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointName(it) == operationName }
        if (sourceMark != null) {
            SourceMarkConstructor.attachAdvice(sourceMark, advice)
            addAdviceData(sourceMark, advice)
        } else {
            pendingAdvice.add(advice)
        }
    }

    private fun createExpressionAdvice(advice: ArtifactAdvice) {
        val qualifiedClassName = advice.artifact.identifier
            .substring(0, advice.artifact.identifier.lastIndexOf("."))
        val fileMarker = SourceMarker.getSourceFileMarker(qualifiedClassName)
        if (fileMarker != null) {
            val sourceMark = SourceMarkConstructor.getOrSetupSourceMark(fileMarker, advice)
            if (sourceMark != null) {
                addAdviceData(sourceMark, advice)
            }
        } else {
            pendingAdvice.add(advice)
        }
    }

    private fun addAdviceData(sourceMark: SourceMark, advice: ArtifactAdvice) {
        pendingAdvice.remove(advice)
        if (sourceMark.getUserData(ARTIFACT_ADVICE) == null) {
            sourceMark.putUserData(ARTIFACT_ADVICE, mutableListOf())
        }

        val activeAdvice = sourceMark.getUserData(ARTIFACT_ADVICE)!!
        val updatedAdvice = activeAdvice.any {
            if (it.isSameArtifactAdvice(advice)) {
                it.updateArtifactAdvice(advice)
                true
            } else {
                false
            }
        }
        if (updatedAdvice) {
            log.trace("Updated artifact advice: $advice")
        } else {
            activeAdvice.add(advice)
            sourceMark.getUserData(SOURCE_PORTAL)?.advice?.add(advice)
            log.debug("Added artifact advice: $advice")
        }
    }
}
