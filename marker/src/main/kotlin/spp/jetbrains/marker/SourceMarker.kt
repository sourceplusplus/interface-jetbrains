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
package spp.jetbrains.marker

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import spp.jetbrains.marker.service.ArtifactNamingService
import spp.jetbrains.marker.service.SourceGuideProvider
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.status.SourceStatusService
import spp.protocol.artifact.ArtifactQualifiedName

/**
 * Holds a collection of [SourceFileMarker]s for a given [Project].
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarker(private val project: Project) {

    companion object {
        var PLUGIN_NAME = "SourceMarker"

        private val log = logger<SourceMarker>()
        private val KEY = Key.create<SourceMarker>("SPP_SOURCE_MARKER")

        @JvmStatic
        @Synchronized
        fun getInstance(project: Project): SourceMarker {
            if (project.getUserData(KEY) == null) {
                val sourceMarker = SourceMarker(project)
                project.putUserData(KEY, sourceMarker)
            }
            return project.getUserData(KEY)!!
        }

        fun getSourceFileMarker(psiFile: PsiFile): SourceFileMarker? {
            return getInstance(psiFile.project).getSourceFileMarker(psiFile)
        }
    }

    val configuration: SourceMarkerConfiguration = SourceMarkerConfiguration()
    private val availableSourceFileMarkers = Maps.newConcurrentMap<Int, SourceFileMarker>()
    private val globalSourceMarkEventListeners = Lists.newArrayList<SourceMarkEventListener>()

    fun getSourceFileMarkers(): List<SourceFileMarker> {
        return availableSourceFileMarkers.values.toList()
    }

    fun clearAvailableSourceFileMarkers() {
        availableSourceFileMarkers.forEach {
            deactivateSourceFileMarker(it.value)
        }
        availableSourceFileMarkers.clear()
    }

    fun deactivateSourceFileMarker(sourceFileMarker: SourceFileMarker): Boolean {
        if (availableSourceFileMarkers.remove(sourceFileMarker.hashCode()) != null) {
            sourceFileMarker.clearSourceMarks()
            sourceFileMarker.psiFile.putUserData(SourceFileMarker.KEY, null)
            log.info("Deactivated source file marker: $sourceFileMarker")
            return true
        }
        return false
    }

    fun getSourceFileMarkerIfExists(psiFile: PsiFile): SourceFileMarker? {
        return psiFile.getUserData(SourceFileMarker.KEY)
    }

    fun getSourceFileMarker(psiFile: PsiFile): SourceFileMarker? {
        var fileMarker = psiFile.getUserData(SourceFileMarker.KEY)
        if (fileMarker != null) {
            return fileMarker
        } else if (!SourceFileMarker.isFileSupported(psiFile)) {
            log.trace("File type not setup: ${psiFile.fileType.name}")
            return null
        } else if (psiFile.virtualFile == null || !psiFile.virtualFile.isInLocalFileSystem) {
            log.trace("Skipping in-memory/non-local file: $psiFile")
            return null
        } else if (!ApplicationManager.getApplication().isUnitTestMode) {
            if (!SourceStatusService.getInstance(project).isConnected()) {
                log.warn("Not connected, skipping source file marker creation for: $psiFile")
                return null
            }
        }

        fileMarker = configuration.sourceFileMarkerProvider.createSourceFileMarker(psiFile)
        availableSourceFileMarkers.putIfAbsent(psiFile.hashCode(), fileMarker)
        fileMarker = availableSourceFileMarkers[psiFile.hashCode()]!!
        psiFile.putUserData(SourceFileMarker.KEY, fileMarker)

        SourceGuideProvider.determineGuideMarks(fileMarker)
        return fileMarker
    }

    fun getSourceFileMarker(qualifiedClassNameOrFilename: String): SourceFileMarker? {
        return ArtifactNamingService.findPsiFile(project, qualifiedClassNameOrFilename)
            ?.let { getSourceFileMarker(it) }
    }

    fun addGlobalSourceMarkEventListener(sourceMarkEventListener: SourceMarkEventListener) {
        log.info("Adding global source mark event listener: $sourceMarkEventListener")
        globalSourceMarkEventListeners.add(sourceMarkEventListener)
    }

    fun removeGlobalSourceMarkEventListener(sourceMarkEventListener: SourceMarkEventListener) {
        log.info("Removing global source mark event listener: $sourceMarkEventListener")
        globalSourceMarkEventListeners.remove(sourceMarkEventListener)
    }

    fun getGlobalSourceMarkEventListeners(): List<SourceMarkEventListener> {
        return ImmutableList.copyOf(globalSourceMarkEventListeners)
    }

    fun clearGlobalSourceMarkEventListeners() {
        globalSourceMarkEventListeners.clear()
    }

    fun getSourceMark(artifactQualifiedName: ArtifactQualifiedName, type: SourceMark.Type): SourceMark? {
        availableSourceFileMarkers.values.forEach {
            val sourceMark = it.getSourceMark(artifactQualifiedName, type)
            if (sourceMark != null) {
                return sourceMark
            }
        }
        return null
    }

    fun getGuideMark(artifactQualifiedName: ArtifactQualifiedName): GuideMark? {
        return getSourceMark(artifactQualifiedName, SourceMark.Type.GUIDE) as GuideMark?
    }

    fun getSourceMarks(artifactQualifiedName: ArtifactQualifiedName): List<SourceMark> {
        availableSourceFileMarkers.values.forEach {
            val sourceMarks = it.getSourceMarks(artifactQualifiedName)
            if (sourceMarks.isNotEmpty()) {
                return sourceMarks
            }
        }
        return emptyList()
    }

    fun getSourceMarks(): List<SourceMark> {
        return availableSourceFileMarkers.values.flatMap { it.getSourceMarks() }
    }

    fun getSourceMark(id: String): SourceMark? {
        return getSourceMarks().find { it.id == id }
    }

    fun getInlayMarks(): List<InlayMark> {
        return getSourceMarks().filterIsInstance<InlayMark>()
    }

    fun getGutterMarks(): List<GutterMark> {
        return getSourceMarks().filterIsInstance<GutterMark>()
    }

    fun getGuideMarks(): List<GuideMark> {
        return getSourceMarks().filterIsInstance<GuideMark>()
    }

    fun findByInstrumentId(instrumentId: String): List<SourceMark> {
        return getSourceMarks().filter {
            it.getUserData(SourceMarkerKeys.INSTRUMENT_ID) == instrumentId
        }
    }

    fun findBySubscriptionId(subscriptionId: String): List<SourceMark> {
        return getSourceMarks().filter {
            it.getUserData(SourceMarkerKeys.VIEW_SUBSCRIPTION_ID) == subscriptionId
        }
    }
}
