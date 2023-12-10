/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.marker.py

import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyFile
import spp.jetbrains.UserData
import spp.jetbrains.artifact.service.ArtifactModelService
import spp.jetbrains.artifact.service.ArtifactScopeService
import spp.jetbrains.artifact.service.ArtifactTypeService
import spp.jetbrains.doOnWorker
import spp.jetbrains.marker.LanguageProvider
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.marker.py.detect.PythonEndpointDetector
import spp.jetbrains.marker.py.detect.PythonLoggerDetector
import spp.jetbrains.marker.py.service.*
import spp.jetbrains.marker.service.*
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.info.EndpointDetector.AggregateEndpointDetector
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SynchronousSourceMarkEventListener
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark

/**
 * Provides Python support for the Marker API.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonLanguageProvider : LanguageProvider {

    override fun canSetup() = classExists("com.jetbrains.python.psi.PyElement")

    override fun setup(project: Project, setupDetectors: Boolean) {
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(PyFile::class.java)

        if (setupDetectors) {
            val endpointDetector = AggregateEndpointDetector(
                project,
                mutableListOf<EndpointDetector<*>>().apply {
                    getUltimateProvider(project)?.let { addAll(it.getEndpointDetectors(project)) }
                    add(PythonEndpointDetector(project))
                }
            )
            val loggerDetector = PythonLoggerDetector(project)

            SourceMarker.getInstance(project).addGlobalSourceMarkEventListener(SynchronousSourceMarkEventListener {
                if (it.eventCode == SourceMarkEventCode.MARK_BEFORE_ADDED) {
                    val mark = it.sourceMark
                    if (it.sourceMark.language.id != "Python") {
                        return@SynchronousSourceMarkEventListener
                    }

                    //setup endpoint detector and attempt detection
                    if (mark is GuideMark) {
                        mark.putUserData(SourceMarkerKeys.ENDPOINT_DETECTOR, endpointDetector)
                        UserData.vertx(project).doOnWorker { endpointDetector.getOrFindEndpointIds(mark) }
                    }

                    //setup logger detector
                    if (mark is InlayMark) {
                        //add logger detector to all inlay marks as live logs can be placed anywhere
                        mark.putUserData(SourceMarkerKeys.LOGGER_DETECTOR, loggerDetector)
                    }

                    //attempt to detect logger(s) on method guide marks
                    if (mark is MethodGuideMark) {
                        UserData.vertx(project).doOnWorker { loggerDetector.determineLoggerStatements(mark) }
                    }
                }
            })
        }

        ArtifactMarkService.addService(PythonArtifactMarkService(), "Python")
        ArtifactCreationService.addService(PythonArtifactCreationService(), "Python")
        ArtifactModelService.addService(PythonArtifactModelService(), "Python")
        ArtifactNamingService.addService(PythonArtifactNamingService(), "Python")
        ArtifactScopeService.addService(PythonArtifactScopeService(), "Python")
        ArtifactConditionService.addService(PythonArtifactConditionService(), "Python")
        ArtifactTypeService.addService(PythonArtifactTypeService(), "Python")
        SourceGuideProvider.addProvider(PythonGuideProvider(), "Python")
    }
}
