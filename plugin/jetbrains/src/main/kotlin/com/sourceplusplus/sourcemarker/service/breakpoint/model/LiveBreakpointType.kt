package com.sourceplusplus.sourcemarker.service.breakpoint.model

import com.intellij.debugger.engine.DebuggerUtils
import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.intellij.debugger.ui.breakpoints.JavaBreakpointType
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.DocumentUtil
import com.intellij.util.PairFunction
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.sourceplusplus.protocol.SourceMarkerServices.Instance
import com.sourceplusplus.protocol.instrument.LiveSourceLocation
import com.sourceplusplus.sourcemarker.icons.SourceMarkerIcons
import com.sourceplusplus.sourcemarker.service.breakpoint.BreakpointTriggerListener
import org.jetbrains.java.debugger.JavaDebuggerEditorsProvider
import org.jetbrains.java.debugger.breakpoints.JavaBreakpointFiltersPanel
import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveBreakpointType : XLineBreakpointType<LiveBreakpointProperties>(
    "live-breakpoint", "Live Breakpoint"
), JavaBreakpointType<LiveBreakpointProperties> {

    override fun createCustomRightPropertiesPanel(project: Project):
            XBreakpointCustomPropertiesPanel<XLineBreakpoint<LiveBreakpointProperties>> {
        return JavaBreakpointFiltersPanel(project)
    }

    override fun getEditorsProvider(
        breakpoint: XLineBreakpoint<LiveBreakpointProperties>, project: Project
    ): XDebuggerEditorsProvider {
        return JavaDebuggerEditorsProvider()
    }

    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
        if (Instance.liveInstrument == null) return false
        return canPutAtElement(file, line, project) { element: PsiElement, _: Document ->
            element !is PsiMethod && element !is PsiField
        }
    }

    override fun getEnabledIcon(): Icon {
        return SourceMarkerIcons.LIVE_BREAKPOINT_ACTIVE_ICON
    }

    override fun getDisabledIcon(): Icon {
        return SourceMarkerIcons.LIVE_BREAKPOINT_ERROR_ICON
    }

    override fun createBreakpointProperties(file: VirtualFile, line: Int): LiveBreakpointProperties {
        val props = LiveBreakpointProperties()
        val psiFile =
            (PsiManager.getInstance(ProjectManager.getInstance().openProjects[0]).findFile(file) as PsiClassOwner)
        val qualifiedName = psiFile.classes[0].qualifiedName!!
        props.setLocation(LiveSourceLocation(qualifiedName, line + 1))
        props.setSuspend(!BreakpointTriggerListener.shiftHeld)
        return props
    }

    override fun createJavaBreakpoint(
        project: Project, breakpoint: XBreakpoint<LiveBreakpointProperties>
    ): Breakpoint<LiveBreakpointProperties> {
        return LiveLineBreakpoint(project, breakpoint)
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

    override fun isSuspendThreadSupported(): Boolean {
        return false
    }
}
