package com.sourceplusplus.marker.source

import com.intellij.psi.PsiFile

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceFileMarkerProvider {

    fun createSourceFileMarker(psiFile: PsiFile): SourceFileMarker {
        return SourceFileMarker(psiFile)
    }
}
