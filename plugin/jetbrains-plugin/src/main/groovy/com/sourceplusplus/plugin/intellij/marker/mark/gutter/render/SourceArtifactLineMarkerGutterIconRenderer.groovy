package com.sourceplusplus.plugin.intellij.marker.mark.gutter.render

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJMethodGutterMark

/**
 * todo: description
 *
 * @version 0.2.3
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class SourceArtifactLineMarkerGutterIconRenderer extends LineMarkerInfo.LineMarkerGutterIconRenderer {

    private final IntelliJMethodGutterMark gutterMark

    SourceArtifactLineMarkerGutterIconRenderer(IntelliJMethodGutterMark gutterMark, LineMarkerInfo info) {
        super(info)
        this.gutterMark = gutterMark
    }

    IntelliJMethodGutterMark getGutterMark() {
        return gutterMark
    }
}
