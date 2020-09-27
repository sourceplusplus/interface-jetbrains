package com.sourceplusplus.marker.source.mark.api

import org.jetbrains.uast.UClass
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceMarkProvider {

    fun createSourceMark(psiExpression: UExpression, type: SourceMark.Type): ExpressionSourceMark
    fun createSourceMark(psiMethod: UMethod, type: SourceMark.Type): MethodSourceMark
    fun createSourceMark(psiClass: UClass, type: SourceMark.Type): ClassSourceMark
}
