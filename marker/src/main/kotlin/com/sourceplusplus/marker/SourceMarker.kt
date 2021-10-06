package com.sourceplusplus.marker

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.intellij.psi.PsiFile
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import org.slf4j.LoggerFactory

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
    lateinit var namingService: ArtifactNamingService
    lateinit var creationService: ArtifactCreationService
    lateinit var scopeService: ArtifactScopeService
    lateinit var conditionParser: InstrumentConditionParser
    private val log = LoggerFactory.getLogger(javaClass)
    private val availableSourceFileMarkers = Maps.newConcurrentMap<Int, SourceFileMarker>()
    private val globalSourceMarkEventListeners = Lists.newArrayList<SourceMarkEventListener>()

    fun clearAvailableSourceFileMarkers() {
        check(enabled) { "SourceMarker disabled" }

        availableSourceFileMarkers.forEach {
            deactivateSourceFileMarker(it.value)
        }
        availableSourceFileMarkers.clear()
    }

    fun refreshAvailableSourceFileMarkers(recreateFileMarkers: Boolean) {
        check(enabled) { "SourceMarker disabled" }

        if (recreateFileMarkers) {
            val previousFileMarkers = getAvailableSourceFileMarkers()
            clearAvailableSourceFileMarkers()
            previousFileMarkers.forEach {
                getSourceFileMarker(it.psiFile)!!.refresh()
            }
        } else {
            availableSourceFileMarkers.forEach {
                it.value.refresh()
            }
        }
    }

    fun deactivateSourceFileMarker(sourceFileMarker: SourceFileMarker): Boolean {
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
        return fileMarker
    }

    fun getSourceFileMarker(classQualifiedName: String): SourceFileMarker? {
        check(enabled) { "SourceMarker disabled" }

        return availableSourceFileMarkers.values.find {
            namingService.getClassQualifiedNames(it.psiFile).contains(classQualifiedName)
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

    fun getSourceMark(artifactQualifiedName: String, type: SourceMark.Type): SourceMark? {
        check(enabled) { "SourceMarker disabled" }

        availableSourceFileMarkers.values.forEach {
            val sourceMark = it.getSourceMark(artifactQualifiedName, type)
            if (sourceMark != null) {
                return sourceMark
            }
        }
        return null
    }

    fun getSourceMarks(artifactQualifiedName: String): List<SourceMark> {
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
