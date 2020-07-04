package com.sourceplusplus.plugin.intellij.marker.mark.gutter

import com.intellij.openapi.util.IconLoader
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJSourceMark
import com.sourceplusplus.marker.source.mark.gutter.GutterMark
import com.sourceplusplus.plugin.intellij.portal.IntelliJSourcePortal

import javax.swing.*

/**
 * Extension of the GutterMark for handling IntelliJ.
 *
 * @version 0.3.0
 * @since 0.2.5
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
interface IntelliJGutterMark extends GutterMark, IntelliJSourceMark {

    static final Icon activeEntryMethod = IconLoader.getIcon("/icons/entry_method/active_entry_method.svg", IntelliJGutterMark.class)
    static final Icon inactiveEntryMethod = IconLoader.getIcon("/icons/entry_method/inactive_entry_method.svg", IntelliJGutterMark.class)
    static final Icon arrowToLeft = IconLoader.getIcon("/icons/trace_navigation/trace_navigation.svg", IntelliJGutterMark.class)
    static final Icon failingMethod = IconLoader.getIcon("/icons/failing_method/failing_method.svg", IntelliJGutterMark.class)
    static final Icon failingLine = IconLoader.getIcon("/icons/failing_method/failing_line.svg", IntelliJGutterMark.class)

    boolean isPortalRegistered()

    String getPortalUuid()

    void registerPortal()

    IntelliJSourcePortal getPortal()

    Icon determineMostSuitableIcon()
}
