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
import spp.jetbrains.sourcemarker.instrument.breakpoint.ExecutionPointManager
import spp.jetbrains.sourcemarker.instrument.breakpoint.model.ActiveStackTrace
import spp.jetbrains.sourcemarker.instrument.breakpoint.model.ActiveStackTraceListener
import spp.protocol.artifact.exception.LiveStackTrace
import spp.protocol.artifact.exception.LiveStackTraceElement
import java.util.concurrent.CopyOnWriteArrayList

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class BreakpointHitTab(
    val project: Project,
    executionPointHighlighter: ExecutionPointHighlighter,
    showExecutionPoint: Boolean
) : Disposable {

    companion object {
        const val LIVE_RUNNER = "Live Runner"
    }

    private val executionPointManager = ExecutionPointManager(project, executionPointHighlighter, showExecutionPoint)
    private val listeners = CopyOnWriteArrayList<ActiveStackTraceListener>()
    private val layoutUi = RunnerLayoutUi.Factory.getInstance(project).create(
        LIVE_RUNNER, LIVE_RUNNER, LIVE_RUNNER, this
    )
    val layoutComponent = layoutUi.component
    lateinit var activeStack: ActiveStackTrace
    var content: Content? = null

    init {
        Disposer.register(this, executionPointManager)
        listeners.add(executionPointManager)

        addFramesPanel()
        addVariablesPanel()
    }

    private fun addFramesPanel() {
        val framesPanel = FramesPanel(project, this)
        val content = layoutUi.createContent(
            "Live Stack Frames", framesPanel.component, "Frames",
            AllIcons.Debugger.Frame, null
        )
        content.isCloseable = false
        layoutUi.addContent(content, 0, PlaceInGrid.left, false)
        listeners.add(framesPanel)
        Disposer.register(this, framesPanel)
    }

    private fun addVariablesPanel() {
        val variablesPanel = VariablesPanel()
        val content = layoutUi.createContent(
            "Live Variables", variablesPanel.component, "Variables",
            AllIcons.Debugger.VariablesTab, null
        )
        content.isCloseable = false
        layoutUi.addContent(content, 0, PlaceInGrid.center, false)
        listeners.add(variablesPanel)
        Disposer.register(this, variablesPanel)
    }

    fun onStackFrameUpdated() {
        ReadAction.nonBlocking {
            for (listener in listeners) {
                if (listener is VariablesPanel || listener is ExecutionPointManager) {
                    listener.onChanged(activeStack)
                }
            }
        }.submit(AppExecutorUtil.getAppExecutorService())

        if (content != null) {
            content!!.displayName = "${activeStack.currentFrame!!.source} at #${activeStack.currentFrameIndex}"
        }
    }

    fun showFrames(stackTrace: LiveStackTrace, currentFrame: LiveStackTraceElement) {
        activeStack = ActiveStackTrace(stackTrace, currentFrame)

        ReadAction.nonBlocking {
            for (listener in listeners) {
                listener.onChanged(activeStack)
            }
        }.submit(AppExecutorUtil.getAppExecutorService())
    }

    fun showExecutionLine() {
        ReadAction.nonBlocking {
            executionPointManager.onChanged(activeStack)
        }.submit(AppExecutorUtil.getAppExecutorService())
    }

    override fun dispose() = Unit
}
