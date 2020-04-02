package com.sourceplusplus.plugin.intellij.marker.mark.gutter

import com.intellij.openapi.util.IconLoader
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJSourceMark
import com.sourceplusplus.marker.source.mark.gutter.GutterMark

import javax.swing.*

/**
 * todo: description
 *
 * @version 0.2.5
 * @since 0.2.5
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
interface IntelliJGutterMark extends GutterMark, IntelliJSourceMark {

    public static final Icon sppActive = IconLoader.getIcon("/icons/s++_active.svg", IntelliJGutterMark.class)
    public static final Icon sppInactive = IconLoader.getIcon("/icons/s++_inactive.svg", IntelliJGutterMark.class)

    boolean isPortalRegistered()

    String getPortalUuid()

    void registerPortal()
}
