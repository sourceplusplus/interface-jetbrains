package spp.jetbrains.marker

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface ArtifactNamingService {

    fun getFullyQualifiedName(element: PsiElement): String

    fun getClassQualifiedNames(psiFile: PsiFile): List<String>
}
