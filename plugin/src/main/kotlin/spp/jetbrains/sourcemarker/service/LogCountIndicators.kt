/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
import spp.protocol.SourceServices.Instance
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
                        val occurrences = Instance.logCountIndicator!!.getPatternOccurrences(
                            fileLogPatterns.map { it.logPattern },
                            config.serviceName,
                            Clock.System.now().minus(15, DateTimeUnit.MINUTE),
                            Clock.System.now(),
                            DurationStep.MINUTE
                        ).onComplete {
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
                            }
                        }
                    }
                }
            }
        }
    }
}
