package com.sourceplusplus.plugin.intellij.marker.mark.gutter

import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJKeys
import com.sourceplusplus.portal.display.PortalInterface
import org.jetbrains.annotations.NotNull
import plus.sourceplus.marker.source.mark.gutter.GutterMark
import plus.sourceplus.marker.source.mark.gutter.component.jcef.GutterMarkJcefComponent
import plus.sourceplus.marker.source.mark.gutter.component.jcef.GutterMarkJcefComponentProvider

import java.awt.Dimension

/**
 * todo: description
 *
 * @version 0.2.4
 * @since 0.2.4
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJGutterMarkComponentProvider extends GutterMarkJcefComponentProvider {

    IntelliJGutterMarkComponentProvider() {
        defaultConfiguration.setComponentSize(new Dimension(775, 250))
        //todo: measure size of editor and make short if it will extend past IDE
        //defaultConfiguration.setComponentSize(new Dimension(620, 250))
    }

    @Override
    GutterMarkJcefComponent getComponent(@NotNull GutterMark gutterMark) {
        GutterMarkJcefComponent component = super.getComponent(gutterMark)
        def intellijGutterMark = gutterMark as IntelliJGutterMark
        intellijGutterMark.registerPortal()

        def portalUuid = intellijGutterMark.getUserData(IntelliJKeys.PortalUUID)
        component.configuration.initialUrl = "file:///" + PortalInterface.uiDirectory.absolutePath + "/tabs/overview.html?portal_uuid=$portalUuid"
        return component
    }
}
