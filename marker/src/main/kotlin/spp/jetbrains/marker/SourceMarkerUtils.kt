/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.parentOfType
import spp.jetbrains.artifact.service.ArtifactTypeService
import spp.jetbrains.marker.source.mark.api.SourceMark
import javax.swing.SwingUtilities

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
        }
        require(line > 0) { "Line number must be greater than 0" }

        val offset = document.getLineStartOffset(line - 1)
        var element = file.viewProvider.findElementAt(offset)
        if (element != null) {
            //check for name identifier on same line (e.g. class/method name)
            val nameIdentifierOwner = element.parentOfType<PsiNameIdentifierOwner>()
            if (nameIdentifierOwner != null && nameIdentifierOwner.nameIdentifier?.let { getLineNumber(it) } == line) {
                return nameIdentifierOwner
            }

            //check for annotation on same line
            val annotationOwner = ArtifactTypeService.getAnnotationOwnerIfAnnotation(element, line)
            if (annotationOwner != null) {
                return annotationOwner
            }

            if (document.getLineNumber(element.textOffset) != line - 1) {
                element = element.nextSibling
            }
        }

        if (element != null && getLineNumber(element) != line) {
            return null
        } else if (element != null && ignoreComments && ArtifactTypeService.isComment(element)) {
            return null
        }

        return element
    }

    /**
     * todo: description.
     *
     * @since 0.3.0
     */
    @JvmStatic
    fun getLineNumber(element: PsiElement): Int {
        return getStartLineNumber(element)
    }

    @JvmStatic
    fun getStartLineNumber(element: PsiElement): Int {
        val document = element.containingFile.viewProvider.document
        return document!!.getLineNumber(element.textRange.startOffset) + 1
    }

    @JvmStatic
    fun getEndLineNumber(element: PsiElement): Int {
        val document = element.containingFile.viewProvider.document
        return document!!.getLineNumber(element.textRange.endOffset) + 1
    }

    fun getPrefixSpacingCount(method: PsiElement): Int {
        val document = method.containingFile.viewProvider.document!!
        val lineStartOffset = document.getLineStartOffset(document.getLineNumber(method.textRange.startOffset))
        return method.textRange.startOffset - lineStartOffset
    }

    fun getJvmLanguages(): List<String> {
        return listOf("JAVA", "kotlin", "Groovy", "Scala")
    }

    fun getJavaScriptLanguages(): List<String> {
        return listOf("JavaScript", "ECMAScript 6")
    }

    @JvmStatic
    fun isPython(language: Language): Boolean {
        return language.id == "Python"
    }

    @JvmStatic
    fun isJvm(language: Language): Boolean {
        return getJvmLanguages().contains(language.id)
    }

    @JvmStatic
    fun isJavaScript(language: Language): Boolean {
        return getJavaScriptLanguages().contains(language.id)
    }

    fun doOnDispatchThread(action: () -> Unit) {
        if (ApplicationManager.getApplication().isDispatchThread) {
            action.invoke()
        } else {
            SwingUtilities.invokeLater { action.invoke() }
        }
    }

    fun substringAfterIgnoreCase(str: String, search: String): String {
        val index = str.indexOf(search, ignoreCase = true)
        if (index == -1) {
            return str
        }
        return str.substring(index + search.length)
    }
}
