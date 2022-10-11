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
package spp.jetbrains.marker.jvm

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import spp.jetbrains.UserData
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.jvm.detect.JVMEndpointDetector
import spp.jetbrains.marker.jvm.detect.JVMLoggerDetector
import spp.jetbrains.marker.jvm.service.*
import spp.jetbrains.marker.LanguageMarker
import spp.jetbrains.marker.source.SourceFileMarker.Companion.SUPPORTED_FILE_TYPES
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.safeLaunch
import spp.jetbrains.marker.SourceMarkerKeys.ENDPOINT_DETECTOR
import spp.jetbrains.marker.SourceMarkerKeys.LOGGER_DETECTOR
import spp.jetbrains.marker.service.*

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMLanguageMarker : LanguageMarker {

    override fun canSetup(): Boolean = true

    override fun setup(project: Project) {
        SUPPORTED_FILE_TYPES.add(GroovyFile::class.java)
        SUPPORTED_FILE_TYPES.add(PsiJavaFile::class.java)
        SUPPORTED_FILE_TYPES.add(KtFile::class.java)

        val endpointDetector = JVMEndpointDetector(project)
        val loggerDetector = JVMLoggerDetector(project)

        SourceMarker.getInstance(project).addGlobalSourceMarkEventListener {
            if (it.eventCode == SourceMarkEventCode.MARK_BEFORE_ADDED) {
                val mark = it.sourceMark
                if (!SourceMarkerUtils.getJvmLanguages().contains(it.sourceMark.language.id)) {
                    return@addGlobalSourceMarkEventListener //non-jvm language
                }

                //setup endpoint detector and attempt detection
                if (mark is GuideMark) {
                    mark.putUserData(ENDPOINT_DETECTOR, endpointDetector)
                    UserData.vertx(project).safeLaunch { endpointDetector.getOrFindEndpointId(mark) }
                }

                //setup logger detector
                if (mark is InlayMark) {
                    //add logger detector to all inlay marks as live logs can be placed anywhere
                    mark.putUserData(LOGGER_DETECTOR, loggerDetector)
                }

                //attempt to detect logger(s) on method guide marks
                if (mark is MethodGuideMark) {
                    UserData.vertx(project).safeLaunch { loggerDetector.determineLoggerStatements(mark) }
                }
            }
        }

        ArtifactMarkService.addService(JVMArtifactMarkService(), SourceMarkerUtils.getJvmLanguages())
        ArtifactCreationService.addService(JVMArtifactCreationService(), SourceMarkerUtils.getJvmLanguages())
        ArtifactNamingService.addService(JVMArtifactNamingService(), SourceMarkerUtils.getJvmLanguages())
        ArtifactScopeService.addService(JVMArtifactScopeService(), SourceMarkerUtils.getJvmLanguages())
        ArtifactConditionService.addService(JVMArtifactConditionService(), SourceMarkerUtils.getJvmLanguages())
        ArtifactTypeService.addService(JVMArtifactTypeService(), SourceMarkerUtils.getJvmLanguages())
        SourceGuideProvider.addProvider(JVMGuideProvider(), SourceMarkerUtils.getJvmLanguages())
    }
}
