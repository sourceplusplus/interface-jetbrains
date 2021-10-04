package com.sourceplusplus.marker.py

import com.intellij.psi.PsiElement
import com.sourceplusplus.marker.ArtifactNamingService

class PythonArtifactNamingService : ArtifactNamingService {
    override fun getFullyQualifiedName(element: PsiElement): String {
        val fileName = element.containingFile.name
        return fileName
    }
}
