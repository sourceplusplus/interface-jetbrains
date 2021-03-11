package com.sourceplusplus.sourcemarker.service

import com.intellij.openapi.application.ApplicationManager
import com.sourceplusplus.marker.source.SourceMarkerUtils
import com.sourceplusplus.protocol.ProtocolErrors
import com.sourceplusplus.protocol.SourceMarkerServices.Instance.Logging
import com.sourceplusplus.sourcemarker.mark.GutterMarkIcons
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys.LOGGER_DETECTOR
import com.sourceplusplus.sourcemarker.search.SourceMarkSearch
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LogCountIndicators : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(LogCountIndicators::class.java)
    }

    override suspend fun start() {
        vertx.setPeriodic(5000) {
            if (Logging.logCountIndicator != null) {
                Logging.logCountIndicator!!.getLogCountSummary {
                    if (it.succeeded()) {
                        GlobalScope.launch(vertx.dispatcher()) {
                            val logCounts = it.result().logCounts
                            logCounts.forEach { logSummary ->
                                val methodMark = SourceMarkSearch.findSourceMark(logSummary.key)
                                if (methodMark != null) {
                                    val loggers = methodMark.getUserData(LOGGER_DETECTOR)!!
                                        .getOrFindLoggerStatements(methodMark)
                                    val logger = loggers.find { it.logPattern == logSummary.key }!!

                                    ApplicationManager.getApplication().runReadAction {
                                        val logIndicator = SourceMarkerUtils.getOrCreateExpressionGutterMark(
                                            methodMark.sourceFileMarker,
                                            logger.lineLocation
                                        ).get()
                                        if (!methodMark.sourceFileMarker.containsSourceMark(logIndicator)) {
                                            logIndicator.configuration.icon =
                                                GutterMarkIcons.getNumericGutterMarkIcon(
                                                    logSummary.value,
                                                    if (logger.level == "warn" || logger.level == "error") "#e1483b" else "#182d34"
                                                )
                                            logIndicator.apply(true)
                                        } else {
                                            logIndicator.configuration.icon =
                                                GutterMarkIcons.getNumericGutterMarkIcon(
                                                    logSummary.value,
                                                    if (logger.level == "warn" || logger.level == "error") "#e1483b" else "#182d34"
                                                )
                                            methodMark.sourceFileMarker.refresh()
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val replyException = it.cause() as ReplyException
                        if (replyException.failureType() == ReplyFailure.TIMEOUT) {
                            log.warn("Timed out getting log count summary")
                        } else {
                            val rawFailure = JsonObject(it.cause().message)
                            val debugInfo = rawFailure.getJsonObject("debugInfo")
                            if (debugInfo.getString("type") == ProtocolErrors.ServiceUnavailable.name) {
                                log.warn("Unable to connect to service: " + debugInfo.getString("name"))
                            } else {
                                it.cause().printStackTrace()
                                log.error("Failed to get log count summary", it.cause())
                            }
                        }
                    }
                }
            }
        }
    }
}
