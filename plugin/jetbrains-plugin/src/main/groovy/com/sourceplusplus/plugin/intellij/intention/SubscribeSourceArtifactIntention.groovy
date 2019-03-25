package com.sourceplusplus.plugin.intellij.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.metric.MetricType
import com.sourceplusplus.api.model.trace.ArtifactTraceSubscribeRequest
import com.sourceplusplus.api.model.trace.TraceOrderType
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.coordinate.artifact.track.PluginArtifactSubscriptionTracker
import com.sourceplusplus.plugin.intellij.util.IntelliUtils
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastContextKt

import static com.sourceplusplus.plugin.PluginBootstrap.*

/**
 * Intention used to subscribe to source code artifacts.
 * Artifacts currently supported:
 *  - methods
 *
 * @version 0.1.3
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class SubscribeSourceArtifactIntention extends PsiElementBaseIntentionAction {

    @NotNull
    String getText() {
        return "Subscribe to source artifact"
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

    @Override
    boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (sourcePlugin == null) return false
        while (element != null) {
            def uMethod = UastContextKt.toUElement(element)
            if (uMethod instanceof UMethod) {
                def qualifiedName = IntelliUtils.getArtifactQualifiedName(uMethod)
                def sourceMark = sourcePlugin.getSourceMark(qualifiedName)
                if (sourceMark != null && !sourceMark.artifactSubscribed) {
                    return true
                }
            }
            element = element.parent
        }
        return false
    }

    @Override
    @SuppressWarnings("GroovyVariableNotAssigned")
    void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        String artifactQualifiedName
        while (element != null) {
            def uMethod = UastContextKt.toUElement(element)
            if (uMethod instanceof UMethod) {
                artifactQualifiedName = IntelliUtils.getArtifactQualifiedName(uMethod)
                break
            }
            element = element.parent
        }

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
                .appUuid(SourcePluginConfig.current.appUuid)
                .artifactQualifiedName(artifactQualifiedName)
                .timeFrame(QueryTimeFrame.LAST_15_MINUTES)
                .metricTypes(metricTypes).build()
        sourcePlugin.vertx.eventBus().send(
                PluginArtifactSubscriptionTracker.SUBSCRIBE_TO_ARTIFACT, metricSubscribeRequest)

        //subscribe to traces
        def traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
                .appUuid(SourcePluginConfig.current.appUuid)
                .artifactQualifiedName(artifactQualifiedName)
                .orderType(TraceOrderType.LATEST_TRACES)
                .build()
        sourcePlugin.vertx.eventBus().send(
                PluginArtifactSubscriptionTracker.SUBSCRIBE_TO_ARTIFACT, traceSubscribeRequest)
    }
}