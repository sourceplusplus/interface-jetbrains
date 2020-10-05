package com.sourceplusplus.sourcemarker.listeners

import com.intellij.openapi.util.IconLoader
import com.sourceplusplus.marker.MarkerUtils
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.inlay.config.InlayMarkVirtualText
import com.sourceplusplus.protocol.advice.AdviceListener
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.sourcemarker.SourceMarkKeys.ARTIFACT_ADVICE
import com.sourceplusplus.sourcemarker.SourceMarkKeys.ENDPOINT_DETECTOR
import com.sourceplusplus.sourcemarker.SourceMarkKeys.SOURCE_PORTAL
import org.jetbrains.kotlin.idea.util.application.runReadAction

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ArtifactAdviceListener : AdviceListener {

    //todo: pending advice

    private val exclamationTriangle = IconLoader.getIcon(
        "/icons/exclamation-triangle.svg",
        PluginSourceMarkEventListener::class.java
    )

    override suspend fun advised(advice: ArtifactAdvice) {
        val sourceMark = if (advice.artifact.type == ArtifactType.ENDPOINT) {
            val operationName = advice.artifact.identifier
            SourceMarkerPlugin.getSourceMarks()
                .filterIsInstance<MethodSourceMark>()
                .firstOrNull {
                    val endpointName = it.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointName(it)
                    endpointName == operationName
                }
        } else if (advice.artifact.type == ArtifactType.STATEMENT) {
            val qualifiedClassName = advice.artifact.identifier
                .substring(0, advice.artifact.identifier.lastIndexOf("."))
            runReadAction {
                val fileMarker = SourceMarkerPlugin.getSourceFileMarker(qualifiedClassName)
                if (fileMarker != null) {
                    try {
                        val inlayMark = MarkerUtils.getOrCreateExpressionInlayMark(
                            fileMarker, advice.artifact.lineNumber!!
                        )!!
                        if (inlayMark.configuration.virtualText == null) {
                            inlayMark.configuration.virtualText = InlayMarkVirtualText(inlayMark, "hello")
                            inlayMark.configuration.virtualText!!.icon = exclamationTriangle
                            inlayMark.apply()

                            val gutterMark = MarkerUtils.getOrCreateExpressionGutterMark(
                                fileMarker, advice.artifact.lineNumber!!
                            )!!
                            gutterMark.configuration.icon = exclamationTriangle
                            gutterMark.apply()
                        } else {
                            inlayMark.configuration.virtualText!!
                                .updateVirtualText("hello" + System.currentTimeMillis())
                        }
                        println(inlayMark)
                    } catch (throwable: Throwable) {
                        throwable.printStackTrace()
                    }
                }
            }
            null //SourceMarkerPlugin.getSourceMark(advice.artifact.qualifiedFunctionName!!, SourceMark.Type.GUTTER)
        } else {
            null
        }
        if (sourceMark != null) {
            sourceMark.putUserData(ARTIFACT_ADVICE, mutableListOf())
            sourceMark.getUserData(ARTIFACT_ADVICE)!!.add(advice)
            sourceMark.getUserData(SOURCE_PORTAL)!!.advice.add(advice)
            println("added advice")
        }
    }
}