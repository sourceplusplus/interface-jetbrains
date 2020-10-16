package com.sourceplusplus.sourcemarker.actions

import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.openapi.editor.Editor
import com.sourceplusplus.marker.source.mark.SourceMarkPopupAction
import com.sourceplusplus.marker.source.mark.api.ClassSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.component.jcef.SourceMarkJcefComponent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.protocol.portal.PageType
import com.sourceplusplus.sourcemarker.SourceMarkKeys.SOURCE_PORTAL
import org.slf4j.LoggerFactory
import javax.swing.UIManager

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PluginSourceMarkPopupAction : SourceMarkPopupAction() {

    companion object {
        private val log = LoggerFactory.getLogger(PluginSourceMarkPopupAction::class.java)
    }

    private var lastDisplayedInternalPortal: SourcePortal? = null

    override fun performPopupAction(sourceMark: SourceMark, editor: Editor) {
        if (sourceMark.getUserData(SOURCE_PORTAL) == null) {
            //register source portal
            val sourcePortal = SourcePortal.getPortal(
                //todo: appUuid/portalUuid
                SourcePortal.register("null", sourceMark.artifactQualifiedName, false)
            )
            sourceMark.putUserData(SOURCE_PORTAL, sourcePortal!!)
            if (sourceMark is ClassSourceMark) {
                //class-based portals start on overview page
                sourcePortal.currentTab = PageType.OVERVIEW
            }

            sourceMark.addEventListener { event ->
                if (event.eventCode == SourceMarkEventCode.MARK_REMOVED) {
                    event.sourceMark.getUserData(SOURCE_PORTAL)!!.close()
                }
            }
        }
        val sourcePortal = sourceMark.getUserData(SOURCE_PORTAL)!!

        refreshPortalIfNecessary(sourceMark, sourcePortal)
        super.performPopupAction(sourceMark, editor)
    }

    private fun refreshPortalIfNecessary(sourceMark: SourceMark, sourcePortal: SourcePortal) {
        val jcefComponent = sourceMark.sourceMarkComponent as SourceMarkJcefComponent
        if (sourcePortal != lastDisplayedInternalPortal) {
            val darkMode = UIManager.getLookAndFeel() !is IntelliJLaf
            val currentUrl = "/${sourcePortal.currentTab.name.toLowerCase()}.html" +
                    "?portalUuid=${sourcePortal.portalUuid}&dark_mode=$darkMode"
            jcefComponent.getBrowser().cefBrowser.executeJavaScript(
                "window.location.href = '$currentUrl';", currentUrl, 0
            )

            lastDisplayedInternalPortal = sourcePortal
        }
    }
}
