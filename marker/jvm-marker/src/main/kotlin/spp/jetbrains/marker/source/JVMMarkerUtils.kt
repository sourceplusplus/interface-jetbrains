/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.marker.source

import com.intellij.lang.Language
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.uast.*
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.gutter.ClassGutterMark
import spp.jetbrains.marker.source.mark.gutter.ExpressionGutterMark
import spp.jetbrains.marker.source.mark.gutter.MethodGutterMark
import spp.jetbrains.marker.source.mark.inlay.ExpressionInlayMark
import spp.jetbrains.marker.source.mark.inlay.MethodInlayMark
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import java.util.*

/**
 * JVM utility functions for working with [SourceMark]s.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions")
object JVMMarkerUtils {

    private val log = LoggerFactory.getLogger(JVMMarkerUtils::class.java)

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiStatement,
        autoApply: Boolean = false
    ): ExpressionInlayMark? {
        log.trace("getOrCreateExpressionInlayMark: $element")
        val statementExpression: PsiElement = getUniversalExpression(element)
        var lookupExpression: PsiElement = statementExpression
        if (lookupExpression is PsiDeclarationStatement) {
            //todo: support for multi-declaration statements
            lookupExpression = lookupExpression.firstChild
        }

        var inlayMark = lookupExpression.getUserData(SourceKey.InlayMark) as ExpressionInlayMark?
        if (inlayMark == null) {
            inlayMark = fileMarker.getExpressionSourceMark(
                lookupExpression,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark?
            if (inlayMark != null) {
                if (inlayMark.updatePsiExpression(
                        statementExpression,
                        getFullyQualifiedName(statementExpression.toUElement() as UExpression)
                    )
                ) {
                    statementExpression.putUserData(SourceKey.InlayMark, inlayMark)
                } else {
                    inlayMark = null
                }
            }
        }

        return if (inlayMark == null) {
            inlayMark = fileMarker.createExpressionSourceMark(
                statementExpression,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark
            return if (autoApply) {
                if (inlayMark.canApply()) {
                    inlayMark.apply(true)
                    inlayMark
                } else {
                    null
                }
            } else {
                inlayMark
            }
        } else {
            if (fileMarker.removeIfInvalid(inlayMark)) {
                statementExpression.putUserData(SourceKey.InlayMark, null)
                null
            } else {
                inlayMark
            }
        }
    }

    /**
     * todo: description.
     *
     * @since 0.3.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = false
    ): ExpressionInlayMark? {
        log.trace("getOrCreateExpressionInlayMark: $element")
        var inlayMark = element.getUserData(SourceKey.InlayMark) as ExpressionInlayMark?
        if (inlayMark == null) {
            inlayMark = fileMarker.getExpressionSourceMark(
                element,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark?
            if (inlayMark != null) {
                if (inlayMark.updatePsiExpression(
                        element, getFullyQualifiedName(element.toUElement() as UExpression)
                    )
                ) {
                    element.putUserData(SourceKey.InlayMark, inlayMark)
                } else {
                    inlayMark = null
                }
            }
        }

        return if (inlayMark == null) {
            val uExpression = element.toUElement()
            if (uExpression !is UExpression) return null
            inlayMark = fileMarker.createExpressionSourceMark(
                element,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark
            return if (autoApply) {
                if (inlayMark.canApply()) {
                    inlayMark.apply(true)
                    inlayMark
                } else {
                    null
                }
            } else {
                inlayMark
            }
        } else {
            if (fileMarker.removeIfInvalid(inlayMark)) {
                element.putUserData(SourceKey.InlayMark, null)
                null
            } else {
                inlayMark
            }
        }
    }

    /**
     * todo: description.
     *
     * @since 0.3.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun createExpressionInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiStatement,
        autoApply: Boolean = false
    ): ExpressionInlayMark {
        log.trace("createExpressionInlayMark: $element")
        val statementExpression: PsiElement = getUniversalExpression(element)
        val inlayMark = fileMarker.createExpressionSourceMark(
            statementExpression,
            SourceMark.Type.INLAY
        ) as ExpressionInlayMark
        return if (autoApply) {
            if (inlayMark.canApply()) {
                inlayMark.apply(true)
                inlayMark
            } else {
                throw IllegalStateException("Could not apply inlay mark: $inlayMark")
            }
        } else {
            inlayMark
        }
    }

    /**
     * todo: description.
     *
     * @since 0.3.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun createExpressionInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = false
    ): ExpressionInlayMark {
        log.trace("createExpressionInlayMark: $element")
        val inlayMark = fileMarker.createExpressionSourceMark(
            element,
            SourceMark.Type.INLAY
        ) as ExpressionInlayMark
        return if (autoApply) {
            if (inlayMark.canApply()) {
                inlayMark.apply(true)
                inlayMark
            } else {
                throw IllegalStateException("Could not apply inlay mark: $inlayMark")
            }
        } else {
            inlayMark
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun getOrCreateExpressionGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiStatement,
        autoApply: Boolean = false
    ): ExpressionGutterMark? {
        log.trace("getOrCreateExpressionGutterMark: $element")
        val statementExpression: PsiElement = getUniversalExpression(element)
        var lookupExpression: PsiElement = statementExpression
        if (lookupExpression is PsiDeclarationStatement) {
            //todo: support for multi-declaration statements
            lookupExpression = lookupExpression.firstChild
        }

        var gutterMark = lookupExpression.getUserData(SourceKey.GutterMark) as ExpressionGutterMark?
        if (gutterMark == null) {
            gutterMark = fileMarker.getExpressionSourceMark(
                lookupExpression,
                SourceMark.Type.GUTTER
            ) as ExpressionGutterMark?
            if (gutterMark != null) {
                if (gutterMark.updatePsiExpression(
                        statementExpression, getFullyQualifiedName(statementExpression.toUElement() as UExpression)
                    )
                ) {
                    statementExpression.putUserData(SourceKey.GutterMark, gutterMark)
                } else {
                    gutterMark = null
                }
            }
        }

        return if (gutterMark == null) {
            gutterMark = fileMarker.createExpressionSourceMark(
                statementExpression,
                SourceMark.Type.GUTTER
            ) as ExpressionGutterMark
            return if (autoApply) {
                if (gutterMark.canApply()) {
                    gutterMark.apply(true)
                    gutterMark
                } else {
                    null
                }
            } else {
                gutterMark
            }
        } else {
            if (fileMarker.removeIfInvalid(gutterMark)) {
                statementExpression.putUserData(SourceKey.InlayMark, null)
                null
            } else {
                gutterMark
            }
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getUniversalExpression(element: PsiStatement): PsiElement {
        var statementExpression: PsiElement = element
        if (statementExpression is PsiExpressionStatement) {
            statementExpression = statementExpression.firstChild
        }
        return statementExpression
    }

    /**
     * todo: description.
     *
     * @since 0.4.3
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun createMethodGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = false
    ): MethodGutterMark {
        log.trace("createMethodGutterMark: $element")
        val gutterMark = fileMarker.createMethodSourceMark(
            element.parent as PsiNameIdentifierOwner,
            getFullyQualifiedName(element.parent.toUElement() as UMethod),
            SourceMark.Type.GUTTER
        ) as MethodGutterMark
        return if (autoApply) {
            if (gutterMark.canApply()) {
                gutterMark.apply(true)
                gutterMark
            } else {
                throw IllegalStateException("Could not apply gutter mark: $gutterMark")
            }
        } else {
            gutterMark
        }
    }

    /**
     * todo: description.
     *
     * @since 0.4.2
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun createMethodInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = false
    ): MethodInlayMark {
        log.trace("createMethodInlayMark: $element")
        val inlayMark = fileMarker.createMethodSourceMark(
            element.parent as PsiNameIdentifierOwner,
            getFullyQualifiedName(element.parent.toUElement() as UMethod),
            SourceMark.Type.INLAY
        ) as MethodInlayMark
        return if (autoApply) {
            if (inlayMark.canApply()) {
                inlayMark.apply(true)
                inlayMark
            } else {
                throw IllegalStateException("Could not apply inlay mark: $inlayMark")
            }
        } else {
            inlayMark
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun getOrCreateMethodInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = false
    ): MethodInlayMark? {
        var inlayMark = element.getUserData(SourceKey.InlayMark) as MethodInlayMark?
        if (inlayMark == null) {
            inlayMark = fileMarker.getMethodSourceMark(element.parent, SourceMark.Type.INLAY) as MethodInlayMark?
            if (inlayMark != null) {
                if (inlayMark.updatePsiMethod(element.parent as PsiNameIdentifierOwner)) {
                    element.putUserData(SourceKey.InlayMark, inlayMark)
                } else {
                    inlayMark = null
                }
            }
        }

        return if (inlayMark == null) {
            inlayMark = fileMarker.createMethodSourceMark(
                element.parent as PsiNameIdentifierOwner,
                getFullyQualifiedName(element.parent.toUElement() as UMethod),
                SourceMark.Type.INLAY
            ) as MethodInlayMark
            return if (autoApply) {
                if (inlayMark.canApply()) {
                    inlayMark.apply(true)
                    inlayMark
                } else {
                    null
                }
            } else {
                inlayMark
            }
        } else {
            if (fileMarker.removeIfInvalid(inlayMark)) {
                element.putUserData(SourceKey.InlayMark, null)
                null
            } else {
                inlayMark
            }
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun getOrCreateClassGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = true
    ): ClassGutterMark? {
        var gutterMark = element.getUserData(SourceKey.GutterMark) as ClassGutterMark?
        if (gutterMark == null) {
            gutterMark = fileMarker.getClassSourceMark(element.parent, SourceMark.Type.GUTTER) as ClassGutterMark?
            if (gutterMark != null) {
                if (gutterMark.updatePsiClass(element.parent.toUElement() as UClass)) {
                    element.putUserData(SourceKey.GutterMark, gutterMark)
                } else {
                    gutterMark = null
                }
            }
        }

        if (gutterMark == null) {
            val uClass = element.parent.toUElement() as UClass
            if (uClass.qualifiedName == null) {
                log.warn("Could not determine qualified name of class: {}", uClass)
                return null
            }
            gutterMark = fileMarker.createClassSourceMark(
                element.parent as PsiNameIdentifierOwner,
                getFullyQualifiedName(uClass),
                SourceMark.Type.GUTTER
            ) as ClassGutterMark
            return if (autoApply) {
                if (gutterMark.canApply()) {
                    gutterMark.apply(true)
                    gutterMark
                } else {
                    null
                }
            } else {
                gutterMark
            }
        } else {
            return when {
                fileMarker.removeIfInvalid(gutterMark) -> {
                    element.putUserData(SourceKey.GutterMark, null)
                    null
                }
                gutterMark.configuration.icon != null -> {
                    gutterMark.setVisible(true)
                    gutterMark
                }
                else -> {
                    gutterMark.setVisible(false)
                    gutterMark
                }
            }
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getNameIdentifier(nameIdentifierOwner: PsiNameIdentifierOwner): PsiElement? {
        return when {
            nameIdentifierOwner.language === Language.findLanguageByID("kotlin") -> {
                when (nameIdentifierOwner) {
                    is KtNamedFunction -> nameIdentifierOwner.nameIdentifier
                    else -> (nameIdentifierOwner.navigationElement as KtNamedFunction).nameIdentifier
                }
            }
            nameIdentifierOwner.language === Language.findLanguageByID("Groovy") -> {
                (nameIdentifierOwner.navigationElement as GrMethod).nameIdentifierGroovy //todo: why can't be null?
            }
            else -> nameIdentifierOwner.nameIdentifier
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getQualifiedClassName(qualifiedName: ArtifactQualifiedName): ArtifactQualifiedName {
        var withoutArgs = qualifiedName.identifier.substring(0, qualifiedName.identifier.indexOf("("))
        val classQualifiedName = if (withoutArgs.contains("<")) {
            withoutArgs = withoutArgs.substring(0, withoutArgs.indexOf("<"))
            withoutArgs.substring(withoutArgs.lastIndexOf("?") + 1, withoutArgs.lastIndexOf("."))
        } else {
            withoutArgs.substring(withoutArgs.lastIndexOf("?") + 1, withoutArgs.lastIndexOf("."))
        }
        return ArtifactQualifiedName(classQualifiedName, type = ArtifactType.CLASS)
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getFullyQualifiedName(expression: UExpression): ArtifactQualifiedName {
        val qualifiedMethodName = expression.getContainingUMethod()?.let { getFullyQualifiedName(it) }
        return ArtifactQualifiedName(
            """${qualifiedMethodName!!.identifier}#${
                Base64.getEncoder().encodeToString(expression.toString().toByteArray())
            }""",
            type = ArtifactType.EXPRESSION,
            lineNumber = SourceMarkerUtils.getLineNumber(expression.sourcePsi!!)
        )
    }

    @JvmStatic
    fun getFullyQualifiedName(element: PsiElement): ArtifactQualifiedName {
        val expression = element.toUElement()!!
        val qualifiedMethodName = expression.getContainingUMethod()?.let { getFullyQualifiedName(it) }
        return ArtifactQualifiedName(
            """${qualifiedMethodName!!.identifier}#${
                Base64.getEncoder().encodeToString(expression.toString().toByteArray())
            }""",
            type = ArtifactType.EXPRESSION,
            lineNumber = SourceMarkerUtils.getLineNumber(element)
        )
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getFullyQualifiedName(method: PsiMethod): ArtifactQualifiedName {
        return getFullyQualifiedName(method.toUElement() as UMethod)
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getFullyQualifiedName(method: UMethod): ArtifactQualifiedName {
        //todo: PsiUtil.getMemberQualifiedName(method)!!
        return ArtifactQualifiedName(
            "${method.containingClass!!.qualifiedName}.${getQualifiedName(method)}",
            type = ArtifactType.METHOD
        )
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getFullyQualifiedName(theClass: UClass): ArtifactQualifiedName {
        //todo: PsiUtil.getMemberQualifiedName(method)!!
        return ArtifactQualifiedName("${theClass.qualifiedName}", type = ArtifactType.CLASS)
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getQualifiedName(method: UMethod): String {
        val methodName = method.nameIdentifier!!.text
        var methodParams = ""
        method.parameterList.parameters.forEach {
            if (methodParams.isNotEmpty()) {
                methodParams += ","
            }
            val qualifiedType = PsiUtil.resolveClassInType(it.type)
            val arrayDimensions = getArrayDimensions(it.type.toString())
            if (qualifiedType != null) {
                methodParams += if (qualifiedType.containingClass != null) {
                    qualifiedType.containingClass!!.qualifiedName + '$' + qualifiedType.name
                } else {
                    qualifiedType.qualifiedName
                }
                repeat(arrayDimensions) {
                    methodParams += "[]"
                }
            } else if (it.typeElement != null) {
                methodParams += it.typeElement!!.text
            } else if (it.type is PsiPrimitiveType) {
                methodParams += if (it.language.id == "kotlin") {
                    (it.type as PsiPrimitiveType).boxedTypeName
                } else {
                    it.type.canonicalText
                }
            } else {
                log.warn("Unable to detect element type: {}", it)
            }
        }
        return "$methodName($methodParams)"
    }

    private fun getArrayDimensions(s: String): Int {
        var arrayDimensions = 0
        for (element in s) {
            if (element == '[') {
                arrayDimensions++
            }
        }
        return arrayDimensions
    }
}
