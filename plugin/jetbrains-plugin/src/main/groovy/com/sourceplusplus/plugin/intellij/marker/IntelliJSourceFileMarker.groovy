package com.sourceplusplus.plugin.intellij.marker

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJSourceMark
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJGutterMark
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJMethodGutterMark
import groovy.util.logging.Slf4j
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.uast.UMethod
import plus.sourceplus.marker.SourceFileMarker
import plus.sourceplus.marker.source.mark.api.SourceMark

/**
 * todo: description
 *
 * @version 0.2.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class IntelliJSourceFileMarker extends SourceFileMarker {

    IntelliJSourceFileMarker(@NotNull PsiFile psiFile) {
        super(psiFile)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    List<IntelliJGutterMark> getRejectedSourceMarks() {
        return super.getRejectedSourceMarks() as List<IntelliJGutterMark>
    }

    /**
     * {@inheritDoc}
     */
    @Override
    List<IntelliJGutterMark> getSourceMarks() {
        return super.getSourceMarks() as List<IntelliJGutterMark>
    }

    /**
     * {@inheritDoc}
     */
    @Override
    IntelliJGutterMark getSourceMark(@Nullable String artifactQualifiedName) {
        return super.getSourceMark(artifactQualifiedName) as IntelliJGutterMark
    }

    /**
     * {@inheritDoc}
     */
    @Override
    IntelliJMethodGutterMark getMethodSourceMark(@NotNull PsiElement psiMethod) {
        return super.getMethodSourceMark(psiMethod) as IntelliJMethodGutterMark
    }

    @Override
    SourceMark createSourceMark(@NotNull UMethod psiMethod, @NotNull SourceMark.Type type) {
        if (type == SourceMark.Type.GUTTER) {
            def sourceMark = new IntelliJMethodGutterMark(this, psiMethod)
            PluginBootstrap.sourcePlugin.vertx.eventBus().publish(IntelliJSourceMark.SOURCE_MARK_CREATED, sourceMark)
            return sourceMark
        } else {
            throw new IllegalStateException("Unsupported mark type: " + type)
        }
    }
}