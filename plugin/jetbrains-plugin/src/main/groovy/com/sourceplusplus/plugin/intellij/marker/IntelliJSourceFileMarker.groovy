package com.sourceplusplus.plugin.intellij.marker

import com.intellij.psi.PsiFile
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.plugin.SourcePlugin
import com.sourceplusplus.plugin.coordinate.artifact.track.PluginArtifactSubscriptionTracker
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJSourceMark
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJMethodGutterMark
import groovy.util.logging.Slf4j
import org.jetbrains.annotations.NotNull
import org.jetbrains.uast.UMethod

import java.util.concurrent.atomic.AtomicBoolean

import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED
import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.ARTIFACT_STATUS_UPDATED
import static com.sourceplusplus.api.util.ArtifactNameUtils.getShortQualifiedFunctionName

/**
 * Extension of the SourceMarker for handling IntelliJ.
 *
 * @version 0.3.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class IntelliJSourceFileMarker extends SourceFileMarker {

    static AtomicBoolean setupSourceMarkArtifactSyncer = new AtomicBoolean()

    static void keepSourceMarkArtifactsUpToDate() {
        if (!setupSourceMarkArtifactSyncer.getAndSet(true)) {
            SourcePlugin.vertx.eventBus().consumer(ARTIFACT_CONFIG_UPDATED.address, {
                def artifact = it.body() as SourceArtifact
                log.info("Artifact config updated. Artifact qualified name: {}",
                        getShortQualifiedFunctionName(artifact.artifactQualifiedName()))

                SourceMarkerPlugin.INSTANCE.getSourceMarks(artifact.artifactQualifiedName()).each {
                    (it as IntelliJSourceMark).updateSourceArtifact(artifact)
                }
            })
            SourcePlugin.vertx.eventBus().consumer(ARTIFACT_STATUS_UPDATED.address, {
                def artifact = it.body() as SourceArtifact
                log.info("Artifact status updated. Artifact qualified name: {}",
                        getShortQualifiedFunctionName(artifact.artifactQualifiedName()))

                SourceMarkerPlugin.INSTANCE.getSourceMarks(artifact.artifactQualifiedName()).each {
                    (it as IntelliJSourceMark).updateSourceArtifact(artifact)
                }
            })
        }
    }

    IntelliJSourceFileMarker(@NotNull PsiFile psiFile) {
        super(psiFile)
        keepSourceMarkArtifactsUpToDate()

        SourcePlugin.vertx.eventBus().send(PluginArtifactSubscriptionTracker.SYNC_AUTOMATIC_SUBSCRIPTIONS, true)
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    List<IntelliJSourceMark> getSourceMarks() {
        return super.getSourceMarks() as List<IntelliJSourceMark>
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    MethodSourceMark createSourceMark(@NotNull UMethod psiMethod, @NotNull SourceMark.Type type) {
        if (type == SourceMark.Type.GUTTER) {
            def sourceMark = new IntelliJMethodGutterMark(this, psiMethod)
            log.info("Created gutter mark: " + sourceMark)
            SourcePlugin.vertx.eventBus().publish(IntelliJSourceMark.SOURCE_MARK_CREATED, sourceMark)
            return sourceMark
        } else {
            return super.createSourceMark(psiMethod, type)
        }
    }
}