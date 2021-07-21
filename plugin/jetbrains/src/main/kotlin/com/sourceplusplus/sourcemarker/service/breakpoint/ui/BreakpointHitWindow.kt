package com.sourceplusplus.sourcemarker.service.breakpoint.ui

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
import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import com.sourceplusplus.protocol.artifact.exception.JvmStackTraceElement
import com.sourceplusplus.sourcemarker.service.breakpoint.DebugStackFrameListener
import com.sourceplusplus.sourcemarker.service.breakpoint.ExecutionPointManager
import com.sourceplusplus.sourcemarker.service.breakpoint.LiveBreakpointConstants
import com.sourceplusplus.sourcemarker.service.breakpoint.StackFrameManager
import com.sourceplusplus.sourcemarker.settings.SourceMarkerConfig
import io.vertx.core.json.Json
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.JComponent

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class BreakpointHitWindow(project: Project, executionPointHighlighter: ExecutionPointHighlighter) : Disposable {

    lateinit var stackFrameManager: StackFrameManager
    var content: Content? = null
    val layoutComponent: JComponent
    private val executionPointManager = ExecutionPointManager(project, executionPointHighlighter)
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

    fun showFrames(stackTrace: JvmStackTrace, currentFrame: JvmStackTraceElement) {
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

    override fun dispose() {}
}
