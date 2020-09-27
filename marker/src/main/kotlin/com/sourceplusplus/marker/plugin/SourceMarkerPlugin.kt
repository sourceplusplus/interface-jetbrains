package com.sourceplusplus.marker.plugin

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.intellij.psi.PsiFile
import com.sourceplusplus.marker.plugin.config.SourceMarkerConfiguration
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.SourceFileMarkerProvider
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.marker.source.navigate.ArtifactNavigator
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
//todo: should be per project
object SourceMarkerPlugin : SourceFileMarkerProvider {

    @Volatile
    var enabled = true
    val configuration: SourceMarkerConfiguration = SourceMarkerConfiguration()
    val artifactNavigator = ArtifactNavigator()
    private val log = LoggerFactory.getLogger(javaClass)
    private val availableSourceFileMarkers = Maps.newConcurrentMap<Int, SourceFileMarker>()
    private val globalSourceMarkEventListeners = Lists.newArrayList<SourceMarkEventListener>()

    fun clearAvailableSourceFileMarkers() {
        check(enabled) { "SourceMarkerPlugin disabled" }

        availableSourceFileMarkers.forEach {
            deactivateSourceFileMarker(it.value)
        }
        availableSourceFileMarkers.clear()
    }

    fun refreshAvailableSourceFileMarkers(recreateFileMarkers: Boolean) {
        check(enabled) { "SourceMarkerPlugin disabled" }

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
        check(enabled) { "SourceMarkerPlugin disabled" }

        if (availableSourceFileMarkers.remove(sourceFileMarker.hashCode()) != null) {
            sourceFileMarker.clearSourceMarks()
            sourceFileMarker.psiFile.putUserData(SourceFileMarker.KEY, null)
            log.info("Deactivated source file marker: {}", sourceFileMarker)
            return true
        }
        return false
    }

    fun getSourceFileMarker(psiFile: PsiFile): SourceFileMarker? {
        check(enabled) { "SourceMarkerPlugin disabled" }

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
        check(enabled) { "SourceMarkerPlugin disabled" }

        availableSourceFileMarkers.values.forEach { marker ->
            if (marker.getClassQualifiedNames().contains(classQualifiedName)) {
                return marker
            }
        }
        return null
    }

    fun getAvailableSourceFileMarkers(): List<SourceFileMarker> {
        check(enabled) { "SourceMarkerPlugin disabled" }

        return ImmutableList.copyOf(availableSourceFileMarkers.values)
    }

    fun addGlobalSourceMarkEventListener(sourceMarkEventListener: SourceMarkEventListener) {
        globalSourceMarkEventListeners.add(sourceMarkEventListener)
    }

    fun getGlobalSourceMarkEventListeners(): List<SourceMarkEventListener> {
        return ImmutableList.copyOf(globalSourceMarkEventListeners)
    }

    fun getSourceMark(artifactQualifiedName: String, type: SourceMark.Type): SourceMark? {
        check(enabled) { "SourceMarkerPlugin disabled" }

        availableSourceFileMarkers.values.forEach {
            val sourceMark = it.getSourceMark(artifactQualifiedName, type)
            if (sourceMark != null) {
                return sourceMark
            }
        }
        return null
    }

    fun getSourceMarks(artifactQualifiedName: String): List<SourceMark> {
        check(enabled) { "SourceMarkerPlugin disabled" }

        availableSourceFileMarkers.values.forEach {
            val sourceMarks = it.getSourceMarks(artifactQualifiedName)
            if (sourceMarks.isNotEmpty()) {
                return sourceMarks
            }
        }
        return emptyList()
    }
}
