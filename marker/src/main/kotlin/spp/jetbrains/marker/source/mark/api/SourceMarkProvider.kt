package spp.jetbrains.marker.source.mark.api

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * Returns a [SourceMark] given a PSI element.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceMarkProvider {

    fun createExpressionSourceMark(
        psiExpression: PsiElement, type: SourceMark.Type
    ): ExpressionSourceMark

    fun createMethodSourceMark(
        psiMethod: PsiNameIdentifierOwner, qualifiedName: String, type: SourceMark.Type
    ): MethodSourceMark

    fun createClassSourceMark(
        psiClass: PsiNameIdentifierOwner, qualifiedName: String, type: SourceMark.Type
    ): ClassSourceMark
}
