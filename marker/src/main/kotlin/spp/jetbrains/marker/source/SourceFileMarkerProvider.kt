package spp.jetbrains.marker.source

import com.intellij.psi.PsiFile

/**
 * Returns a [SourceFileMarker] given a [PsiFile].
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceFileMarkerProvider {

    fun createSourceFileMarker(psiFile: PsiFile): SourceFileMarker {
        return SourceFileMarker(psiFile)
    }
}
