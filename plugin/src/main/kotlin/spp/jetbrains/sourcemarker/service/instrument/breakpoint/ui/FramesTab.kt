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
package spp.jetbrains.sourcemarker.service.instrument.breakpoint.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.DebugStackFrameListener
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.StackFrameManager
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
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
    private val tab: BreakpointHitWindow,
    private val config: SourceMarkerConfig
) : DebugStackFrameListener, Disposable {

    val component: JPanel = JPanel(BorderLayout())
    private val stackFrameList: JList<LiveStackTraceElement>

    init {
        stackFrameList = JBList()
        stackFrameList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        addListeners()
        component.add(JBScrollPane(stackFrameList), "Center")
    }

    override fun onChanged(stackFrameManager: StackFrameManager) {
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
                if (config.rootSourcePackages.any { frame.method.startsWith(it) } &&
                    frame.sourceAsLineNumber() != null) {
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
