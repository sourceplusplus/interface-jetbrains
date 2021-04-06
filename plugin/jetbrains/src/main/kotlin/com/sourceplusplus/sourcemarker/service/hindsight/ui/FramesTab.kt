package com.sourceplusplus.sourcemarker.service.hindsight.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.sourceplusplus.protocol.artifact.exception.JvmStackTraceElement
import com.sourceplusplus.protocol.artifact.exception.sourceAsLineNumber
import com.sourceplusplus.sourcemarker.service.hindsight.DebugStackFrameListener
import com.sourceplusplus.sourcemarker.service.hindsight.StackFrameManager
import java.awt.BorderLayout
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionEvent

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class FramesTab(private val tab: BreakpointHitWindow) : DebugStackFrameListener, Disposable {

    val component: JPanel = JPanel(BorderLayout())
    private val stackFrameList: JList<JvmStackTraceElement>

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
        stackFrameList.cellRenderer = object : ColoredListCellRenderer<JvmStackTraceElement>() {
            override fun customizeCellRenderer(
                jList: JList<out JvmStackTraceElement>,
                frame: JvmStackTraceElement,
                i: Int,
                b: Boolean,
                b1: Boolean
            ) {
                this.icon = AllIcons.Debugger.Frame
                if (frame.method.startsWith("spp.example") && frame.sourceAsLineNumber() != null) { //todo: dynamic
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

    override fun dispose() {}
}
