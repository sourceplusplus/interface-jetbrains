package com.sourceplusplus.plugin.intellij.marker.mark.gutter.render

import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJMethodGutterMark
import org.jetbrains.annotations.NotNull

import javax.swing.*

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class SourceArtifactGutterMarkRenderer extends GutterIconRenderer {

    static final Icon inactiveIcon = IconLoader.getIcon("/icons/s++_inactive.png", IntelliJMethodGutterMark.class)
    static final Icon activeIcon = IconLoader.getIcon("/icons/s++_active.png", IntelliJMethodGutterMark.class)
    private IntelliJMethodGutterMark gutterMark

    SourceArtifactGutterMarkRenderer(IntelliJMethodGutterMark gutterMark) {
        this.gutterMark = Objects.requireNonNull(gutterMark)
    }

    int getLineNumber() {
        return gutterMark.lineNumber
    }

    @NotNull
    IntelliJMethodGutterMark getGutterMark() {
        return gutterMark
    }

    @NotNull
    @Override
    Icon getIcon() {
        if (gutterMark.artifactDataAvailable) {
            return activeIcon
        } else {
            return inactiveIcon
        }
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        SourceArtifactGutterMarkRenderer that = (SourceArtifactGutterMarkRenderer) o
        if (gutterMark != that.gutterMark) return false
        return true
    }

    int hashCode() {
        return (gutterMark != null ? gutterMark.hashCode() : 0)
    }
}
