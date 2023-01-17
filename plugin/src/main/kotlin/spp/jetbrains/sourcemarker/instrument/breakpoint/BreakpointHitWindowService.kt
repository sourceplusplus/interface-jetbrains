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
package spp.jetbrains.sourcemarker.instrument.breakpoint

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.sourcemarker.instrument.breakpoint.LiveBreakpointConstants.LIVE_BREAKPOINT_NAME
import spp.jetbrains.sourcemarker.instrument.breakpoint.ui.BreakpointHitWindow
import spp.jetbrains.sourcemarker.instrument.breakpoint.ui.EventsWindow
import spp.protocol.instrument.event.LiveBreakpointHit

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class BreakpointHitWindowService(private val project: Project) : Disposable {

    companion object {
        fun getInstance(project: Project): BreakpointHitWindowService {
            return project.getService(BreakpointHitWindowService::class.java)
        }
    }

    private val executionPointHighlighter: ExecutionPointHighlighter = ExecutionPointHighlighter(project)
    private var _toolWindow: ToolWindow? = null
    private var contentManager: ContentManager? = null
    private lateinit var breakpointWindow: BreakpointHitWindow
    lateinit var eventsWindow: EventsWindow

    init {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow(LIVE_BREAKPOINT_NAME)
        if (toolWindow != null) {
            _toolWindow = toolWindow
        } else {
            _toolWindow = toolWindowManager.registerToolWindow(
                RegisterToolWindowTask.closable(LIVE_BREAKPOINT_NAME, PluginIcons.ToolWindow.satelliteDish)
            )

            _toolWindow!!.contentManager.addContentManagerListener(object : ContentManagerListener {
                override fun contentAdded(contentManagerEvent: ContentManagerEvent) = Unit
                override fun contentRemoved(event: ContentManagerEvent) {
                    if (_toolWindow!!.contentManager.contentCount == 0) {
                        _toolWindow!!.setAvailable(false, null)
                    }
                }

                override fun contentRemoveQuery(contentManagerEvent: ContentManagerEvent) = Unit
                override fun selectionChanged(event: ContentManagerEvent) {
                    val disposable = event.content.disposer
                    if (disposable is BreakpointHitWindow) {
                        if (event.operation == ContentManagerEvent.ContentOperation.add) {
                            disposable.showExecutionLine()
                        }
                    }
                }
            })
        }
        contentManager = _toolWindow!!.contentManager
    }

    fun showEventsWindow() {
        eventsWindow = EventsWindow(project)
        val content = ContentFactory.SERVICE.getInstance().createContent(eventsWindow.layoutComponent, "Events", true)
        content.setDisposer(eventsWindow)
        content.isCloseable = false
        contentManager!!.addContent(content)
    }

    fun clearContent() {
        contentManager!!.removeAllContents(true)
    }

    fun addBreakpointHit(hit: LiveBreakpointHit) {
        eventsWindow.eventsTab.model.insertRow(0, hit)
        val max = 1000
        if (eventsWindow.eventsTab.model.items.size > max) {
            eventsWindow.eventsTab.model.items.removeAt(0)
        }
    }

    fun showBreakpointHit(hit: LiveBreakpointHit, showExecutionPoint: Boolean = true) {
        if (showExecutionPoint) removeExecutionShower()
        breakpointWindow = BreakpointHitWindow(project, executionPointHighlighter, showExecutionPoint)

        //grab first non-skywalking frame and add real variables from skywalking frame
        val firstFrame = hit.stackTrace.first()
        val firstNonSkyWalkingFrame = hit.stackTrace.getElements(true).first()
        if (firstFrame != firstNonSkyWalkingFrame) {
            firstNonSkyWalkingFrame.variables.addAll(hit.stackTrace.first().variables)
        }

        breakpointWindow.showFrames(hit.stackTrace, firstNonSkyWalkingFrame)
        val content = ContentFactory.SERVICE.getInstance().createContent(
            breakpointWindow.layoutComponent, firstNonSkyWalkingFrame.source + " at #0", false
        )
        content.setDisposer(breakpointWindow)
        breakpointWindow.content = content
        contentManager!!.addContent(content)
        contentManager!!.setSelectedContent(content)
        _toolWindow!!.setAvailable(true, null)
        if (_toolWindow!!.isActive) {
            _toolWindow!!.show(null)
        } else {
            _toolWindow!!.activate(null)
        }
    }

    private fun removeExecutionShower() {
        executionPointHighlighter.hide()
    }

    fun getBreakpointWindow(): BreakpointHitWindow? {
        return if (::breakpointWindow.isInitialized) {
            breakpointWindow
        } else {
            null
        }
    }

    override fun dispose() = Unit
}
