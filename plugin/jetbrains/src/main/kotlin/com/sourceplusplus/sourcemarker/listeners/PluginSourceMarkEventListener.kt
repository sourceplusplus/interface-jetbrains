package com.sourceplusplus.sourcemarker.listeners

import com.sourceplusplus.marker.source.mark.api.ClassSourceMark
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.protocol.portal.PageType
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin.vertx
import com.sourceplusplus.sourcemarker.mark.SourceMarkConstructor
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys.ENDPOINT_DETECTOR
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys.LOGGER_DETECTOR
import com.sourceplusplus.sourcemarker.psi.EndpointDetector
import com.sourceplusplus.sourcemarker.psi.LoggerDetector
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
        private val loggerDetector = LoggerDetector()
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
                sourcePortal.configuration.currentPage = PageType.OVERVIEW
                sourcePortal.configuration.visibleActivity = false
                sourcePortal.configuration.visibleTraces = false
                sourcePortal.configuration.artifactType = ArtifactType.CLASS
            } else {
                //method-based portals don't have overview page
                sourcePortal.configuration.visibleOverview = false
                sourcePortal.configuration.artifactType = ArtifactType.METHOD
            }

            if (sourceMark is MethodSourceMark) {
                //setup endpoint detector and attempt detection
                sourceMark.putUserData(ENDPOINT_DETECTOR, endpointDetector)
                GlobalScope.launch(vertx.dispatcher()) {
                    endpointDetector.getOrFindEndpointId(sourceMark)
                }

                //setup logger detector
                sourceMark.putUserData(LOGGER_DETECTOR, loggerDetector)
            }
        } else if (event.eventCode == SourceMarkEventCode.MARK_REMOVED) {
            event.sourceMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!.close()
            SourceMarkConstructor.tearDownSourceMark(event.sourceMark)
        }
    }
}
