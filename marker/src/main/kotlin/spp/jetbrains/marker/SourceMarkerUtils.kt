/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.marker

import com.intellij.openapi.editor.Document
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.util.parentOfType
import spp.jetbrains.marker.source.mark.api.SourceMark

/**
 * Utility functions for working with [SourceMark]s.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkerUtils {

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getElementAtLine(file: PsiFile, line: Int, ignoreComments: Boolean = true): PsiElement? {
        val document: Document = PsiDocumentManager.getInstance(file.project).getDocument(file)!!
        if (document.lineCount == line - 1) {
            return null
        } else if (line < 0) {
            throw IllegalArgumentException("Line number must be greater than 0")
        }

        val offset = document.getLineStartOffset(line - 1)
        var element = file.viewProvider.findElementAt(offset)
        if (element != null) {
            //check for name identifier on same line (e.g. class/method name)
            val nameIdentifierOwner = element.parentOfType<PsiNameIdentifierOwner>()
            if (nameIdentifierOwner != null && nameIdentifierOwner.nameIdentifier?.let { getLineNumber(it) } == line) {
                return nameIdentifierOwner
            }

            //check for annotation on same line
            val annotation = element.parentOfType<PsiAnnotation>()
            if (annotation != null && getLineNumber(annotation) == line) {
                if (annotation.owner is PsiModifierList) {
                    return (annotation.owner as PsiModifierList).parent
                }
            }

            if (document.getLineNumber(element.textOffset) != line - 1) {
                element = element.nextSibling
            }
        }

        if (element != null && getLineNumber(element) != line) {
            return null
        } else if (element != null && ignoreComments && isComment(element)) {
            return null
        }

        return element
    }

    private fun isComment(element: PsiElement): Boolean {
        val comment = element is PsiDocToken || element is PsiComment || element is PsiDocComment
        if (comment) return true

        return if (element is LeafPsiElement) {
            isComment(element.parent)
        } else false
    }

    /**
     * todo: description.
     *
     * @since 0.3.0
     */
    @JvmStatic
    fun getLineNumber(element: PsiElement): Int {
        val document = element.containingFile.viewProvider.document
        return document!!.getLineNumber(element.textRange.startOffset) + 1
    }

    fun getJvmLanguages(): List<String> {
        return listOf("JAVA", "kotlin", "Groovy", "Scala")
    }

    fun getJavaScriptLanguages(): List<String> {
        return listOf("JavaScript", "ECMAScript 6")
    }
}
