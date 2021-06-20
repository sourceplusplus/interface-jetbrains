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
import com.sourceplusplus.sourcemarker.PluginBundle.message
import com.sourceplusplus.sourcemarker.icons.SourceMarkerIcons
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin.SOURCE_RED
import kotlinx.datetime.Clock
import org.jetbrains.uast.UThrowExpression
import org.slf4j.LoggerFactory

/**
 * Sets up the appropriate [SourceMark] display configuration based on [AdviceType].
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkConstructor {

    private val log = LoggerFactory.getLogger(SourceMarkConstructor::class.java)
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

    fun getOrSetupSourceMark(fileMarker: SourceFileMarker, advice: ArtifactAdvice): SourceMark? {
        when (advice.type) {
            AdviceType.RampDetectionAdvice -> {
                val gutterMark = getOrCreateExpressionGutterMark(fileMarker, advice.artifact.lineNumber!!)
                return if (gutterMark.isPresent) {
                    if (!fileMarker.containsSourceMark(gutterMark.get())) {
                        attachAdvice(gutterMark.get(), advice)
                        gutterMark.get().apply()
                    }
                    gutterMark.get()
                } else {
                    log.warn("No detected expression at line {}. Gutter mark ignored", advice.artifact.lineNumber!!)
                    null
                }
            }
            AdviceType.ActiveExceptionAdvice -> {
                val inlayMark = getOrCreateExpressionInlayMark(fileMarker, advice.artifact.lineNumber!!)
                return if (inlayMark.isPresent) {
                    if (!fileMarker.containsSourceMark(inlayMark.get())) {
                        attachAdvice(inlayMark.get(), advice)
                        inlayMark.get().apply()
                    }
                    inlayMark.get()
                } else {
                    log.warn("No detected expression at line {}. Inlay mark ignored", advice.artifact.lineNumber!!)
                    null
                }
            }
        }
    }

    fun attachAdvice(sourceMark: SourceMark, advice: ArtifactAdvice) = when (sourceMark.type) {
        SourceMark.Type.GUTTER -> attachAdvice(sourceMark as GutterMark, advice)
        SourceMark.Type.INLAY -> attachAdvice(sourceMark as InlayMark, advice)
    }

    private fun attachAdvice(gutterMark: GutterMark, advice: ArtifactAdvice) {
        gutterMark.configuration.icon = SourceMarkerIcons.getGutterMarkIcon(advice)
        gutterMark.setVisible(true)
        gutterMark.sourceFileMarker.refresh()
    }

    @Suppress("MagicNumber")
    private fun attachAdvice(inlayMark: InlayMark, advice: ArtifactAdvice) {
        when (advice) {
            is ActiveExceptionAdvice -> {
                val expressionMark = inlayMark as ExpressionInlayMark
                val prettyTimeAgo = if (expressionMark.getPsiExpression() is UThrowExpression) {
                    {
                        val occurred = (Clock.System.now() - advice.occurredAt).toPrettyDuration() +
                                " " + message("ago")
                        " //${message("last_occurred")} $occurred "
                    }
                } else {
                    {
                        val exceptionType = advice.stackTrace.exceptionType.substringAfterLast(".")
                        val occurred = (Clock.System.now() - advice.occurredAt).toPrettyDuration() +
                                " " + message("ago")
                        " //${message("threw")} $exceptionType $occurred "
                    }
                }

                inlayMark.configuration.virtualText = InlayMarkVirtualText(inlayMark, prettyTimeAgo.invoke())
                inlayMark.configuration.virtualText!!.textAttributes.foregroundColor = SOURCE_RED
                inlayMark.configuration.virtualText!!.useInlinePresentation = true
                inlayMark.configuration.activateOnMouseClick = false

                inlayMark.putUserData(ADVICE_TIMER, SourceMarkerPlugin.vertx.setPeriodic(1000) {
                    inlayMark.configuration.virtualText!!.updateVirtualText(prettyTimeAgo.invoke())
                })

                //todo: shouldn't be creating gutter mark here
                val gutterMark = getOrCreateExpressionGutterMark(
                    inlayMark.sourceFileMarker, advice.artifact.lineNumber!!
                ).get()
                if (!gutterMark.sourceFileMarker.containsSourceMark(gutterMark)) {
                    gutterMark.configuration.icon = SourceMarkerIcons.activeException
                    gutterMark.apply()
                }
            }
        }
    }
}
