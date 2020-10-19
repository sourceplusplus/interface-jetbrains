package com.sourceplusplus.marker.source.mark.api

import org.jetbrains.uast.UClass
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod

/**
 * Returns a [SourceMark] given a PSI element.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceMarkProvider {

    fun createSourceMark(psiExpression: UExpression, type: SourceMark.Type): ExpressionSourceMark
    fun createSourceMark(psiMethod: UMethod, type: SourceMark.Type): MethodSourceMark
    fun createSourceMark(psiClass: UClass, type: SourceMark.Type): ClassSourceMark
}
