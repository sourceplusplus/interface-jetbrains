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
package spp.jetbrains.marker.js

import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.project.Project
import spp.jetbrains.UserData
import spp.jetbrains.marker.LanguageProvider
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.js.detect.JavascriptEndpointDetector
import spp.jetbrains.marker.js.detect.JavascriptLoggerDetector
import spp.jetbrains.marker.js.service.*
import spp.jetbrains.marker.service.*
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.info.EndpointDetector.AggregateEndpointDetector
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SynchronousSourceMarkEventListener
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.safeLaunch

/**
 * Provides JavaScript support for the Marker API.
 *
 * @since 0.7.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JavascriptLanguageProvider : LanguageProvider {

    override fun canSetup(): Boolean {
        return try {
            Class.forName("com.intellij.lang.javascript.psi.impl.JSElementImpl")
            true
        } catch (ignore: ClassNotFoundException) {
            false
        }
    }

    override fun setup(project: Project) {
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(JSFile::class.java)

        val endpointDetector = AggregateEndpointDetector(
            project,
            mutableListOf<EndpointDetector<*>>().apply {
                addAll(getUltimateProvider(project).getEndpointDetectors(project))
                add(JavascriptEndpointDetector(project))
            }
        )
        val loggerDetector = JavascriptLoggerDetector(project)

        SourceMarker.getInstance(project).addGlobalSourceMarkEventListener(SynchronousSourceMarkEventListener {
            if (it.eventCode == SourceMarkEventCode.MARK_BEFORE_ADDED) {
                val mark = it.sourceMark
                if (!SourceMarkerUtils.getJavaScriptLanguages().contains(it.sourceMark.language.id)) {
                    return@SynchronousSourceMarkEventListener //non-javascript language
                }

                //setup endpoint detector and attempt detection
                if (mark is GuideMark) {
                    mark.putUserData(SourceMarkerKeys.ENDPOINT_DETECTOR, endpointDetector)
                    UserData.vertx(project).safeLaunch { endpointDetector.getOrFindEndpointId(mark) }
                }

                //setup logger detector
                if (mark is InlayMark) {
                    //add logger detector to all inlay marks as live logs can be placed anywhere
                    mark.putUserData(SourceMarkerKeys.LOGGER_DETECTOR, loggerDetector)
                }

                //attempt to detect logger(s) on method guide marks
                if (mark is MethodGuideMark) {
                    UserData.vertx(project).safeLaunch { loggerDetector.determineLoggerStatements(mark) }
                }
            }
        })

        ArtifactMarkService.addService(JavascriptArtifactMarkService(), SourceMarkerUtils.getJavaScriptLanguages())
        ArtifactCreationService.addService(JavascriptArtifactCreationService(), SourceMarkerUtils.getJavaScriptLanguages())
        ArtifactNamingService.addService(JavascriptArtifactNamingService(),  SourceMarkerUtils.getJavaScriptLanguages())
        ArtifactScopeService.addService(JavascriptArtifactScopeService(),  SourceMarkerUtils.getJavaScriptLanguages())
        ArtifactConditionService.addService(JavascriptArtifactConditionService(),  SourceMarkerUtils.getJavaScriptLanguages())
        ArtifactTypeService.addService(JavascriptArtifactTypeService(),  SourceMarkerUtils.getJavaScriptLanguages())
        SourceGuideProvider.addProvider(JavascriptGuideProvider(),  SourceMarkerUtils.getJavaScriptLanguages())
    }
}
