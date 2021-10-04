package com.sourceplusplus.marker

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

interface ArtifactNamingService {
    fun getFullyQualifiedName(element: PsiElement): String

    fun getClassQualifiedNames(psiFile: PsiFile): List<String>
}
