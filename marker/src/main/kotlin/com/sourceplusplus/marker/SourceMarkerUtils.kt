package com.sourceplusplus.marker

import com.intellij.openapi.editor.Document
import com.intellij.psi.*

object SourceMarkerUtils {

    /**
     * todo: description.
     *
     * @since 0.3.0
     */
    @JvmStatic
    fun isBlankLine(psiFile: PsiFile, lineNumber: Int): Boolean {
        val element = SourceMarkerUtils.getElementAtLine(psiFile, lineNumber)
        if (element != null) {
            return SourceMarkerUtils.getLineNumber(element) != lineNumber
        }
        return true
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getElementAtLine(file: PsiFile, line: Int): PsiElement? {
        val document: Document = PsiDocumentManager.getInstance(file.project).getDocument(file)!!
        if (document.lineCount == line - 1) {
            return null
        }

        val offset = document.getLineStartOffset(line - 1)
        var element = file.viewProvider.findElementAt(offset)
        if (element != null) {
            if (document.getLineNumber(element.textOffset) != line - 1) {
                element = element.nextSibling
            }
        }

        if (element != null && getLineNumber(element) != line) {
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
        val document = element.containingFile.viewProvider.document
        return document!!.getLineNumber(element.textRange.startOffset) + 1
    }
}
