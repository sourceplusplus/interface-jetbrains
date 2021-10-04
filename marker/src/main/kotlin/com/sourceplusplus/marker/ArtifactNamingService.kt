package com.sourceplusplus.marker

import com.intellij.psi.PsiElement

interface ArtifactNamingService {
    fun getFullyQualifiedName(element: PsiElement): String
}
