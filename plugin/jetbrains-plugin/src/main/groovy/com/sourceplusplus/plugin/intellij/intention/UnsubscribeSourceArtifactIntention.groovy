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
import com.sourceplusplus.plugin.intellij.util.IntelliUtils
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastContextKt

import static com.sourceplusplus.plugin.PluginBootstrap.*

/**
 * Intention used to unsubscribe from source code artifacts.
 * Artifacts currently supported:
 *  - methods
 *
 * @version 0.2.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class UnsubscribeSourceArtifactIntention extends PsiElementBaseIntentionAction {

    @NotNull
    String getText() {
        return "Unsubscribe from source artifact"
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
                if (sourceMark != null && sourceMark.artifactSubscribed) {
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

        //unsubscribe from artifact
        def unsubscribeRequest = SourceArtifactUnsubscribeRequest.builder()
                .appUuid(SourcePluginConfig.current.activeEnvironment.appUuid)
                .artifactQualifiedName(artifactQualifiedName)
                .removeAllArtifactSubscriptions(true)
                .build()
        sourcePlugin.vertx.eventBus().send(
                PluginArtifactSubscriptionTracker.UNSUBSCRIBE_FROM_ARTIFACT, unsubscribeRequest)
    }
}