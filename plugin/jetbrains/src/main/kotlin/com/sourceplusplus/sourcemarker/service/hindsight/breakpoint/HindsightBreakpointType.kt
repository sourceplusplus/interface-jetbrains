package com.sourceplusplus.sourcemarker.service.hindsight.breakpoint

import com.intellij.debugger.engine.DebuggerUtils
import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.intellij.debugger.ui.breakpoints.JavaBreakpointType
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.DocumentUtil
import com.intellij.util.PairFunction
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.sourceplusplus.protocol.SourceMarkerServices.Instance.Tracing
import com.sourceplusplus.sourcemarker.icons.SourceMarkerIcons
import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class HindsightBreakpointType : XLineBreakpointType<HindsightBreakpointProperties>(
    "hindsight-breakpoint", "Hindsight Breakpoint"
), JavaBreakpointType<HindsightBreakpointProperties> {

    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
        if (Tracing.hindsightDebugger == null) return false
        return canPutAtElement(file, line, project) { element: PsiElement, _: Document ->
            element !is PsiMethod
        }
    }

    override fun getEnabledIcon(): Icon {
        return SourceMarkerIcons.EYE_ICON
    }

    override fun getDisabledIcon(): Icon {
        return SourceMarkerIcons.EYE_SLASH_ICON
    }

    override fun createBreakpointProperties(file: VirtualFile, line: Int): HindsightBreakpointProperties {
        return HindsightBreakpointProperties()
    }

    override fun createJavaBreakpoint(
        project: Project, breakpoint: XBreakpoint<HindsightBreakpointProperties>
    ): Breakpoint<HindsightBreakpointProperties> {
        return HindsightLineBreakpoint(project, breakpoint)
    }

    private fun canPutAtElement(
        file: VirtualFile,
        line: Int,
        project: Project,
        processor: PairFunction<in PsiElement, in Document, Boolean>
    ): Boolean {
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return false
        if (JavaClassFileType.INSTANCE != psiFile.fileType && !DebuggerUtils.isBreakpointAware(psiFile)) {
            return false
        }

        val document = FileDocumentManager.getInstance().getDocument(file)
        if (document != null) {
            val res = Ref.create(false)
            XDebuggerUtil.getInstance().iterateLine(project, document, line) { element: PsiElement? ->
                // avoid comments
                if (element is PsiWhiteSpace
                    || PsiTreeUtil.getParentOfType(
                        element,
                        PsiComment::class.java,
                        PsiImportStatementBase::class.java,
                        PsiPackageStatement::class.java
                    ) != null
                ) {
                    return@iterateLine true
                }

                var el: PsiElement? = element
                var parent = element
                while (element != null) {
                    // skip modifiers
                    if (el is PsiModifierList) {
                        el = el.getParent()
                        continue
                    }
                    val offset = el!!.textOffset
                    if (!DocumentUtil.isValidOffset(offset, document) || document.getLineNumber(offset) != line) {
                        break
                    }
                    parent = el
                    el = el.parent
                }
                if (processor.`fun`(parent, document)) {
                    res.set(true)
                    return@iterateLine false
                }
                true
            }
            return res.get()
        }
        return false
    }
}
