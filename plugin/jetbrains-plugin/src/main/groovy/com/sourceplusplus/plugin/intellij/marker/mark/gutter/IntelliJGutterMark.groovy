package com.sourceplusplus.plugin.intellij.marker.mark.gutter

import com.intellij.openapi.util.IconLoader
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJSourceMark
import com.sourceplusplus.marker.source.mark.gutter.GutterMark

import javax.swing.*

/**
 * Extension of the GutterMark for handling IntelliJ.
 *
 * @version 0.2.6
 * @since 0.2.5
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
interface IntelliJGutterMark extends GutterMark, IntelliJSourceMark {

    public static final Icon sppActive = IconLoader.getIcon("/icons/s++_active.svg", IntelliJGutterMark.class)
    public static final Icon sppInactive = IconLoader.getIcon("/icons/s++_inactive.svg", IntelliJGutterMark.class)
    public static final Icon arrowToLeft = IconLoader.getIcon("/icons/s++_trace_navigation.svg", IntelliJGutterMark.class)

    boolean isPortalRegistered()

    String getPortalUuid()

    void registerPortal()
}
