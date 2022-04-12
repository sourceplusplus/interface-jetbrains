package spp.jetbrains.sourcemarker.view

import com.intellij.openapi.application.ApplicationManager
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker.creationService
import spp.jetbrains.marker.jvm.psi.EndpointDetector.Companion.ENDPOINT_ID
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode.MARK_USER_DATA_UPDATED
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.bridge.EndpointMetricsBridge
import spp.jetbrains.monitor.skywalking.model.GetEndpointMetrics
import spp.jetbrains.monitor.skywalking.model.ZonedDuration
import spp.jetbrains.sourcemarker.SourceMarkerPlugin.vertx
import spp.jetbrains.sourcemarker.icons.SourceMarkerIcons
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.protocol.SourceServices
import spp.protocol.SourceServices.Provide.toLiveViewSubscriberAddress
import spp.protocol.instrument.LiveSourceLocation
import spp.protocol.view.LiveViewConfig
import spp.protocol.view.LiveViewEvent
import spp.protocol.view.LiveViewSubscription
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Adds a gutter icon to the editor indicating that the endpoint is currently failing.
 */
class FailingEndpointIndicator(val config: SourceMarkerConfig) : SourceMarkEventListener {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(FailingEndpointIndicator::class.java)
        private val FAILING_ENDPOINT_INDICATOR = SourceKey<Boolean>("FAILING_ENDPOINT_INDICATOR")
        private val GUTTER_MARK = SourceKey<GutterMark>("FAILING_ENDPOINT_INDICATOR_GUTTER_MARK")
        const val FAIL_PERCENT = 60.00 //todo: config through LCP
    }

    override fun handleEvent(event: SourceMarkEvent) {
        if (event.sourceMark !is GuideMark || event.sourceMark.getUserData(FAILING_ENDPOINT_INDICATOR) == true) return
        if (event.eventCode == MARK_USER_DATA_UPDATED && event.sourceMark.getUserData(ENDPOINT_ID) != null) {
            event.sourceMark.putUserData(FAILING_ENDPOINT_INDICATOR, true)
            val artifactQualifiedName = event.sourceMark.artifactQualifiedName
            log.info("Tracking endpoint failures for ${artifactQualifiedName.identifier}")

            //found endpoint, subscribe to SLA to track currently failing status
            SourceServices.Instance.liveView!!.addLiveViewSubscription(
                LiveViewSubscription(
                    entityIds = listOf(event.sourceMark.getUserData(ENDPOINT_ID)!!),
                    artifactQualifiedName = artifactQualifiedName,
                    artifactLocation = LiveSourceLocation(artifactQualifiedName.identifier, -1),
                    liveViewConfig = LiveViewConfig("FAILING_ENDPOINT_INDICATOR", listOf("endpoint_sla"), -1)
                )
            ).onComplete {
                if (it.succeeded()) {
                    val subscriptionId = it.result().subscriptionId!!
                    vertx.eventBus().consumer<JsonObject>(toLiveViewSubscriberAddress(subscriptionId)) {
                        val viewEvent = Json.decodeValue(it.body().toString(), LiveViewEvent::class.java)
                        val metricsData = JsonObject(viewEvent.metricsData)
                        val currentSLA = metricsData.getInteger("percentage").toDouble() / 100.0
                        if (currentSLA > FAIL_PERCENT) {
                            log.debug("Endpoint ${artifactQualifiedName.identifier} is not failing. SLA: $currentSLA%")
                            removeFailingIndicatorIfNecessary(event.sourceMark)
                        } else {
                            log.debug("Endpoint ${artifactQualifiedName.identifier} is currently failing. SLA: $currentSLA%")
                            addFailingIndicatorIfNecessary(event.sourceMark)
                        }
                    }
                    event.sourceMark.addEventListener {
                        if (it.eventCode == SourceMarkEventCode.MARK_REMOVED) {
                            SourceServices.Instance.liveView!!.removeLiveViewSubscription(subscriptionId)
                        }
                    }

                    //get initial SLA as endpoints that receive no traffic won't produce metric subscriptions
                    val endTime = ZonedDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.MINUTES) //exclusive
                    val startTime = endTime.minusMinutes(2)
                    val metricsRequest = GetEndpointMetrics(
                        listOf("endpoint_sla"),
                        event.sourceMark.getUserData(ENDPOINT_ID)!!,
                        ZonedDuration(startTime, endTime, SkywalkingClient.DurationStep.MINUTE)
                    )
                    GlobalScope.launch(vertx.dispatcher()) {
                        val metrics = EndpointMetricsBridge.getMetrics(metricsRequest, vertx)
                        if (metrics.flatMap { it.values }.filter { (it.value as Int) < FAIL_PERCENT * 100 }.size > 1) {
                            log.info("Endpoint ${artifactQualifiedName.identifier} is currently failing")
                            addFailingIndicatorIfNecessary(event.sourceMark)
                        }
                    }
                }
            }
        }
    }

    private fun addFailingIndicatorIfNecessary(sourceMark: SourceMark) {
        val existingGutterMark = sourceMark.getUserData(GUTTER_MARK)
        if (existingGutterMark == null) {
            log.info("Adding failing indicator gutter mark to ${sourceMark.artifactQualifiedName.identifier}")
            ApplicationManager.getApplication().runReadAction {
                val gutterMark = creationService.createMethodGutterMark(
                    sourceMark.sourceFileMarker,
                    (sourceMark as MethodSourceMark).getPsiElement().nameIdentifier!!,
                    false
                )
                gutterMark.configuration.activateOnMouseHover = false //todo: show tooltip with extra info
                gutterMark.configuration.icon = SourceMarkerIcons.exclamationTriangle
                gutterMark.apply(true)
                sourceMark.putUserData(GUTTER_MARK, gutterMark)
            }
        }
    }

    private fun removeFailingIndicatorIfNecessary(sourceMark: SourceMark) {
        val existingGutterMark = sourceMark.getUserData(GUTTER_MARK)
        if (existingGutterMark != null) {
            log.info("Removing failing indicator gutter mark from ${sourceMark.artifactQualifiedName.identifier}")
            existingGutterMark.dispose()
            sourceMark.putUserData(GUTTER_MARK, null)
        }
    }
}
