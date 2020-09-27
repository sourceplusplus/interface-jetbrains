package com.sourceplusplus.marker

import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.marker.source.mark.gutter.ClassGutterMark
import com.sourceplusplus.marker.source.mark.gutter.MethodGutterMark
import com.sourceplusplus.marker.source.mark.inlay.ExpressionInlayMark
import com.sourceplusplus.marker.source.mark.inlay.MethodInlayMark
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.uast.*
import org.slf4j.LoggerFactory
import java.awt.Point
import java.util.*

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class MarkerUtils private constructor() {
    companion object {
        private val log = LoggerFactory.getLogger(MarkerUtils::class.java)

        /**
         * todo: description.
         *
         * @since 0.0.1
         */
        @JvmStatic
        fun getElementAtLine(file: PsiFile, line: Int): PsiElement? {
            val document: Document = PsiDocumentManager.getInstance(file.project).getDocument(file)!!
            val offset = document.getLineStartOffset(line - 1)
            var element: PsiElement = file.viewProvider.findElementAt(offset)!!
            if (document.getLineNumber(element.textOffset) != line - 1) {
                element = element.nextSibling
            }
            return element
        }

        /**
         * todo: description.
         *
         * @since 0.0.1
         */
        @JvmStatic
        @JvmOverloads
        fun getOrCreateExpressionInlayMark(
            fileMarker: SourceFileMarker,
            lineNumber: Int,
            autoApply: Boolean = false
        ): ExpressionInlayMark? {
            val element = getElementAtLine(fileMarker.psiFile, lineNumber)
            return if (element is PsiStatement) {
                getOrCreateExpressionInlayMark(fileMarker, element, autoApply = autoApply)
            } else null
        }

        /**
         * todo: description.
         *
         * @since 0.0.1
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
                    if (inlayMark.updatePsiExpression(statementExpression.toUElement() as UExpression)) {
                        statementExpression.putUserData(SourceKey.InlayMark, inlayMark)
                    } else {
                        inlayMark = null
                    }
                }
            }

            return if (inlayMark == null) {
                inlayMark = fileMarker.createSourceMark(
                    statementExpression.toUElement() as UExpression,
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
         * @since 0.0.1
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
         * @since 0.0.1
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
                    if (inlayMark.updatePsiMethod(element.parent.toUElement() as UMethod)) {
                        element.putUserData(SourceKey.InlayMark, inlayMark)
                    } else {
                        inlayMark = null
                    }
                }
            }

            return if (inlayMark == null) {
                inlayMark = fileMarker.createSourceMark(
                    element.parent.toUElement() as UMethod,
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
         * @since 0.0.1
         */
        @JvmStatic
        @JvmOverloads
        @Synchronized
        fun getOrCreateMethodGutterMark(
            fileMarker: SourceFileMarker,
            element: PsiElement,
            autoApply: Boolean = true
        ): MethodGutterMark? {
            var gutterMark = element.getUserData(SourceKey.GutterMark) as MethodGutterMark?
            if (gutterMark == null) {
                gutterMark = fileMarker.getMethodSourceMark(element.parent, SourceMark.Type.GUTTER) as MethodGutterMark?
                if (gutterMark != null) {
                    if (gutterMark.updatePsiMethod(element.parent.toUElement() as UMethod)) {
                        element.putUserData(SourceKey.GutterMark, gutterMark)
                    } else {
                        gutterMark = null
                    }
                }
            }

            if (gutterMark == null) {
                gutterMark = fileMarker.createSourceMark(
                    element.parent.toUElement() as UMethod,
                    SourceMark.Type.GUTTER
                ) as MethodGutterMark
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
         * @since 0.0.1
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
                gutterMark = fileMarker.createSourceMark(
                    element.parent.toUElement() as UClass,
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
         * @since 0.0.1
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
         * @since 0.0.1
         */
        @JvmStatic
        fun getQualifiedClassName(qualifiedName: String): String {
            var withoutArgs = qualifiedName.substring(0, qualifiedName.indexOf("("))
            return if (withoutArgs.contains("<")) {
                withoutArgs = withoutArgs.substring(0, withoutArgs.indexOf("<"))
                withoutArgs.substring(withoutArgs.lastIndexOf("?") + 1, withoutArgs.lastIndexOf("."))
            } else {
                withoutArgs.substring(withoutArgs.lastIndexOf("?") + 1, withoutArgs.lastIndexOf("."))
            }
        }

        /**
         * todo: description.
         *
         * @since 0.0.1
         */
        @JvmStatic
        fun getFullyQualifiedName(expression: UExpression): String {
            val qualifiedMethodName = expression.getContainingUMethod()?.let { getFullyQualifiedName(it) }
            val psiFile = expression.getContainingUFile()!!.sourcePsi
            val document: Document = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile)!!

            return if (expression is UDeclarationsExpression) {
                //todo: support for multi-declaration statements
                """$qualifiedMethodName#${
                    document.getLineNumber(expression.declarations[0].sourcePsi!!.textOffset)
                }#${Base64.getEncoder().encodeToString(expression.toString().toByteArray())}"""
            } else {
                """$qualifiedMethodName#${
                    document.getLineNumber(expression.sourcePsi!!.textOffset)
                }#${Base64.getEncoder().encodeToString(expression.toString().toByteArray())}"""
            }
        }

        /**
         * todo: description.
         *
         * @since 0.0.1
         */
        @JvmStatic
        fun getFullyQualifiedName(method: PsiMethod): String {
            return getFullyQualifiedName(method.toUElement() as UMethod)
        }

        /**
         * todo: description.
         *
         * @since 0.0.1
         */
        @JvmStatic
        fun getFullyQualifiedName(method: UMethod): String {
            //todo: PsiUtil.getMemberQualifiedName(method)!!
            return "${method.containingClass!!.qualifiedName}.${getQualifiedName(method)}"
        }

        /**
         * todo: description.
         *
         * @since 0.0.1
         */
        @JvmStatic
        fun getFullyQualifiedName(theClass: UClass): String {
            //todo: PsiUtil.getMemberQualifiedName(method)!!
            return "${theClass.qualifiedName}"
        }

        /**
         * todo: description.
         *
         * @since 0.0.1
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
                    for (i in 0 until arrayDimensions) {
                        methodParams += "[]"
                    }
                } else {
                    methodParams += it.typeElement!!.text
                }
            }
            return "$methodName($methodParams)"
        }

        /**
         * todo: description.
         *
         * @since 0.0.1
         */
        @JvmStatic
        fun convertPointToLineNumber(project: Project, p: Point): Int {
            val myEditor = FileEditorManager.getInstance(project).selectedTextEditor
            val document = myEditor!!.document
            val line = EditorUtil.yPositionToLogicalLine(myEditor, p)
            if (!isValidLine(document, line)) return -1
            val startOffset = document.getLineStartOffset(line)
            val region = myEditor.foldingModel.getCollapsedRegionAtOffset(startOffset)
            return if (region != null) {
                document.getLineNumber(region.endOffset)
            } else line
        }

        private fun isValidLine(document: Document, line: Int): Boolean {
            if (line < 0) return false
            val lineCount = document.lineCount
            return if (lineCount == 0) line == 0 else line < lineCount
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
}
