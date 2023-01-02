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
package spp.jetbrains.sourcemarker.view.window

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTreeTable
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import spp.jetbrains.UserData
import spp.jetbrains.sourcemarker.view.trace.LiveViewTraceModel
import spp.jetbrains.sourcemarker.view.trace.LiveViewTraceRowSorter
import spp.jetbrains.sourcemarker.view.trace.LiveViewTraceTreeStructure
import spp.jetbrains.sourcemarker.view.trace.node.TraceListRootNode
import spp.jetbrains.sourcemarker.view.trace.renderer.TraceDurationTableCellRenderer
import spp.jetbrains.view.window.LiveTraceWindow
import spp.protocol.artifact.trace.Trace
import spp.protocol.view.LiveView
import java.awt.BorderLayout
import javax.swing.*

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewTraceWindowImpl(
    project: Project,
    override var liveView: LiveView,
    private val consumerCreator: (LiveTraceWindow) -> MessageConsumer<JsonObject>
) : LiveTraceWindow {

    private val log = logger<LiveViewTraceWindowImpl>()
    private val viewService = UserData.liveViewService(project)!!
    val layoutComponent: JComponent
        get() = component

    private var consumer: MessageConsumer<JsonObject>? = null
    override var isRunning = false
        private set

    val component: JPanel = JPanel(BorderLayout())
    private val rootNode = TraceListRootNode(project)
    private val model = LiveViewTraceModel(LiveViewTraceTreeStructure(project, rootNode))

    init {
        Disposer.register(this, model)
        val table = JBTreeTable(model)
        val rowSorter = LiveViewTraceRowSorter(table, model)
        table.setRowSorter(rowSorter)
        val sortKey = RowSorter.SortKey(
            LiveViewTraceModel.COLUMN_INFOS.indexOfFirst { it.name == "Time" },
            SortOrder.DESCENDING
        )
        rowSorter.sortKeys = listOf(sortKey)
        table.setDefaultRenderer(Icon::class.java, TraceDurationTableCellRenderer())
        component.add(table, "Center")

        resume()
    }

    override fun addTrace(trace: Trace) {
        rootNode.traces.add(trace)
        model.reset() //todo: optimize
    }

    override fun resume() {
        if (isRunning) return
        isRunning = true
        viewService.addLiveView(liveView).onSuccess {
            liveView = it
            consumer = consumerCreator.invoke(this)
        }.onFailure {
            log.error("Failed to resume live view", it)
        }
    }

    override fun pause() {
        if (!isRunning) return
        isRunning = false
        consumer?.unregister()
        consumer = null
        liveView.subscriptionId?.let {
            viewService.removeLiveView(it).onFailure {
                log.error("Failed to pause live view", it)
            }
        }
    }

    override fun dispose() = Unit
}
