package com.sourceplusplus.plugin.intellij.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.metric.MetricType
import com.sourceplusplus.api.model.trace.ArtifactTraceSubscribeRequest
import com.sourceplusplus.api.model.trace.TraceOrderType
import com.sourceplusplus.plugin.coordinate.artifact.track.PluginArtifactSubscriptionTracker
import com.sourceplusplus.plugin.intellij.marker.IntelliJSourceFileMarker
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastContextKt
import plus.sourceplus.marker.plugin.SourceMarkerPlugin

import static com.sourceplusplus.plugin.PluginBootstrap.getSourcePlugin

/**
 * Intention used to subscribe to all source code artifacts in a given file.
 * Artifacts currently supported:
 *  - methods
 *
 * @version 0.2.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class SubscribeFileSourceArtifactsIntention extends PsiElementBaseIntentionAction {

    @Override
    boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (sourcePlugin == null || element == null) return false
        def originalElement = element
        boolean inMethod = false
        while (element != null && !inMethod) {
            def uMethod = UastContextKt.toUElement(element)
            if (uMethod instanceof UMethod) {
                inMethod = true
            }
            element = element.parent
        }
        if (!inMethod) {
            //check for unsubscribed source marks
            def fileMarker = SourceMarkerPlugin.INSTANCE.getSourceFileMarker(
                    originalElement.containingFile) as IntelliJSourceFileMarker
            if (fileMarker) {
                return fileMarker.sourceMarks.find { !it.artifactSubscribed }
            }
        }
        return false
    }

    @Override
    @SuppressWarnings("GroovyVariableNotAssigned")
    void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        def fileMarker = SourceMarkerPlugin.INSTANCE.getSourceFileMarker(
                element.containingFile) as IntelliJSourceFileMarker
        if (fileMarker) {
            fileMarker.sourceMarks.each {
                if (!it.artifactSubscribed) {
                    //subscribe to metrics
                    def metricTypes = [MetricType.Throughput_Average,
                                       MetricType.ResponseTime_Average,
                                       MetricType.ServiceLevelAgreement_Average,
                                       MetricType.ResponseTime_99Percentile,
                                       MetricType.ResponseTime_95Percentile,
                                       MetricType.ResponseTime_90Percentile,
                                       MetricType.ResponseTime_75Percentile,
                                       MetricType.ResponseTime_50Percentile]
                    def metricSubscribeRequest = ArtifactMetricSubscribeRequest.builder()
                            .appUuid(SourcePluginConfig.current.activeEnvironment.appUuid)
                            .artifactQualifiedName(it.artifactQualifiedName)
                            .timeFrame(QueryTimeFrame.LAST_15_MINUTES)
                            .metricTypes(metricTypes).build()
                    sourcePlugin.vertx.eventBus().send(
                            PluginArtifactSubscriptionTracker.SUBSCRIBE_TO_ARTIFACT, metricSubscribeRequest)

                    //subscribe to traces
                    def traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
                            .appUuid(SourcePluginConfig.current.activeEnvironment.appUuid)
                            .artifactQualifiedName(it.artifactQualifiedName)
                            .addOrderTypes(TraceOrderType.LATEST_TRACES, TraceOrderType.SLOWEST_TRACES)
                            .build()
                    sourcePlugin.vertx.eventBus().send(
                            PluginArtifactSubscriptionTracker.SUBSCRIBE_TO_ARTIFACT, traceSubscribeRequest)
                }
            }
        }
    }

    @NotNull
    String getText() {
        return "Subscribe to all source artifacts"
    }

    @NotNull
    String getFamilyName() {
        return getText()
    }

    @Override
    boolean startInWriteAction() {
        return false
    }

    @Nullable
    @Override
    PsiElement getElementToMakeWritable(@NotNull PsiFile currentFile) {
        return currentFile
    }
}