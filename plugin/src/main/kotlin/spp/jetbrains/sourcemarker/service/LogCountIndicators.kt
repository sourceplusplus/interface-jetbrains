package spp.jetbrains.sourcemarker.service

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import io.vertx.core.json.Json
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarker.creationService
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.sourcemarker.icons.SourceMarkerIcons
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.LOGGER_DETECTOR
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.protocol.SourceMarkerServices.Instance
import spp.protocol.instrument.DurationStep

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
                val project = ProjectManager.getInstance().openProjects[0]
                val config = Json.decodeValue(
                    PropertiesComponent.getInstance(project).getValue("sourcemarker_plugin_config"),
                    SourceMarkerConfig::class.java
                )

                SourceMarker.getAvailableSourceFileMarkers().forEach { fileMarker ->
                    launch {
                        val fileLogPatterns = fileMarker.getSourceMarks().filterIsInstance<MethodSourceMark>().flatMap {
                            it.getUserData(LOGGER_DETECTOR)!!.getOrFindLoggerStatements(it)
                        }
                        Instance.logCountIndicator!!.getPatternOccurrences(
                            fileLogPatterns.map { it.logPattern },
                            config.serviceName,
                            Clock.System.now().minus(15, DateTimeUnit.MINUTE),
                            Clock.System.now(),
                            DurationStep.MINUTE
                        ) {
                            if (it.succeeded()) {
                                val occurrences = it.result()
                                //log.info("Found ${occurrences} occurrences of log patterns")

                                ApplicationManager.getApplication().runReadAction {
                                    fileLogPatterns.forEach { logger ->
                                        val sumValue = occurrences.getJsonObject(logger.logPattern)
                                            .getJsonArray("values").list.sumOf { it as Int? ?: 0 }

                                        val logIndicator = creationService.getOrCreateExpressionGutterMark(
                                            fileMarker,
                                            logger.lineLocation
                                        ).get()
                                        if (!fileMarker.containsSourceMark(logIndicator)) {
                                            logIndicator.configuration.icon =
                                                SourceMarkerIcons.getNumericGutterMarkIcon(
                                                    sumValue,
                                                    if (logger.level == "warn" || logger.level == "error") "#e1483b"
                                                    else "#182d34"
                                                )
                                            logIndicator.apply(true)
                                        } else {
                                            logIndicator.configuration.icon =
                                                SourceMarkerIcons.getNumericGutterMarkIcon(
                                                    sumValue,
                                                    if (logger.level == "warn" || logger.level == "error") "#e1483b"
                                                    else "#182d34"
                                                )
                                            //todo: should just be updating rendering, not all analysis
                                            fileMarker.refresh()
                                        }
                                    }
                                }
                            } else {
                                log.error("Failed to get log pattern occurrences", it.cause())
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
