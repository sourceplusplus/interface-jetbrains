package com.sourceplusplus.plugin.intellij.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import com.sourceplusplus.api.model.artifact.SourceArtifactUnsubscribeRequest
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.coordinate.artifact.track.PluginArtifactSubscriptionTracker
import com.sourceplusplus.plugin.intellij.marker.IntelliJSourceFileMarker
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastContextKt

import static com.sourceplusplus.plugin.PluginBootstrap.getSourcePlugin

/**
 * Intention used to unsubscribe from all source code artifacts in a given file.
 * Artifacts currently supported:
 *  - methods
 *
 * @version 0.1.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class UnsubscribeFileSourceArtifactsIntention extends PsiElementBaseIntentionAction {

    @NotNull
    String getText() {
        return "Unsubscribe from all source artifacts"
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
            //check for subscribed source marks
            def fileMarkers = sourcePlugin.getAvailableSourceFileMarkers() as List<IntelliJSourceFileMarker>
            def fileMarker = fileMarkers.find { it.psiFile == originalElement.containingFile }
            if (fileMarker) {
                return fileMarker.sourceMarks.find { it.artifactSubscribed }
            }
        }
        return false
    }

    @Override
    @SuppressWarnings("GroovyVariableNotAssigned")
    void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        def fileMarkers = sourcePlugin.getAvailableSourceFileMarkers() as List<IntelliJSourceFileMarker>
        def fileMarker = fileMarkers.find { it.psiFile == element.containingFile }
        if (fileMarker) {
            fileMarker.sourceMarks.each {
                if (it.artifactSubscribed) {
                    //unsubscribe from artifact
                    def unsubscribeRequest = SourceArtifactUnsubscribeRequest.builder()
                            .appUuid(SourcePluginConfig.current.appUuid)
                            .artifactQualifiedName(it.artifactQualifiedName)
                            .removeAllArtifactSubscriptions(true)
                            .build()
                    sourcePlugin.vertx.eventBus().send(
                            PluginArtifactSubscriptionTracker.UNSUBSCRIBE_FROM_ARTIFACT, unsubscribeRequest)
                }
            }
        }
    }
}