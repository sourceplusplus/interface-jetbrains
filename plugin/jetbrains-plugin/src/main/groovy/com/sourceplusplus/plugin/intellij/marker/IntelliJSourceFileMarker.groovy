package com.sourceplusplus.plugin.intellij.marker

import com.google.common.collect.Lists
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.sourceplusplus.plugin.PluginSourceFile
import com.sourceplusplus.plugin.intellij.inspect.IntelliJSourceMarkInspection
import com.sourceplusplus.plugin.marker.SourceFileMarker
import com.sourceplusplus.plugin.marker.mark.SourceMark
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJSourceFileMarker extends SourceFileMarker {

    public static final Key<IntelliJSourceFileMarker> KEY = Key.create("IntelliJSourceFileMarker")
    private static final Logger log = LoggerFactory.getLogger(this.name)
    private final PsiFile psiFile
    private final List<SourceMark> sourceMarks

    IntelliJSourceFileMarker(@NotNull PsiFile psiFile, @NotNull PluginSourceFile sourceFile) {
        super(sourceFile)
        this.psiFile = psiFile
        this.sourceMarks = Lists.newArrayList()
    }

    PsiFile getPsiFile() {
        return psiFile
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void refresh() {
        if (!psiFile.project.isDisposed()) {
            DaemonCodeAnalyzer.getInstance(psiFile.project).restart(psiFile)
        }
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    List<SourceMark> getSourceMarks() {
        return Lists.newArrayList(sourceMarks)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void clearSourceMarks() {
        sourceMarks.clear()
        refresh()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void setSourceMarks(@NotNull List<SourceMark> sourceMarks) {
        this.sourceMarks.clear()
        this.sourceMarks.addAll(sourceMarks)
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    List<SourceMark> createSourceMarks() {
        return ApplicationManager.getApplication().runReadAction(new IntelliJSourceMarkInspection(this, psiFile))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean removeSourceMark(@NotNull SourceMark sourceMark) {
        log.trace("Removing source mark for artifact: " + sourceMark.artifactQualifiedName)
        if (sourceMarks.remove(sourceMark)) {
            refresh()
            log.trace("Removed source mark for artifact: " + sourceMark.artifactQualifiedName)
            return true
        } else {
            return false
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean applySourceMark(@NotNull SourceMark sourceMark) {
        log.trace("Applying source mark for artifact: " + sourceMark.artifactQualifiedName)
        if (sourceMarks.add(sourceMark)) {
            refresh()
            log.trace("Applied source mark for artifact: " + sourceMark.artifactQualifiedName)
            return true
        } else {
            return false
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        IntelliJSourceFileMarker that = (IntelliJSourceFileMarker) o
        if (sourceFile != that.sourceFile) return false
        return true
    }

    /**
     * {@inheritDoc}
     */
    @Override
    int hashCode() {
        return sourceFile.hashCode()
    }
}
