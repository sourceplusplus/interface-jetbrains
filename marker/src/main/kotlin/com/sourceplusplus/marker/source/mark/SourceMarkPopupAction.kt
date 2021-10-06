package com.sourceplusplus.marker.source.mark

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.ClassSourceMark
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import org.jetbrains.annotations.NotNull
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class SourceMarkPopupAction : AnAction() {

    private val log = LoggerFactory.getLogger(SourceMarkPopupAction::class.java)

    override fun update(@NotNull e: AnActionEvent) {
        if (!SourceMarker.enabled) {
            log.warn("SourceMarker popup action disabled")
            return
        }

        val project: Project? = e.project
        e.presentation.isEnabledAndVisible = project != null
    }

    override fun actionPerformed(@NotNull e: AnActionEvent) {
        val project: Project? = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        if (project != null && editor != null && psiFile != null) {
            var classSourceMark: ClassSourceMark? = null
            var sourceMark: SourceMark? = null
            val fileMarker = psiFile.getUserData(SourceFileMarker.KEY)
            if (fileMarker != null) {
                sourceMark = fileMarker.getSourceMarks().find {
                    if (it is ClassSourceMark) {
                        classSourceMark = it //todo: probably doesn't handle inner classes well
                        false
                    } else if (it is MethodSourceMark) {
                        if (it.configuration.activateOnKeyboardShortcut) {
                            //+1 on end offset so match is made even right after method end
                            val incTextRange = TextRange(
                                it.psiMethod.textRange.startOffset,
                                it.psiMethod.textRange.endOffset + 1
                            )
                            incTextRange.contains(editor.logicalPositionToOffset(editor.caretModel.logicalPosition))
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
            }

            if (sourceMark != null) {
                performPopupAction(sourceMark, editor)
            } else if (classSourceMark != null) {
                performPopupAction(classSourceMark!!, editor)
            }
        }
    }

    open fun performPopupAction(sourceMark: SourceMark, editor: Editor) {
        sourceMark.triggerEvent(SourceMarkEvent(sourceMark, SourceMarkEventCode.PORTAL_OPENING)) {
            sourceMark.displayPopup(editor)
        }
    }
}
