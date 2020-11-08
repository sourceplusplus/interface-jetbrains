package com.sourceplusplus.sourcemarker.listeners

import com.sourceplusplus.marker.source.mark.api.ClassSourceMark
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.model.PageType
import com.sourceplusplus.sourcemarker.mark.SourceMarkConstructor
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys.ENDPOINT_DETECTOR
import com.sourceplusplus.sourcemarker.psi.EndpointDetector
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PluginSourceMarkEventListener : SourceMarkEventListener {

    companion object {
        private val log = LoggerFactory.getLogger(PluginSourceMarkEventListener::class.java)
        private val endpointDetector = EndpointDetector()
    }

    override fun handleEvent(event: SourceMarkEvent) {
        if (event.eventCode == SourceMarkEventCode.PORTAL_OPENED) {
            val sourceMark = event.sourceMark
            val sourcePortal = sourceMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
            sourcePortal.visible = true
        } else if (event.eventCode == SourceMarkEventCode.PORTAL_CLOSED) {
            val sourceMark = event.sourceMark
            val sourcePortal = sourceMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
            sourcePortal.visible = false
        } else if (event.eventCode == SourceMarkEventCode.MARK_ADDED) {
            val sourceMark = event.sourceMark

            //register portal for source mark
            val sourcePortal = SourcePortal.getPortal(
                SourcePortal.register("null", sourceMark.artifactQualifiedName, false) //todo: appUuid
            )
            sourceMark.putUserData(SourceMarkKeys.SOURCE_PORTAL, sourcePortal!!)
            if (sourceMark is ClassSourceMark) {
                //class-based portals only have overview page
                sourcePortal.currentTab = PageType.OVERVIEW
                sourcePortal.configuration.visibleActivity = false
                sourcePortal.configuration.visibleTraces = false
            } else {
                //method-based portals don't have overview page
                sourcePortal.configuration.visibleOverview = false
            }

            if (sourceMark is MethodSourceMark) {
                sourceMark.putUserData(ENDPOINT_DETECTOR, endpointDetector)
            }
        } else if (event.eventCode == SourceMarkEventCode.MARK_REMOVED) {
            event.sourceMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!.close()
            SourceMarkConstructor.tearDownSourceMark(event.sourceMark)
        }
    }
}
