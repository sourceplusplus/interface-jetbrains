/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.sourcemarker.mark

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import io.vertx.core.Vertx
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.js.detect.JavascriptEndpointDetector
import spp.jetbrains.marker.js.detect.JavascriptLoggerDetector
import spp.jetbrains.marker.jvm.detect.JVMEndpointDetector
import spp.jetbrains.marker.jvm.detect.JVMLoggerDetector
import spp.jetbrains.marker.py.detect.PythonEndpointDetector
import spp.jetbrains.marker.py.detect.PythonLoggerDetector
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.info.LoggerDetector
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SynchronousSourceMarkEventListener
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.monitor.skywalking.bridge.ServiceBridge
import spp.jetbrains.safeLaunch
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.ENDPOINT_DETECTOR
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.LOGGER_DETECTOR

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PluginSourceMarkEventListener(val project: Project, val vertx: Vertx) : SynchronousSourceMarkEventListener {

    companion object {
        private val log = logger<PluginSourceMarkEventListener>()
    }

    private val endpointDetectors = mutableMapOf<String, EndpointDetector<*>>()
        .apply {
            JVMEndpointDetector(project).let {
                put("JAVA", it)
                put("kotlin", it)
                put("Scala", it)
                put("Groovy", it)
            }
            put("Python", PythonEndpointDetector(project))
            put("ECMAScript 6", JavascriptEndpointDetector(project))
        }.toMap()
    private val loggerDetectors = mutableMapOf<String, LoggerDetector>()
        .apply {
            JVMLoggerDetector(project).let {
                put("JAVA", it)
                put("kotlin", it)
                put("Scala", it)
                put("Groovy", it)
            }
            put("Python", PythonLoggerDetector(project))
            put("ECMAScript 6", JavascriptLoggerDetector(project))
        }.toMap()

    init {
        //refresh source marks on service changes
        ServiceBridge.currentServiceConsumer(vertx).handler {
            vertx.safeLaunch {
                SourceMarker.getInstance(project).clearAvailableSourceFileMarkers()
                FileEditorManager.getInstance(project).allEditors.forEach {
                    ApplicationManager.getApplication().runReadAction {
                        PsiManager.getInstance(project).findFile(it.file)?.let {
                            SourceMarker.getSourceFileMarker(it)
                            DaemonCodeAnalyzer.getInstance(project).restart(it)
                        }
                    }
                }
            }
        }
    }

    override fun handleEvent(event: SourceMarkEvent) {
        log.trace { "Handling event: $event" }

        if (event.eventCode == SourceMarkEventCode.MARK_BEFORE_ADDED) {
            val sourceMark = event.sourceMark

            //setup endpoint detector and attempt detection
            if (sourceMark is GuideMark) {
                val endpointDetector = endpointDetectors[sourceMark.language.id]
                if (endpointDetector != null) {
                    sourceMark.putUserData(ENDPOINT_DETECTOR, endpointDetector)
                    vertx.safeLaunch { endpointDetector.getOrFindEndpointId(sourceMark) }
                }
            }

            //setup logger detector
            val loggerDetector = loggerDetectors[sourceMark.language.id]
            if (loggerDetector != null) {
                if (sourceMark is InlayMark) {
                    //add logger detector to all inlay marks as live logs can be placed anywhere
                    sourceMark.putUserData(LOGGER_DETECTOR, loggerDetector)
                }

                //attempt to detect logger(s) on method guide marks
                if (sourceMark is MethodGuideMark) {
                    vertx.safeLaunch { loggerDetector.determineLoggerStatements(sourceMark) }
                }
            }
        }
    }
}
