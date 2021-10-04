package com.sourceplusplus.marker.source

import com.intellij.psi.PsiFile
import com.sourceplusplus.marker.ArtifactNamingService

/**
 * Returns a [SourceFileMarker] given a [PsiFile].
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceFileMarkerProvider {

    fun getArtifactNamingService(): ArtifactNamingService

    fun createSourceFileMarker(psiFile: PsiFile): SourceFileMarker {
        return SourceFileMarker(psiFile, getArtifactNamingService())
    }
}
