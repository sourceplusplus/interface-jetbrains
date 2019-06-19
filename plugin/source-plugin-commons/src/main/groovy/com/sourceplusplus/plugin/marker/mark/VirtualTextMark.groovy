package com.sourceplusplus.plugin.marker.mark

import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.plugin.PluginSourceFile
import com.sourceplusplus.plugin.marker.SourceFileMarker
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
abstract class VirtualTextMark extends SourceMark {

    private final String virtualText

    VirtualTextMark(@NotNull SourceFileMarker sourceFileMarker, @NotNull String virtualText) {
        super(sourceFileMarker)
        this.virtualText = virtualText
    }

    @NotNull
    abstract VirtualTextMark update(@NotNull String virtualText)

    @NotNull
    String getVirtualText() {
        return virtualText
    }

    @NotNull
    @Override
    PluginSourceFile getSourceFile() {
        return null
    }

    @Nullable
    @Override
    SourceArtifact getSourceMethod() {
        return null
    }

    @Override
    boolean isClassMark() {
        return false
    }

    @Override
    boolean isMethodMark() {
        return false
    }
}
