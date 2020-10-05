package com.sourceplusplus.sourcemarker.actions

import com.intellij.openapi.editor.Editor
import com.sourceplusplus.marker.source.mark.SourceMarkPopupAction
import com.sourceplusplus.marker.source.mark.api.ClassSourceMark
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.component.jcef.SourceMarkJcefComponent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.sourcemarker.SourceMarkKeys.ENDPOINT_DETECTOR
import com.sourceplusplus.sourcemarker.SourceMarkKeys.ENDPOINT_ID
import com.sourceplusplus.sourcemarker.SourceMarkKeys.SOURCE_PORTAL
import com.sourceplusplus.sourcemarker.activities.PluginSourceMarkerStartupActivity.Companion.vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

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
        //register source portal (if necessary)
        if (sourceMark.getUserData(SOURCE_PORTAL) == null) {
            val sourcePortal = SourcePortal.getPortal(
                //todo: appUuid/portalUuid
                SourcePortal.register("null", "null", sourceMark.artifactQualifiedName, false)
            )
            sourceMark.putUserData(SOURCE_PORTAL, sourcePortal)

            sourceMark.addEventListener { event ->
                if (event.eventCode == SourceMarkEventCode.MARK_REMOVED) {
                    event.sourceMark.getUserData(SOURCE_PORTAL)!!.close()
                }
            }
        }
        val sourcePortal = sourceMark.getUserData(SOURCE_PORTAL)!!

        //todo: determine sourceportal context
        when (sourceMark) {
            is ClassSourceMark -> GlobalScope.launch(vertx.dispatcher()) { performClassPopup(sourceMark) }
            is MethodSourceMark -> GlobalScope.launch(vertx.dispatcher()) { performMethodPopup(sourceMark) }
        }

        //todo: use SourcePortalAPI to ensure correct view is showing (don't refresh if correct already viewing)
        //todo: likely need to unregister old portal handlers
        val jcefComponent = sourceMark.sourceMarkComponent as SourceMarkJcefComponent
        if (sourcePortal != lastDisplayedInternalPortal) {
            jcefComponent.getBrowser().cefBrowser.executeJavaScript(
                """
                    window.location.href = 'http://localhost:8080/overview?portal_uuid=${sourcePortal.portalUuid}';
                """.trimIndent(),
                "", 0
            )
        }

        lastDisplayedInternalPortal = sourcePortal
        super.performPopupAction(sourceMark, editor)
    }

    private suspend fun performClassPopup(sourceMark: ClassSourceMark) {
        //todo: get all endpoint keys for current file
        val endpointIds = sourceMark.sourceFileMarker.getSourceMarks()
            .filterIsInstance<MethodSourceMark>()
            .filter { it.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointId(it) != null }
            .map { it.getUserData(ENDPOINT_ID)!! }
        println("Endpoint ids: $endpointIds")

        //todo: disable traces page, disable overview page
    }

    private suspend fun performMethodPopup(sourceMark: MethodSourceMark) {
        val endpointId = sourceMark.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointId(sourceMark)
        println("Endpoint id: $endpointId")
    }
}
