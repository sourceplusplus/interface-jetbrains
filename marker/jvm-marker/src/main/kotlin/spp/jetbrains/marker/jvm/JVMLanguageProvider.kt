/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
import spp.jetbrains.artifact.service.ArtifactModelService
import spp.jetbrains.artifact.service.ArtifactScopeService
import spp.jetbrains.artifact.service.ArtifactTypeService
import spp.jetbrains.doOnWorker
import spp.jetbrains.marker.LanguageProvider
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarkerKeys.ENDPOINT_DETECTOR
import spp.jetbrains.marker.SourceMarkerKeys.LOGGER_DETECTOR
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.jvm.detect.JVMEndpointDetector
import spp.jetbrains.marker.jvm.detect.JVMLoggerDetector
import spp.jetbrains.marker.jvm.service.*
import spp.jetbrains.marker.service.*
import spp.jetbrains.marker.source.SourceFileMarker.Companion.SUPPORTED_FILE_TYPES
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.info.EndpointDetector.AggregateEndpointDetector
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SynchronousSourceMarkEventListener
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark

/**
 * Provides JVM support for the Marker API.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMLanguageProvider : LanguageProvider {

    override fun canSetup() = classExists("com.intellij.psi.PsiJavaFile")

    override fun setup(project: Project, setupDetectors: Boolean) {
        SUPPORTED_FILE_TYPES.add(PsiJavaFile::class.java)
        if (classExists("org.jetbrains.plugins.groovy.lang.psi.GroovyFile")) {
            SUPPORTED_FILE_TYPES.add(GroovyFile::class.java)
        }
        if (classExists("org.jetbrains.kotlin.psi.KtFile")) {
            SUPPORTED_FILE_TYPES.add(KtFile::class.java)
        }

        if (setupDetectors) {
            val endpointDetector = AggregateEndpointDetector(
                project,
                mutableListOf<EndpointDetector<*>>().apply {
                    getUltimateProvider(project)?.let { addAll(it.getEndpointDetectors(project)) }
                    add(JVMEndpointDetector(project))
                }
            )
            val loggerDetector = JVMLoggerDetector(project)

            SourceMarker.getInstance(project).addGlobalSourceMarkEventListener(SynchronousSourceMarkEventListener {
                if (it.eventCode == SourceMarkEventCode.MARK_BEFORE_ADDED) {
                    val mark = it.sourceMark
                    if (!SourceMarkerUtils.isJvm(it.sourceMark.language)) {
                        return@SynchronousSourceMarkEventListener //non-jvm language
                    }

                    //setup endpoint detector and attempt detection
                    if (mark is GuideMark) {
                        mark.putUserData(ENDPOINT_DETECTOR, endpointDetector)
                        UserData.vertx(project).doOnWorker { endpointDetector.getOrFindEndpointIds(mark) }
                    }

                    //setup logger detector
                    if (mark is InlayMark) {
                        //add logger detector to all inlay marks as live logs can be placed anywhere
                        mark.putUserData(LOGGER_DETECTOR, loggerDetector)
                    }

                    //attempt to detect logger(s) on method guide marks
                    if (mark is MethodGuideMark) {
                        UserData.vertx(project).doOnWorker { loggerDetector.determineLoggerStatements(mark) }
                    }
                }
            })
        }

        SourceMarkerUtils.getJvmLanguages().let {
            ArtifactMarkService.addService(JVMArtifactMarkService(), it)
            ArtifactCreationService.addService(JVMArtifactCreationService(), it)
            ArtifactModelService.addService(JVMArtifactModelService(), it)
            ArtifactNamingService.addService(JVMArtifactNamingService(), it)
            ArtifactScopeService.addService(JVMArtifactScopeService(), it)
            ArtifactConditionService.addService(JVMArtifactConditionService(), it)
            ArtifactTypeService.addService(JVMArtifactTypeService(), it)
            SourceGuideProvider.addProvider(JVMGuideProvider(), it)
        }
    }
}
