package com.sourceplusplus.plugin.intellij.inspect

import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiFile
import com.sourceplusplus.plugin.intellij.source.GroovySourceVisitor
import com.sourceplusplus.plugin.intellij.source.JavaSourceVisitor
import com.sourceplusplus.plugin.intellij.source.KotlinSourceVisitor
import com.sourceplusplus.plugin.marker.SourceFileMarker
import com.sourceplusplus.plugin.marker.mark.SourceMark
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyFileImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Primary inspection. Computes SourceMarks for the given file.
 *
 * @version 0.2.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJSourceMarkInspection implements Computable<List<SourceMark>> {

    private static final Logger log = LoggerFactory.getLogger(this.name)

    private final SourceFileMarker sourceFileMarker
    private final PsiFile psiFile

    IntelliJSourceMarkInspection(@NotNull SourceFileMarker sourceFileMarker, @NotNull PsiFile psiFile) {
        this.sourceFileMarker = sourceFileMarker
        this.psiFile = psiFile
    }

    @NotNull
    @Override
    List<SourceMark> compute() {
        log.debug("Computing source marks for file: {}", psiFile)
        def sourceMarks = new ArrayList<>()
        def document = psiFile.getViewProvider().getDocument()
        def module = ModuleUtil.findModuleForPsiElement(psiFile)
        if (document == null || module == null) {
            return sourceMarks
        }

        if (psiFile instanceof KtFile) {
            psiFile.accept(new KotlinSourceVisitor(sourceFileMarker, sourceMarks))
        } else if (psiFile instanceof GroovyFileImpl) {
            psiFile.accept(new GroovySourceVisitor(sourceFileMarker, sourceMarks))
        } else {
            psiFile.accept(new JavaSourceVisitor(sourceFileMarker, sourceMarks))
        }
        log.debug("Found {} source marks for file: {}", sourceMarks.size(), psiFile)
        return sourceMarks
    }
}
