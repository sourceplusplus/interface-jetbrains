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
package spp.jetbrains.sourcemarker.instrument.breakpoint.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import spp.jetbrains.marker.service.ArtifactNamingService
import spp.jetbrains.sourcemarker.instrument.breakpoint.DebugStackFrameListener
import spp.jetbrains.sourcemarker.instrument.breakpoint.StackFrameManager
import spp.protocol.artifact.exception.LiveStackTraceElement
import spp.protocol.artifact.exception.sourceAsLineNumber
import java.awt.BorderLayout
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionEvent

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class FramesTab(
    private val project: Project,
    private val tab: BreakpointHitWindow,
) : DebugStackFrameListener, Disposable {

    val component: JPanel = JPanel(BorderLayout())
    private val stackFrameList: JList<LiveStackTraceElement>
    private var stackFrameManager: StackFrameManager? = null

    init {
        stackFrameList = JBList()
        stackFrameList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        addListeners()
        component.add(JBScrollPane(stackFrameList), "Center")
    }

    override fun onChanged(stackFrameManager: StackFrameManager) {
        this.stackFrameManager = stackFrameManager

        val currentFrame = stackFrameManager.currentFrame
        stackFrameList.model = CollectionListModel(stackFrameManager.stackTrace.getElements(true))
        stackFrameList.setSelectedValue(currentFrame, true)
    }

    private fun addListeners() {
        stackFrameList.cellRenderer = object : ColoredListCellRenderer<LiveStackTraceElement>() {
            override fun customizeCellRenderer(
                jList: JList<out LiveStackTraceElement>,
                frame: LiveStackTraceElement,
                i: Int,
                b: Boolean,
                b1: Boolean
            ) {
                this.icon = AllIcons.Debugger.Frame

                val psiFile = stackFrameManager?.stackTrace?.language?.let {
                    ArtifactNamingService.getService(it).findPsiFile(it, project, frame)
                }
                if (psiFile?.isWritable == true && frame.sourceAsLineNumber() != null) {
                    this.append(frame.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                } else {
                    this.append(frame.toString(), SimpleTextAttributes.GRAY_ATTRIBUTES)
                }
            }
        }
        stackFrameList.addListSelectionListener { event: ListSelectionEvent ->
            if (!event.valueIsAdjusting) {
                val stackFrame = stackFrameList.selectedValue
                if (stackFrame != null) {
                    stackFrameList.setSelectedValue(stackFrame, true)
                    tab.stackFrameManager.currentFrame = stackFrame
                    tab.stackFrameManager.currentFrameIndex = stackFrameList.selectedIndex
                    tab.onStackFrameUpdated()
                }
            }
        }
    }

    override fun dispose() = Unit
}
