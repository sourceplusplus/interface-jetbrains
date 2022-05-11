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
package spp.jetbrains.marker

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.intellij.psi.PsiFile
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.extend.CommandCenter
import spp.jetbrains.marker.plugin.SourceGuideProvider
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.protocol.artifact.ArtifactNameUtils
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions")
object SourceMarker {

    var PLUGIN_NAME = "SourceMarker"

    @Volatile
    var enabled = true
    val configuration: SourceMarkerConfiguration = SourceMarkerConfiguration()
    lateinit var commandCenter: CommandCenter
    lateinit var guideProvider: SourceGuideProvider
    lateinit var namingService: ArtifactNamingService
    lateinit var creationService: ArtifactCreationService
    lateinit var scopeService: ArtifactScopeService
    lateinit var conditionParser: InstrumentConditionParser
    private val log = LoggerFactory.getLogger(javaClass)
    private val availableSourceFileMarkers = Maps.newConcurrentMap<Int, SourceFileMarker>()
    private val globalSourceMarkEventListeners = Lists.newArrayList<SourceMarkEventListener>()

    suspend fun clearAvailableSourceFileMarkers() {
        check(enabled) { "SourceMarker disabled" }

        availableSourceFileMarkers.forEach {
            deactivateSourceFileMarker(it.value)
        }
        availableSourceFileMarkers.clear()
    }

    suspend fun deactivateSourceFileMarker(sourceFileMarker: SourceFileMarker): Boolean {
        check(enabled) { "SourceMarker disabled" }

        if (availableSourceFileMarkers.remove(sourceFileMarker.hashCode()) != null) {
            sourceFileMarker.clearSourceMarks()
            sourceFileMarker.psiFile.putUserData(SourceFileMarker.KEY, null)
            log.info("Deactivated source file marker: {}", sourceFileMarker)
            return true
        }
        return false
    }

    fun getSourceFileMarker(psiFile: PsiFile): SourceFileMarker? {
        check(enabled) { "SourceMarker disabled" }

        var fileMarker = psiFile.getUserData(SourceFileMarker.KEY)
        if (fileMarker != null) {
            return fileMarker
        } else if (!SourceFileMarker.isFileSupported(psiFile)) {
            return null
        }

        fileMarker = configuration.sourceFileMarkerProvider.createSourceFileMarker(psiFile)
        availableSourceFileMarkers.putIfAbsent(psiFile.hashCode(), fileMarker)
        fileMarker = availableSourceFileMarkers[psiFile.hashCode()]!!
        psiFile.putUserData(SourceFileMarker.KEY, fileMarker)

        GlobalScope.launch {
            guideProvider.determineGuideMarks(fileMarker)
        }
        return fileMarker
    }

    fun getSourceFileMarker(classQualifiedName: String): SourceFileMarker? {
        check(enabled) { "SourceMarker disabled" }

        return availableSourceFileMarkers.values.find {
            namingService.getQualifiedClassNames(it.psiFile).find { it.identifier.contains(classQualifiedName) } != null
        }
    }

    fun getSourceFileMarker(artifactQualifiedName: ArtifactQualifiedName): SourceFileMarker? {
        check(enabled) { "SourceMarker disabled" }

        val classArtifactQualifiedName = artifactQualifiedName.copy(
            identifier = ArtifactNameUtils.getQualifiedClassName(artifactQualifiedName.identifier)!!,
            type = ArtifactType.CLASS
        )
        return availableSourceFileMarkers.values.find {
            namingService.getQualifiedClassNames(it.psiFile).contains(classArtifactQualifiedName)
        }
    }

    fun getAvailableSourceFileMarkers(): List<SourceFileMarker> {
        check(enabled) { "SourceMarker disabled" }

        return ImmutableList.copyOf(availableSourceFileMarkers.values)
    }

    fun addGlobalSourceMarkEventListener(sourceMarkEventListener: SourceMarkEventListener) {
        globalSourceMarkEventListeners.add(sourceMarkEventListener)
    }

    fun getGlobalSourceMarkEventListeners(): List<SourceMarkEventListener> {
        return ImmutableList.copyOf(globalSourceMarkEventListeners)
    }

    fun clearGlobalSourceMarkEventListeners() {
        globalSourceMarkEventListeners.clear()
    }

    fun getSourceMark(artifactQualifiedName: ArtifactQualifiedName, type: SourceMark.Type): SourceMark? {
        check(enabled) { "SourceMarker disabled" }

        availableSourceFileMarkers.values.forEach {
            val sourceMark = it.getSourceMark(artifactQualifiedName, type)
            if (sourceMark != null) {
                return sourceMark
            }
        }
        return null
    }

    fun getSourceMarks(artifactQualifiedName: ArtifactQualifiedName): List<SourceMark> {
        check(enabled) { "SourceMarker disabled" }

        availableSourceFileMarkers.values.forEach {
            val sourceMarks = it.getSourceMarks(artifactQualifiedName)
            if (sourceMarks.isNotEmpty()) {
                return sourceMarks
            }
        }
        return emptyList()
    }

    fun getSourceMarks(): List<SourceMark> {
        check(enabled) { "SourceMarker disabled" }
        return availableSourceFileMarkers.values.flatMap { it.getSourceMarks() }
    }

    fun getSourceMark(id: String): SourceMark? {
        return getSourceMarks().find { it.id == id }
    }
}
