package com.sourceplusplus.plugin.marker.mark

import com.sourceplusplus.plugin.marker.SourceFileMarker
import com.sourceplusplus.portal.SourcePortal
import org.jetbrains.annotations.NotNull

import java.util.concurrent.atomic.AtomicBoolean

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
abstract class GutterMark extends SourceMark {

    final AtomicBoolean showingPortalWindow = new AtomicBoolean()
    final int portalId

    GutterMark(@NotNull SourceFileMarker sourceFileMarker) {
        super(sourceFileMarker)
        this.portalId = SourcePortal.registerPortalId(sourceFileMarker.sourceFile.appUuid)
    }

    int getPortalId() {
        return portalId
    }

    abstract int getLineNumber()
}
