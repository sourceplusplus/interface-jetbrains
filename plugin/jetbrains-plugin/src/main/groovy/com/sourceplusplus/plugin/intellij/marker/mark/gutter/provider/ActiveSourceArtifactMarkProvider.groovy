package com.sourceplusplus.plugin.intellij.marker.mark.gutter.provider

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.*
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.intellij.marker.IntelliJSourceFileMarker
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJMethodGutterMark
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.render.SourceArtifactGutterMarkRenderer
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.render.SourceArtifactLineMarkerGutterIconRenderer
import com.sourceplusplus.plugin.intellij.util.IntelliUtils
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastContextKt
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*

/**
 * Provides the visual rendering for 'Active' source marks located in the IDE gutter.
 *
 * @version 0.2.3
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class ActiveSourceArtifactMarkProvider extends LineMarkerProviderDescriptor {

    private static final Logger log = LoggerFactory.getLogger(ActiveSourceArtifactMarkProvider.name)

    @Override
    LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        return determineLineMarkerInfo(element)
    }

    @Override
    void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
    }

    @Override
    String getName() {
        return "Active Source++ Artifact"
    }

    @Override
    Icon getIcon() {
        return SourceArtifactGutterMarkRenderer.activeIcon
    }

    private LineMarkerInfo<PsiElement> determineLineMarkerInfo(PsiElement element) {
        def activeMarkers = PluginBootstrap.sourcePlugin?.availableSourceFileMarkers
        if (activeMarkers == null) return null
        def owner = getAnnotationOwner(element)
        if (owner == null) return null
        def uElement = UastContextKt.toUElement(element)

        def activeMarker = activeMarkers.find {
            ((IntelliJSourceFileMarker) it).psiFile == element.containingFile
        } as IntelliJSourceFileMarker
        if (activeMarker != null) {
            def artifactQualifiedName = IntelliUtils.getArtifactQualifiedName(uElement.uastParent as UMethod)
            def sourceMark = activeMarker.sourceMarks.find {
                it.artifactQualifiedName == artifactQualifiedName
            } as IntelliJMethodGutterMark

            if (sourceMark != null && sourceMark.artifactDataAvailable) {
                log.debug("Created active source mark: " + sourceMark)
                return new LineMarkerInfo<PsiElement>(element, element.textRange,
                        sourceMark.gutterMarkRenderer.icon, Pass.LINE_MARKERS,
                        null, null,
                        GutterIconRenderer.Alignment.CENTER) {
                    @Override
                    GutterIconRenderer createGutterRenderer() {
                        return new SourceArtifactLineMarkerGutterIconRenderer(sourceMark, this)
                    }
                }
            }
        }
        return null
    }

    static PsiElement getAnnotationOwner(PsiElement element) {
        if (element == null) return null
        def uElement = UastContextKt.toUElement(element)
        if (uElement == null) return null
        def owner = element.getParent()
        def uOwner = uElement.uastParent
        if (uOwner == null || !(uOwner instanceof UMethod)) return null

        if ((!(owner instanceof PsiModifierListOwner) || !(owner instanceof PsiNameIdentifierOwner))
                && !(owner instanceof KtNamedFunction)) return null
        if (owner instanceof PsiParameter || owner instanceof PsiLocalVariable) return null

        // support non-Java languages where getNameIdentifier may return non-physical psi with the same range
        PsiElement nameIdentifier = ((PsiNameIdentifierOwner) owner).getNameIdentifier()
        if (nameIdentifier == null || !nameIdentifier.getTextRange().equals(element.getTextRange())) return null
        return owner
    }
}
