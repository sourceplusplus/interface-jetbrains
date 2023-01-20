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
package spp.jetbrains.sourcemarker.instrument

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.sourcemarker.instrument.breakpoint.BreakpointHitWindow
import spp.jetbrains.sourcemarker.instrument.ui.InstrumentEventTab
import spp.jetbrains.sourcemarker.instrument.ui.InstrumentOverviewTab
import spp.jetbrains.sourcemarker.instrument.ui.action.ClearInstrumentsAction
import spp.jetbrains.sourcemarker.instrument.ui.action.RemoveInstrumentAction
import spp.jetbrains.sourcemarker.instrument.ui.model.InstrumentOverview
import spp.jetbrains.status.SourceStatusListener
import spp.protocol.instrument.event.LiveBreakpointHit
import spp.protocol.instrument.event.LiveInstrumentEvent

/**
 * todo: description.
 *
 * @since 0.7.7
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class InstrumentEventWindowService(val project: Project) : Disposable {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): InstrumentEventWindowService {
            return project.getService(InstrumentEventWindowService::class.java)
        }
    }

    private val contentFactory = ApplicationManager.getApplication().getService(ContentFactory::class.java)
    private var toolWindow = ToolWindowManager.getInstance(project)
        .registerToolWindow(RegisterToolWindowTask.closable("Live Instruments", PluginIcons.ToolWindow.satelliteDish))
    private var contentManager = toolWindow.contentManager
    private val executionPointHighlighter = ExecutionPointHighlighter(project)
    private lateinit var hitWindow: BreakpointHitWindow
    private lateinit var overviewTab: InstrumentOverviewTab
    val selectedTab: Disposable?
        get() = contentManager.selectedContent?.disposer
    val selectedInstrumentOverview: InstrumentOverview?
        get() {
            return when (val selectedTab = selectedTab) {
                is InstrumentOverviewTab -> return selectedTab.selectedInstrumentOverview
                is InstrumentEventTab -> return selectedTab.overview
                else -> null
            }
        }
    val allOverviews: List<InstrumentOverview>
        get() {
            return overviewTab.model.items
        }

    init {
        project.messageBus.connect().subscribe(SourceStatusListener.TOPIC, SourceStatusListener {
            if (it.disposedPlugin) {
                ApplicationManager.getApplication().invokeLater {
                    hideWindows()
                }
            }
        })
        contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                val disposable = event.content.disposer
                if (disposable is BreakpointHitWindow) {
                    if (event.operation == ContentManagerEvent.ContentOperation.add) {
                        disposable.showExecutionLine()
                    }
                }
            }
        })
        Disposer.register(this, contentManager)

        toolWindow.setTitleActions(
            listOf(
                RemoveInstrumentAction(this),
                ClearInstrumentsAction(this)
            )
        )
    }

    fun makeOverviewTab() {
        overviewTab = InstrumentOverviewTab(project)
        val content = contentFactory.createContent(overviewTab.component, "Overview", true)
        content.setDisposer(overviewTab)
        content.isCloseable = false
        contentManager.addContent(content)
    }

    fun selectInOverviewTab(instrumentId: String) {
        contentManager.setSelectedContent(contentManager.contents.first(), true)
        toolWindow.show()

        val selectedIndex = overviewTab.model.items.indexOfFirst { it.instrumentId == instrumentId }
        if (selectedIndex != -1) {
            val selectRow = overviewTab.table.convertRowIndexToView(selectedIndex)
            overviewTab.table.setRowSelectionInterval(selectRow, selectRow)
        }
        overviewTab.table.grabFocus()
    }

    private fun hideWindows() {
        contentManager.contents.forEach { content ->
            contentManager.removeContent(content, true)
        }
    }

    fun addInstrumentEvent(event: LiveInstrumentEvent) {
        val existingOverview = overviewTab.model.items.find { it.instrumentId == event.instrument.id }
        if (existingOverview != null) {
            existingOverview.events.add(event)
            overviewTab.component.repaint()
        } else {
            overviewTab.model.insertRow(0, InstrumentOverview(mutableListOf(event)))
        }

        val max = 1000
        if (overviewTab.model.items.size > max) {
            overviewTab.model.items.removeAt(0)
        }

        //also add to events window (if open)
        contentManager.contents.forEach {
            if (it.disposer is InstrumentEventTab) {
                val eventTab = it.disposer as InstrumentEventTab
                if (eventTab.overview.instrumentId == event.instrument.id) {
                    eventTab.model.insertRow(0, event)
                    if (eventTab.model.items.size > max) {
                        eventTab.model.items.removeAt(0)
                    }
                }
            }
        }
    }

    fun showInstrumentEvents(overview: InstrumentOverview) {
        //if already open, select it
        val existingContent = contentManager.contents.find {
            (it.disposer as? InstrumentEventTab)?.overview?.instrumentId == overview.instrumentId
        }
        if (existingContent != null) {
            contentManager.setSelectedContent(existingContent, true)
            return
        }

        //otherwise, create new tab
        val eventTab = InstrumentEventTab(project, overview)
        val content = contentFactory.createContent(
            eventTab.component, overview.instrumentTypeFormatted + ": " + overview.source, false
        )
        content.setDisposer(eventTab)
        content.isCloseable = true
        contentManager.addContent(content)
        contentManager.setSelectedContent(content)
    }

    fun showInstrumentEvents(instrumentId: String) {
        val overview = overviewTab.model.items.find { it.instrumentId == instrumentId }
        if (overview != null) {
            showInstrumentEvents(overview)
            toolWindow.show()
        }
    }

    fun showBreakpointHit(hit: LiveBreakpointHit, showExecutionPoint: Boolean = true) {
        if (showExecutionPoint) removeExecutionShower()
        hitWindow = BreakpointHitWindow(project, executionPointHighlighter, showExecutionPoint)

        //grab first non-skywalking frame and add real variables from skywalking frame
        val firstFrame = hit.stackTrace.first()
        val firstNonSkyWalkingFrame = hit.stackTrace.getElements(true).first()
        if (firstFrame != firstNonSkyWalkingFrame) {
            firstNonSkyWalkingFrame.variables.addAll(hit.stackTrace.first().variables)
        }

        hitWindow.showFrames(hit.stackTrace, firstNonSkyWalkingFrame)
        val content = contentFactory.createContent(
            hitWindow.layoutComponent, firstNonSkyWalkingFrame.source + " at #0", false
        )
        content.setDisposer(hitWindow)
        hitWindow.content = content
        contentManager.addContent(content)
        contentManager.setSelectedContent(content)
        toolWindow.setAvailable(true, null)
        if (toolWindow.isActive) {
            toolWindow.show(null)
        } else {
            toolWindow.activate(null)
        }
    }

    private fun removeExecutionShower() {
        executionPointHighlighter.hide()
    }

    fun getHitWindow(): BreakpointHitWindow? {
        return if (::hitWindow.isInitialized) {
            hitWindow
        } else {
            null
        }
    }

    override fun dispose() = Unit
}
