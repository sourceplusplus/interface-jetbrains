package com.sourceplusplus.plugin.intellij.source

import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiMethod
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJMethodGutterMark
import com.sourceplusplus.plugin.intellij.util.IntelliUtils
import com.sourceplusplus.plugin.marker.SourceFileMarker
import com.sourceplusplus.plugin.marker.mark.SourceMark
import com.sourceplusplus.plugin.source.SourceVisitor
import org.jetbrains.annotations.NotNull
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastContextKt

/**
 * todo: description
 *
 * @version 0.1.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class JavaSourceVisitor extends JavaRecursiveElementVisitor implements SourceVisitor {

    private final SourceFileMarker sourceFileMarker
    private final List<SourceMark> sourceMarks

    JavaSourceVisitor(@NotNull SourceFileMarker sourceFileMarker, @NotNull List<SourceMark> sourceMarks) {
        this.sourceFileMarker = sourceFileMarker
        this.sourceMarks = sourceMarks
    }

    @Override
    void visitMethod(PsiMethod method) {
        def uMethod = UastContextKt.toUElement(method) as UMethod
        def methodQualifiedName = IntelliUtils.getArtifactQualifiedName(uMethod)
        sourceFileMarker.sourceFile.addSourceMethod(SourceArtifact.builder()
                .artifactQualifiedName(methodQualifiedName).build())

        if (SourcePluginConfig.current.methodGutterMarksEnabled) {
            def gradeGutterRenderer = new IntelliJMethodGutterMark(sourceFileMarker,
                    sourceFileMarker.sourceFile.getSourceMethod(methodQualifiedName), uMethod)
            sourceMarks.add(gradeGutterRenderer)
        }
        super.visitMethod(method)
    }
}