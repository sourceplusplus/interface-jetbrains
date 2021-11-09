package com.sourceplusplus.portal.display

import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.extensions.displayLog
import spp.protocol.ProtocolAddress.Global.ArtifactLogUpdated
import spp.protocol.ProtocolAddress.Global.ClickedDisplayLog
import spp.protocol.ProtocolAddress.Global.ClickedDisplayLogs
import spp.protocol.ProtocolAddress.Global.FetchMoreLogs
import spp.protocol.ProtocolAddress.Global.RefreshLogs
import spp.protocol.ProtocolAddress.Global.SetLogOrderType
import spp.protocol.ProtocolAddress.Portal.DisplayLogs
import spp.protocol.ProtocolAddress.Portal.RenderPage
import spp.protocol.artifact.log.Log
import spp.protocol.artifact.log.LogOrderType
import spp.protocol.artifact.log.LogResult
import spp.protocol.artifact.log.LogViewType
import spp.protocol.portal.PageType
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LogsDisplay(private val refreshIntervalMs: Int, private val pullMode: Boolean) : AbstractDisplay(PageType.LOGS) {

    companion object {
        private val log = LoggerFactory.getLogger(LogsDisplay::class.java)
    }

    override suspend fun start() {
        if (pullMode) {
            log.info("Log pull mode enabled")
            vertx.setPeriodic(refreshIntervalMs.toLong()) {
                SourcePortal.getPortals().filter {
                    it.configuration.currentPage == PageType.LOGS && (it.visible || it.configuration.external)
                }.forEach {
                    vertx.eventBus().send(RefreshLogs, it)
                }
            }
        } else {
            log.info("Log push mode enabled")
        }

        vertx.eventBus().consumer(SetLogOrderType, this@LogsDisplay::setLogOrderType)
        vertx.eventBus().consumer<LogResult>(ArtifactLogUpdated) { handleArtifactLogResult(it.body()) }
        vertx.eventBus().consumer(ClickedDisplayLog, this@LogsDisplay::clickedDisplayLog)
        vertx.eventBus().consumer(ClickedDisplayLogs, this@LogsDisplay::clickedDisplayLogs)
        vertx.eventBus().consumer(FetchMoreLogs, this@LogsDisplay::fetchMoreLogs)

        super.start()
    }

    override fun updateUI(portal: SourcePortal) {
        if (portal.configuration.currentPage != thisTab) {
            return
        }

        when (portal.logsView.viewType) {
            LogViewType.LIVE_TAIL -> {
                val logResult = portal.logsView.logResult
                if (logResult != null) {
                    vertx.eventBus().send(DisplayLogs(portal.portalUuid), logResult)
                    log.debug("Displayed logs for artifact. Log size: {}", logResult.logs.size)
                }
            }
            LogViewType.INDIVIDUAL_LOG -> {
                //do nothing
            }
        }
    }

    private fun fetchMoreLogs(messageHandler: Message<JsonObject>) {
        val portalUuid = messageHandler.body().getString("portalUuid")
        val pageNumber = messageHandler.body().getInteger("pageNumber")
        val portal = SourcePortal.getPortal(portalUuid)!!
        if (pageNumber == null) {
            portal.logsView.pageNumber++
            log.debug("Page number set to: ${portal.logsView.pageNumber}")
        } else {
            portal.logsView.pageNumber = pageNumber
            log.debug("Page number set to: ${portal.logsView.pageNumber}")
        }
        vertx.eventBus().send(RefreshLogs, portal)
    }

    private fun clickedDisplayLog(messageHandler: Message<JsonObject>) {
        val request = messageHandler.body() as JsonObject
        val portal = SourcePortal.getPortal(request.getString("portalUuid"))!!
        portal.logsView.viewType = LogViewType.INDIVIDUAL_LOG

        //todo: shouldn't need to deserialize/reserialize timestamp
        var log = Json.decodeValue(request.getJsonObject("log").toString(), Log::class.java)
        log = log.copy(
            timestamp = kotlinx.datetime.Instant.parse(
                request.getJsonObject("log").getJsonObject("timestamp").map.values.first().toString()
            )
        )
        vertx.eventBus().displayLog(portal.portalUuid, log)
    }

    private fun clickedDisplayLogs(messageHandler: Message<JsonObject>) {
        val request = messageHandler.body() as JsonObject
        val portal = SourcePortal.getPortal(request.getString("portalUuid"))!!
        portal.logsView.viewType = LogViewType.LIVE_TAIL

        SourcePortal.ensurePortalActive(portal)
        updateUI(portal)

        vertx.eventBus().send(RefreshLogs, portal)
    }

    private fun setLogOrderType(it: Message<JsonObject>) {
        log.info("Changed log order type")
        val message = JsonObject.mapFrom(it.body())
        val portalUuid = message.getString("portalUuid")
        val portal = SourcePortal.getPortal(portalUuid)
        if (portal == null) {
            log.warn("Ignoring logs tab opened event. Unable to find portal: {}", portalUuid)
            return
        }

        if (portal.configuration.currentPage != PageType.LOGS) {
            portal.configuration.currentPage = thisTab
            vertx.eventBus().send(RenderPage(portal.portalUuid), JsonObject.mapFrom(portal.configuration))
        }

        val orderType = message.getString("logOrderType")!!
        portal.logsView.orderType = LogOrderType.valueOf(orderType.toUpperCase())
        portal.logsView.viewType = LogViewType.LIVE_TAIL //updating order type implies live tail

        SourcePortal.ensurePortalActive(portal)
        updateUI(portal)

        vertx.eventBus().send(RefreshLogs, portal)
    }

    private fun handleArtifactLogResult(artifactLogResult: LogResult) {
        SourcePortal.getPortals().filter {
            it.viewingPortalArtifact == artifactLogResult.artifactQualifiedName!!
        }.forEach {
            it.logsView.cacheArtifactLogResult(artifactLogResult)
            updateUI(it)
        }
    }
}
