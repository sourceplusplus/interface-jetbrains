package com.sourceplusplus.plugin.marker.mark

import com.sourceplusplus.plugin.marker.SourceFileMarker
import org.jetbrains.annotations.NotNull

import java.util.concurrent.atomic.AtomicBoolean

/**
 * todo: description
 *
 * @version 0.1.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
abstract class GutterMark extends SourceMark {

    final AtomicBoolean showingTooltipWindow = new AtomicBoolean()

    GutterMark(@NotNull SourceFileMarker sourceFileMarker) {
        super(sourceFileMarker)
    }

    abstract int getLineNumber()
}
