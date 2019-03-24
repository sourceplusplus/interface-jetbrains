package com.sourceplusplus.plugin.marker

import com.sourceplusplus.plugin.PluginSourceFile
import com.sourceplusplus.plugin.marker.mark.SourceMark
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * Used to mark a source code file with Source++ artifact marks.
 * Source++ artifact marks can be used to subscribe to and collect source code runtime information.
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
abstract class SourceFileMarker {

    private PluginSourceFile sourceFile

    SourceFileMarker(@NotNull PluginSourceFile sourceFile) {
        this.sourceFile = sourceFile
    }

    @NotNull
    PluginSourceFile getSourceFile() {
        return sourceFile
    }

    abstract void refresh()

    /**
     * Gets the {@link SourceMark}s recognized in the current source code file.
     * @return a list of the {@link SourceMark}s
     */
    @NotNull
    abstract List<SourceMark> getSourceMarks()

    abstract void clearSourceMarks()

    abstract void setSourceMarks(@NotNull List<SourceMark> sourceMarks)

    @NotNull
    abstract List<SourceMark> createSourceMarks()

    abstract boolean removeSourceMark(@NotNull SourceMark sourceMark)

    abstract boolean applySourceMark(@NotNull SourceMark sourceMark)

    @Nullable
    SourceMark getSourceMark(String artifactQualifiedName) {
        return getSourceMarks().find { it.artifactQualifiedName == artifactQualifiedName }
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    String toString() {
        return sourceFile.qualifiedClassName
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        SourceFileMarker that = (SourceFileMarker) o
        if (sourceFile != that.sourceFile) return false
        return true
    }

    /**
     * {@inheritDoc}
     */
    @Override
    int hashCode() {
        return (sourceFile != null ? sourceFile.hashCode() : 0)
    }
}
