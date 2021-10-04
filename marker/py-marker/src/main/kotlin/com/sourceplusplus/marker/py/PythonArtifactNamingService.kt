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
        return element.containingFile.name //todo: include method name when possible?
    }

    //todo: method name could be better
    override fun getClassQualifiedNames(psiFile: PsiFile): List<String> {
        return listOf(psiFile.virtualFile.path)
    }
}
