package com.sourceplusplus.plugin.intellij.marker.mark.gutter

import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJSourceMark
import plus.sourceplus.marker.source.mark.gutter.GutterMark

/**
 * todo: description
 *
 * @version 0.2.5
 * @since 0.2.5
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
interface IntelliJGutterMark extends GutterMark, IntelliJSourceMark {

    boolean isPortalRegistered()

    String getPortalUuid()

    void registerPortal()
}
