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

import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.content.Content
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter
import io.vertx.core.json.Json
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.DebugStackFrameListener
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.ExecutionPointManager
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.LiveBreakpointConstants
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.StackFrameManager
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
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
    project: Project,
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
        val projectSettings = PropertiesComponent.getInstance(ProjectManager.getInstance().openProjects[0])
        val config = Json.decodeValue(
            projectSettings.getValue("sourcemarker_plugin_config"), SourceMarkerConfig::class.java
        )
        val framesTab = FramesTab(this, config)
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