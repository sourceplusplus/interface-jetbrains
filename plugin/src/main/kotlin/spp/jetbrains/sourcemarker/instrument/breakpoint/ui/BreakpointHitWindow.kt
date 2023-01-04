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

import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.content.Content
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter
import spp.jetbrains.sourcemarker.instrument.breakpoint.DebugStackFrameListener
import spp.jetbrains.sourcemarker.instrument.breakpoint.ExecutionPointManager
import spp.jetbrains.sourcemarker.instrument.breakpoint.LiveBreakpointConstants
import spp.jetbrains.sourcemarker.instrument.breakpoint.StackFrameManager
import spp.protocol.artifact.exception.LiveStackTrace
import spp.protocol.artifact.exception.LiveStackTraceElement
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.JComponent

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class BreakpointHitWindow(
    val project: Project,
    executionPointHighlighter: ExecutionPointHighlighter,
    showExecutionPoint: Boolean
) : Disposable {

    lateinit var stackFrameManager: StackFrameManager
    var content: Content? = null
    val layoutComponent: JComponent
    private val executionPointManager = ExecutionPointManager(project, executionPointHighlighter, showExecutionPoint)
    private val listeners: MutableList<DebugStackFrameListener>
    private val layoutUi: RunnerLayoutUi

    init {
        listeners = CopyOnWriteArrayList()
        layoutUi = RunnerLayoutUi.Factory.getInstance(project).create(
            LiveBreakpointConstants.LIVE_RUNNER,
            LiveBreakpointConstants.LIVE_RUNNER,
            LiveBreakpointConstants.LIVE_RUNNER,
            this
        )
        layoutComponent = layoutUi.component

        Disposer.register(this, executionPointManager)
        listeners.add(executionPointManager)

        addFramesTab()
        addVariableTab()
    }

    private fun addFramesTab() {
        val framesTab = FramesTab(project, this)
        val content = layoutUi.createContent(
            LiveBreakpointConstants.LIVE_RECORDER_STACK_FRAMES, framesTab.component, "Frames",
            AllIcons.Debugger.Frame, null
        )
        content.isCloseable = false
        layoutUi.addContent(content, 0, PlaceInGrid.left, false)
        listeners.add(framesTab)
        Disposer.register(this, framesTab)
    }

    private fun addVariableTab() {
        val variableTab = VariableTab()
        val content = layoutUi.createContent(
            LiveBreakpointConstants.LIVE_RECORDER_VARIABLES, variableTab.component, "Variables",
            AllIcons.Debugger.VariablesTab, null
        )
        content.isCloseable = false
        layoutUi.addContent(content, 0, PlaceInGrid.center, false)
        listeners.add(variableTab)
        Disposer.register(this, variableTab)
    }

    fun onStackFrameUpdated() {
        ReadAction.nonBlocking {
            for (listener in listeners) {
                if (listener is VariableTab || listener is ExecutionPointManager) {
                    listener.onChanged(stackFrameManager)
                }
            }
        }.submit(AppExecutorUtil.getAppExecutorService())

        if (content != null) {
            content!!.displayName =
                "${stackFrameManager.currentFrame!!.source} at #${stackFrameManager.currentFrameIndex}"
        }
    }

    fun showFrames(stackTrace: LiveStackTrace, currentFrame: LiveStackTraceElement) {
        stackFrameManager = StackFrameManager(stackTrace)
        stackFrameManager.currentFrame = currentFrame

        ReadAction.nonBlocking {
            for (listener in listeners) {
                listener.onChanged(stackFrameManager)
            }
        }.submit(AppExecutorUtil.getAppExecutorService())
    }

    fun showExecutionLine() {
        ReadAction.nonBlocking {
            executionPointManager.onChanged(stackFrameManager)
        }.submit(AppExecutorUtil.getAppExecutorService())
    }

    override fun dispose() = Unit
}
