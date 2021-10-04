package com.sourceplusplus.marker.py

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyFile
import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.marker.plugin.SourceLineMarkerProvider
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.gutter.GutterMark
import org.slf4j.LoggerFactory

/**
 * Associates Python [GutterMark]s to PSI elements.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonLineMarkerProvider : SourceLineMarkerProvider() {

    private val log = LoggerFactory.getLogger(PythonLineMarkerProvider::class.java)

    companion object {
        init {
            SourceFileMarker.SUPPORTED_FILE_TYPES.add(PyFile::class.java)
        }
    }

    override fun getLineMarkerInfo(parent: PsiElement?, element: PsiElement): LineMarkerInfo<PsiElement>? {
        val fileMarker = SourceMarker.getSourceFileMarker(element.containingFile)
        return null //todo: this
    }

    override fun getName(): String = "Python source line markers"
}
