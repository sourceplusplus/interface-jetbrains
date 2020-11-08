package com.sourceplusplus.sourcemarker.mark

import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.SourceMarkerUtils.getOrCreateExpressionGutterMark
import com.sourceplusplus.marker.source.SourceMarkerUtils.getOrCreateExpressionInlayMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.marker.source.mark.gutter.GutterMark
import com.sourceplusplus.marker.source.mark.inlay.ExpressionInlayMark
import com.sourceplusplus.marker.source.mark.inlay.InlayMark
import com.sourceplusplus.marker.source.mark.inlay.config.InlayMarkVirtualText
import com.sourceplusplus.protocol.advice.AdviceType
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.advice.informative.ActiveExceptionAdvice
import com.sourceplusplus.protocol.utils.toPrettyDuration
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import kotlinx.datetime.Clock
import org.jetbrains.uast.UThrowExpression
import java.awt.Color

/**
 * Sets up the appropriate [SourceMark] display configuration based on [AdviceType].
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkConstructor {

    private val SOURCE_RED = Color(225, 72, 59)
    private val ADVICE_TIMER = SourceKey<Long>("ADVICE_TIMER")

    fun tearDownSourceMark(sourceMark: SourceMark) {
        val artifactAdvice = sourceMark.getUserData(SourceMarkKeys.ARTIFACT_ADVICE)
        if (artifactAdvice == null || artifactAdvice.isEmpty()) {
            return
        }

        artifactAdvice.forEach {
            when (it.type) {
                AdviceType.ActiveExceptionAdvice -> {
                    val timerId = sourceMark.getUserData(ADVICE_TIMER)
                    if (timerId != null) {
                        SourceMarkerPlugin.vertx.cancelTimer(timerId)
                        sourceMark.putUserData(ADVICE_TIMER, null)
                    }
                }
                else -> {
                    //no additional tear down necessary
                }
            }
        }
        artifactAdvice.clear()
    }

    fun getOrSetupSourceMark(fileMarker: SourceFileMarker, advice: ArtifactAdvice): SourceMark {
        when (advice.type) {
            AdviceType.RampDetectionAdvice -> {
                val gutterMark = getOrCreateExpressionGutterMark(fileMarker, advice.artifact.lineNumber!!)!!
                if (!fileMarker.containsSourceMark(gutterMark)) {
                    attachAdvice(gutterMark, advice)
                    gutterMark.apply()
                }
                return gutterMark
            }
            AdviceType.ActiveExceptionAdvice -> {
                val inlayMark = getOrCreateExpressionInlayMark(fileMarker, advice.artifact.lineNumber!!)!!
                if (!fileMarker.containsSourceMark(inlayMark)) {
                    attachAdvice(inlayMark, advice)
                    inlayMark.apply()
                }
                return inlayMark
            }
        }
    }

    fun attachAdvice(sourceMark: SourceMark, advice: ArtifactAdvice) = when (sourceMark.type) {
        SourceMark.Type.GUTTER -> attachAdvice(sourceMark as GutterMark, advice)
        SourceMark.Type.INLAY -> attachAdvice(sourceMark as InlayMark, advice)
    }

    private fun attachAdvice(gutterMark: GutterMark, advice: ArtifactAdvice) {
        gutterMark.configuration.icon = GutterMarkIcons.getGutterMarkIcon(advice)
        gutterMark.setVisible(true)
        gutterMark.sourceFileMarker.refresh()
    }

    private fun attachAdvice(inlayMark: InlayMark, advice: ArtifactAdvice) {
        when (advice) {
            is ActiveExceptionAdvice -> {
                val expressionMark = inlayMark as ExpressionInlayMark
                val prettyTimeAgo = if (expressionMark.getPsiExpresion() is UThrowExpression) {
                    { " //Last occurred ${(Clock.System.now() - advice.occurredAt).toPrettyDuration()} ago       " }
                } else {
                    { " //Threw ${advice.stackTrace.exceptionType.substringAfterLast(".")} ${(Clock.System.now() - advice.occurredAt).toPrettyDuration()} ago       " }
                }

                inlayMark.configuration.virtualText = InlayMarkVirtualText(inlayMark, prettyTimeAgo.invoke())
                inlayMark.configuration.virtualText!!.textAttributes.foregroundColor = SOURCE_RED
                inlayMark.configuration.virtualText!!.useInlinePresentation = true
                inlayMark.configuration.activateOnMouseClick = false

                inlayMark.putUserData(ADVICE_TIMER, SourceMarkerPlugin.vertx.setPeriodic(1000) {
                    inlayMark.configuration.virtualText!!.updateVirtualText(prettyTimeAgo.invoke())
                })
            }
        }
    }
}
