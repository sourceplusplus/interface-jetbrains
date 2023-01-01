/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022 CodeBrig, Inc.
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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import spp.jetbrains.sourcemarker.instrument.breakpoint.DebugStackFrameListener
import spp.jetbrains.sourcemarker.instrument.breakpoint.LiveBreakpointConstants
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.JComponent

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class EventsWindow(val project: Project) : Disposable {

    private val listeners: MutableList<DebugStackFrameListener>
    private val layoutUi: RunnerLayoutUi
    lateinit var eventsTab: EventsTab

    private fun addEventsTab() {
        eventsTab = EventsTab(project)
        val content = layoutUi.createContent(
            LiveBreakpointConstants.LIVE_RECORDER_STACK_FRAMES, eventsTab.component, "Events",
            AllIcons.Debugger.Console, null
        )
        content.isCloseable = false
        layoutUi.addContent(content, 0, PlaceInGrid.left, false)
        Disposer.register(this, eventsTab)
    }

    val layoutComponent: JComponent
        get() = layoutUi.component

    override fun dispose() = Unit

    init {
        listeners = CopyOnWriteArrayList()
        layoutUi = RunnerLayoutUi.Factory.getInstance(project).create(
            LiveBreakpointConstants.LIVE_RUNNER,
            LiveBreakpointConstants.LIVE_RUNNER,
            LiveBreakpointConstants.LIVE_RUNNER,
            this
        )
        addEventsTab()
    }
}
