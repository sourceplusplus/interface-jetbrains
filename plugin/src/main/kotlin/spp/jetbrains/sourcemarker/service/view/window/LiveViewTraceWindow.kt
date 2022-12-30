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
package spp.jetbrains.sourcemarker.service.view.window

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTreeTable
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import spp.jetbrains.ScopeExtensions
import spp.jetbrains.UserData
import spp.jetbrains.sourcemarker.service.view.trace.CallUsageTableCellRenderer
import spp.jetbrains.sourcemarker.service.view.trace.LiveViewTraceModel
import spp.jetbrains.sourcemarker.service.view.trace.LiveViewTraceRowSorter
import spp.jetbrains.sourcemarker.service.view.trace.LiveViewTraceTreeStructure
import spp.jetbrains.sourcemarker.service.view.trace.node.TraceListRootNode
import spp.protocol.artifact.trace.Trace
import spp.protocol.service.SourceServices.Subscribe.toLiveViewSubscriberAddress
import spp.protocol.view.LiveView
import spp.protocol.view.LiveViewConfig
import spp.protocol.view.LiveViewEvent
import java.awt.BorderLayout
import javax.swing.*

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewTraceWindow(val project: Project) : Disposable {

    val layoutComponent: JComponent
        get() = component

    val component: JPanel = JPanel(BorderLayout())
    private val rootNode = TraceListRootNode(project)
    private val model = LiveViewTraceModel(
        LiveViewTraceTreeStructure(project, rootNode)
    )

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
        table.setDefaultRenderer(Icon::class.java, CallUsageTableCellRenderer())
        component.add(table, "Center")

        val vertx = UserData.vertx(project)

//        vertx.setPeriodic(2000) {
//            val trace = Trace(
//                "key" + System.currentTimeMillis(),
//                listOf("operationNames" + System.currentTimeMillis()),
//                1,
//                Instant.now(),
//                false,
//                listOf("traceIds" + System.currentTimeMillis()),
//                false,
//                "segmentId" + System.currentTimeMillis(),
//                mutableMapOf()
//            )
//            rootNode.traces.add(trace)
//            rootNode.children.add(TraceListNode(project, trace))
//            model.reset()
//        }

        ScopeExtensions.safeRunBlocking(vertx.dispatcher()) {
            val refreshRate = 2000
            val liveView = UserData.liveViewService(project)!!.addLiveView(
                LiveView(
                    entityIds = mutableSetOf("GET:/high-load-endpoint"),
                    viewConfig = LiveViewConfig("TRACE_VIEW", listOf("endpoint_traces"), refreshRate)
                )
            ).await()

            val consumer = vertx.eventBus().consumer<JsonObject>(
                toLiveViewSubscriberAddress("system")
            )
            consumer.handler {
                val liveViewEvent = LiveViewEvent(it.body())
                if (liveView.subscriptionId != liveViewEvent.subscriptionId) return@handler
                println(liveViewEvent)
                val event = JsonObject(liveViewEvent.metricsData)
                val trace = Trace(event.getJsonObject("trace"))
                rootNode.traces.add(trace)
                model.reset()
            }
        }
    }

    override fun dispose() = Unit
}
