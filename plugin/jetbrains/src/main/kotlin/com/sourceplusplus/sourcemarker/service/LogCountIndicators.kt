package com.sourceplusplus.sourcemarker.service

import com.intellij.openapi.application.ApplicationManager
import com.sourceplusplus.marker.source.JVMMarkerUtils
import com.sourceplusplus.protocol.SourceMarkerServices.Instance
import com.sourceplusplus.protocol.error.AccessDenied
import com.sourceplusplus.sourcemarker.icons.SourceMarkerIcons
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys.LOGGER_DETECTOR
import com.sourceplusplus.sourcemarker.search.SourceMarkSearch
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure
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
        log.info("Log count indicators started")
        vertx.setPeriodic(5000) {
            if (Instance.logCountIndicator != null) {
                Instance.logCountIndicator!!.getLogCountSummary {
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
                                        val logIndicator = JVMMarkerUtils.getOrCreateExpressionGutterMark(
                                            methodMark.sourceFileMarker,
                                            logger.lineLocation
                                        ).get()
                                        if (!methodMark.sourceFileMarker.containsSourceMark(logIndicator)) {
                                            logIndicator.configuration.icon =
                                                SourceMarkerIcons.getNumericGutterMarkIcon(
                                                    logSummary.value,
                                                    if (logger.level == "warn" || logger.level == "error") "#e1483b"
                                                    else "#182d34"
                                                )
                                            logIndicator.apply(true)
                                        } else {
                                            logIndicator.configuration.icon =
                                                SourceMarkerIcons.getNumericGutterMarkIcon(
                                                    logSummary.value,
                                                    if (logger.level == "warn" || logger.level == "error") "#e1483b"
                                                    else "#182d34"
                                                )
                                            //todo: should just be updating rendering, not all analysis
                                            methodMark.sourceFileMarker.refresh()
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        //todo: use circuit breaker
                        val replyException = it.cause() as ReplyException
                        if (replyException.failureType() == ReplyFailure.TIMEOUT) {
                            log.warn("Timed out getting log count summary")
                        } else {
                            val actualException = replyException.cause!!
                            if (actualException is AccessDenied) {
                                log.error("Access denied. Reason: " + actualException.reason)
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

    override suspend fun stop() {
        log.info("Log count indicators stopped")
    }
}
