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
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ListTableModel
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import spp.jetbrains.UserData
import spp.jetbrains.sourcemarker.view.trace.column.TraceRowColumnInfo
import spp.jetbrains.sourcemarker.view.trace.renderer.TraceDurationTableCellRenderer
import spp.jetbrains.sourcemarker.view.trace.renderer.TraceErrorTableCellRenderer
import spp.jetbrains.view.LiveViewTraceManager
import spp.jetbrains.view.window.LiveTraceWindow
import spp.protocol.artifact.trace.Trace
import spp.protocol.view.LiveView
import java.awt.BorderLayout
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.Duration
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.SortOrder

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
    private var consumer: MessageConsumer<JsonObject>? = null
    override var isRunning = false
        private set

    private val model = ListTableModel<Trace>(
        arrayOf(
            TraceRowColumnInfo("Trace"),
            TraceRowColumnInfo("Duration"),
            TraceRowColumnInfo("Time"),
            TraceRowColumnInfo("Status")
        ),
        ArrayList(), 2, SortOrder.DESCENDING
    )
    val component: JPanel = JPanel(BorderLayout())

    init {
        val table = JBTable(model)
//        table.isStriped = true
        table.setShowColumns(true)
        table.setDefaultRenderer(Duration::class.java, TraceDurationTableCellRenderer())
        table.setDefaultRenderer(Icon::class.java, TraceErrorTableCellRenderer())
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(mouseEvent: MouseEvent) {
                val point: Point = mouseEvent.point
                val row = table.rowAtPoint(point)
                if (mouseEvent.clickCount == 2 && row >= 0) {
                    val traceRow = model.items[table.rowSorter.convertRowIndexToModel(row)]
                    LiveViewTraceManager.getInstance(project).showTraceSpans(traceRow)
                }
            }
        })
        component.add(JBScrollPane(table), "Center")

        //default column widths
        table.columnModel.getColumn(2).maxWidth = 175
        table.columnModel.getColumn(2).minWidth = 125
        table.columnModel.getColumn(3).maxWidth = 150
        table.columnModel.getColumn(3).minWidth = 100

        resume()
    }

    override fun addTrace(trace: Trace) {
        model.addRow(trace)
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
