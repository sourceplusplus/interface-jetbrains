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
package spp.jetbrains.marker.source.mark

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.annotations.NotNull
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.ClassSourceMark
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode

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
            log.warn("SourceMarker disabled. Ignoring popup action.")
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
        sourceMark.triggerEvent(SourceMarkEvent(sourceMark, SourceMarkEventCode.PORTAL_OPENING))
    }
}
