package com.sourceplusplus.marker.py

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.sourceplusplus.marker.ArtifactNamingService

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonArtifactNamingService : ArtifactNamingService {
    override fun getFullyQualifiedName(element: PsiElement): String {
        val fileName = element.containingFile.name
        return fileName
    }

    override fun getClassQualifiedNames(psiFile: PsiFile): List<String> {
        return listOf(psiFile.virtualFile.path)
    }
}
