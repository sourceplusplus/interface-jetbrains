package com.sourceplusplus.marker.plugin

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyFile
import com.sourceplusplus.marker.source.SourceFileMarker
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class PythonLineMarkerProvider : SourceLineMarkerProvider() {

    private val log = LoggerFactory.getLogger(PythonLineMarkerProvider::class.java)

    companion object {
        init {
            SourceFileMarker.SUPPORTED_FILE_TYPES.add(PyFile::class.java)
        }
    }

    override fun getLineMarkerInfo(parent: PsiElement?, element: PsiElement): LineMarkerInfo<PsiElement>? {
        return null //todo: this
    }
}
