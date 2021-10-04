package com.sourceplusplus.marker.jvm

import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.sourceplusplus.marker.ArtifactNamingService
import com.sourceplusplus.marker.source.SourceMarkerUtils
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactNamingService : ArtifactNamingService {

    override fun getFullyQualifiedName(element: PsiElement): String {
        return when (val uElement = element.toUElement()) {
            is UClass -> SourceMarkerUtils.getFullyQualifiedName(uElement)
            is UMethod -> SourceMarkerUtils.getFullyQualifiedName(uElement)
            is UExpression -> SourceMarkerUtils.getFullyQualifiedName(uElement)
            else -> TODO("Not yet implemented")
        }
    }

    override fun getClassQualifiedNames(psiFile: PsiFile): List<String> {
        return when (psiFile) {
            is PsiClassOwner -> psiFile.classes.map { it.qualifiedName!! }.toList()
            else -> throw IllegalStateException("Unsupported file: $psiFile")
        }
    }
}
