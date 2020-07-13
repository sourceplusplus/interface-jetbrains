package com.sourceplusplus.plugin.coordinate.artifact.status

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.JavaPsiFacade
import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.config.SourcePortalConfig
import com.sourceplusplus.api.model.trace.*
import com.sourceplusplus.marker.MarkerUtils
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.marker.source.mark.gutter.event.GutterMarkEventCode
import com.sourceplusplus.marker.source.mark.inlay.InlayMark
import com.sourceplusplus.plugin.SourcePlugin
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJGutterMark
import com.sourceplusplus.plugin.intellij.marker.mark.inlay.IntelliJVirtualText
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import org.jetbrains.annotations.NotNull
import org.jetbrains.uast.UThrowExpression

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern

import static com.intellij.psi.search.GlobalSearchScope.*
import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.ARTIFACT_STATUS_UPDATED
import static com.sourceplusplus.api.util.ArtifactNameUtils.getQualifiedClassName
import static com.sourceplusplus.plugin.intellij.IntelliJStartupActivity.currentProject

/**
 * Keeps track of failing artifact inlay marks.
 *
 * @version 0.3.1
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PluginFailingArtifactStatus extends AbstractVerticle {

    private static final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

    @Override
    void start() throws Exception {
        //listen for failing artifacts to add inlay marks to
        SourceMarkerPlugin.INSTANCE.addGlobalSourceMarkEventListener(new SourceMarkEventListener() {
            @Override
            void handleEvent(@NotNull SourceMarkEvent sourceMarkEvent) {
                if (sourceMarkEvent.eventCode == GutterMarkEventCode.GUTTER_MARK_VISIBLE) {
                    def gutterMark = sourceMarkEvent.sourceMark as IntelliJGutterMark
                    if (gutterMark.sourceArtifact.status().activelyFailing()) {
                        subscribeToFailingArtifactTraces(gutterMark.sourceArtifact)
                        addFailingVirtualText(gutterMark.sourceArtifact)
                    }
                }
            }
        })
        SourcePlugin.vertx.eventBus().consumer(ARTIFACT_STATUS_UPDATED.address, {
            def artifact = it.body() as SourceArtifact
            if (artifact.status().activelyFailing()) {
                subscribeToFailingArtifactTraces(artifact)
                addFailingVirtualText(artifact)
            }
        })
        SourcePlugin.vertx.eventBus().consumer(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address, {
            def traceResult = it.body() as ArtifactTraceResult
            if (traceResult.orderType() == TraceOrderType.FAILED_TRACES) {
                def latestFailedTrace = traceResult.traces()[0]
                def traceStackQuery = TraceSpanStackQuery.builder()
                        .oneLevelDeep(true)
                        .traceId(latestFailedTrace.traceIds()[0]).build()
                SourcePortalConfig.current.getCoreClient(traceResult.appUuid()).getTraceSpans(
                        traceResult.appUuid(), traceResult.artifactQualifiedName(), traceStackQuery, {
                    if (it.succeeded()) {
                        def span = it.result().traceSpans()[0]
                        ApplicationManager.getApplication().invokeLater {
                            addOrUpdateVirtualText(traceResult.artifactQualifiedName(), span)
                        }
                    } else {
                        log.error("Failed to get trace spans", it.cause())
                    }
                })
            }
        })
    }

    private static addOrUpdateVirtualText(String artifactQualifiedName, TraceSpan span) {
        if (!span.logs()) return
        def errorLogs = span.logs()[0]
        def errorKind = errorLogs.data().get("error.kind").replaceAll("\\w+(\\.)", '')
        def errorText = "@ ${dateTimeFormatter.format(errorLogs.time())}"

        //check if error line can be determined
        def useMethodVirtualText = true
        def errorStack = errorLogs.data().get("stack")
        if (errorStack) {
            def qualifiedClassName = getQualifiedClassName(artifactQualifiedName)
            def psiClass = JavaPsiFacade.getInstance(currentProject).findClass(qualifiedClassName, allScope(currentProject))
            def fileMarker = SourceMarkerPlugin.INSTANCE.getSourceFileMarker(psiClass.containingFile)

            def simpleClassName = qualifiedClassName.replaceAll("\\w+(\\.)", "")
            def errorLocation = Pattern.compile("\\($simpleClassName\\.[^.]+:([0-9]+)\\)", Pattern.MULTILINE)
            def matcher = errorLocation.matcher(errorStack)
            if (matcher.find()) {
                def errorLine = matcher.group(1) as int
                def inlayMark = MarkerUtils.getOrCreateExpressionInlayMark(fileMarker, errorLine)
                if (inlayMark != null) {
                    updateInlineVirtualText(inlayMark, errorKind, errorText)
                    useMethodVirtualText = false
                }
            }

            //default to method if error line can't be found
            if (useMethodVirtualText) {
                for (def method : psiClass.methods) {
                    if (MarkerUtils.getFullyQualifiedName(method) == artifactQualifiedName) {
                        def inlayMark = MarkerUtils.getOrCreateMethodInlayMark(fileMarker, method.nameIdentifier)
                        updateMethodVirtualText(inlayMark, errorKind, errorText)
                        break
                    }
                }
            }
        }
    }

    private static void updateInlineVirtualText(InlayMark inlayMark, String errorKind, String errorStatus) {
        if (!inlayMark.sourceFileMarker.containsSourceMark(inlayMark)) inlayMark.apply(true)
        if (inlayMark.configuration.virtualText == null) {
            inlayMark.configuration.virtualText = new IntelliJVirtualText(inlayMark, errorStatus, true)
        }

        def virtualText = (IntelliJVirtualText) inlayMark.configuration.virtualText
        virtualText.updateFailingArtifactStatus(errorKind, errorStatus, inlayMark.psiExpression instanceof UThrowExpression)
    }

    private static void updateMethodVirtualText(InlayMark inlayMark, String errorKind, String errorText) {
        if (!inlayMark.sourceFileMarker.containsSourceMark(inlayMark)) inlayMark.apply(true)
        if (inlayMark.configuration.virtualText == null) {
            inlayMark.configuration.virtualText = new IntelliJVirtualText(inlayMark, errorText, false)
        }

        def virtualText = (IntelliJVirtualText) inlayMark.configuration.virtualText
        virtualText.updateFailingArtifactStatus(errorKind, errorText, false)
    }

    private static void addFailingVirtualText(SourceArtifact sourceArtifact) {
        def traceQuery = TraceQuery.builder()
                .orderType(TraceOrderType.FAILED_TRACES)
                .pageSize(1)
                .appUuid(sourceArtifact.appUuid())
                .artifactQualifiedName(sourceArtifact.artifactQualifiedName())
                .serviceInstanceId(sourceArtifact.status().latestFailedServiceInstance())
                .durationStart(Instant.now().minus(30, ChronoUnit.DAYS))
                .durationStop(Instant.now()) //todo: don't look 30 days back
                .durationStep("SECOND").build()
        SourcePortalConfig.current.getCoreClient(sourceArtifact.appUuid()).getTraces(traceQuery, {
            if (it.succeeded()) {
                if (it.result().traces()) {
                    def latestFailedTrace = it.result().traces()[0]
                    def traceStackQuery = TraceSpanStackQuery.builder()
                            .oneLevelDeep(true)
                            .traceId(latestFailedTrace.traceIds()[0]).build()
                    SourcePortalConfig.current.getCoreClient(sourceArtifact.appUuid()).getTraceSpans(
                            sourceArtifact.appUuid(), sourceArtifact.artifactQualifiedName(), traceStackQuery, {
                        if (it.succeeded()) {
                            if (it.result().traceSpans()) {
                                def span = it.result().traceSpans()[0]
                                ApplicationManager.getApplication().invokeLater {
                                    addOrUpdateVirtualText(sourceArtifact.artifactQualifiedName(), span)
                                }
                            }
                        } else {
                            log.error("Failed to get trace spans", it.cause())
                        }
                    })
                }
            } else {
                log.error("Failed to get traces", it.cause())
            }
        })
    }

    private static void subscribeToFailingArtifactTraces(SourceArtifact sourceArtifact) {
        def subscribeRequest = ArtifactTraceSubscribeRequest.builder()
                .appUuid(sourceArtifact.appUuid())
                .artifactQualifiedName(sourceArtifact.artifactQualifiedName())
                .addOrderTypes(TraceOrderType.FAILED_TRACES)
                .timeFrame(QueryTimeFrame.LAST_5_MINUTES)
                .build()
        SourcePortalConfig.current.getCoreClient(sourceArtifact.appUuid()).subscribeToArtifact(subscribeRequest, {
            if (it.succeeded()) {
                log.info("Successfully subscribed to traces with request: {}", subscribeRequest)
            } else {
                log.error("Failed to subscribe to artifact traces", it.cause())
            }
        })
    }
}
