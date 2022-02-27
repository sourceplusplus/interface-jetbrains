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
package spp.jetbrains.sourcemarker.service.breakpoint.ui

import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import spp.jetbrains.sourcemarker.service.breakpoint.DebugStackFrameListener
import spp.jetbrains.sourcemarker.service.breakpoint.LiveBreakpointConstants
import spp.jetbrains.sourcemarker.service.breakpoint.StackFrameManager
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.JComponent

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class EventsWindow(project: Project) : Disposable {

    lateinit var stackFrameManager: StackFrameManager
    private val listeners: MutableList<DebugStackFrameListener>
    private val layoutUi: RunnerLayoutUi
    lateinit var eventsTab: EventsTab

    private fun addEventsTab() {
        eventsTab = EventsTab()
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
