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

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ListTableModel
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import spp.jetbrains.UserData
import spp.jetbrains.safeLaunch
import spp.jetbrains.sourcemarker.service.view.LiveViewChartService
import spp.jetbrains.sourcemarker.service.view.model.ServiceEndpointRow
import spp.jetbrains.sourcemarker.service.view.trace.column.ServiceEndpointColumnInfo
import spp.protocol.artifact.metrics.MetricType.Companion.Endpoint_CPM
import spp.protocol.artifact.metrics.MetricType.Companion.Endpoint_RespTime_AVG
import spp.protocol.artifact.metrics.MetricType.Companion.Endpoint_SLA
import spp.protocol.instrument.LiveSourceLocation
import spp.protocol.platform.general.Service
import spp.protocol.service.LiveViewService
import spp.protocol.service.SourceServices.Subscribe.toLiveViewSubscriberAddress
import spp.protocol.view.LiveView
import spp.protocol.view.LiveViewConfig
import spp.protocol.view.LiveViewEvent
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SortOrder

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveEndpointsWindow(project: Project, service: Service) : LiveViewChartWindow(project) {

    companion object {
        private val log = logger<LiveEndpointsWindow>()
    }

    override val layoutComponent: JComponent
        get() = component

    val component: JPanel = JPanel(BorderLayout())
    val model: ListTableModel<ServiceEndpointRow> = ListTableModel<ServiceEndpointRow>(
        arrayOf(
            ServiceEndpointColumnInfo("Name"),
            ServiceEndpointColumnInfo("Load"),
            ServiceEndpointColumnInfo("Success Rate"),
            ServiceEndpointColumnInfo("Response Time")
        ),
        ArrayList(), 0, SortOrder.DESCENDING
    )

    init {
        val table = JBTable(model)
        table.isStriped = true
        table.setShowColumns(true)
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(mouseEvent: MouseEvent) {
                val row = table.rowAtPoint(mouseEvent.point)
                if (mouseEvent.clickCount == 2 && row >= 0) {
                    val endpointRow = model.items[table.rowSorter.convertRowIndexToModel(row)]
                    LiveViewChartService.getInstance(project).doThing(endpointRow.endpoint.name)
                }
            }
        })
        component.add(JBScrollPane(table), "Center")

        val vertx = UserData.vertx(project)
        val viewService = UserData.liveViewService(project)
//        ServiceBridge.currentServiceConsumer(vertx).handler {
//            val service = it.body()
        vertx.safeLaunch {
            UserData.liveManagementService(project)?.getEndpoints(service.id)?.await()?.forEach {
                val endpointRow = ServiceEndpointRow(it)
                model.addRow(endpointRow)
                viewService?.let { viewService -> addView(vertx, viewService, service, endpointRow) }
            }
//            }
        }
    }

    private fun addView(vertx: Vertx, viewService: LiveViewService, service: Service, endpoint: ServiceEndpointRow) {
        val listenMetrics = listOf(
            Endpoint_CPM.metricId,
            Endpoint_RespTime_AVG.metricId,
            Endpoint_SLA.metricId
        )
        viewService.addLiveView(
            LiveView(
                null,
                mutableSetOf(endpoint.endpoint.name),
                null,
                LiveSourceLocation("", 0, service.id),
                LiveViewConfig("LiveEndpointsWindow", listenMetrics, -1)
            )
        ).onComplete {
            if (it.succeeded()) {
                val subscriptionId = it.result().subscriptionId!!
                vertx.eventBus().consumer<JsonObject>(toLiveViewSubscriberAddress(subscriptionId)) {
                    val viewEvent = LiveViewEvent(it.body())
                    if (viewEvent.subscriptionId != subscriptionId) return@consumer

                    val metricArr = JsonArray(viewEvent.metricsData)
                    for (i in 0 until metricArr.size()) {
                        val metric = metricArr.getJsonObject(i)
                        when (metric.getJsonObject("meta").getString("metricsName")) {
                            Endpoint_CPM.metricId -> endpoint.cpm = metric.getInteger("value")
                            Endpoint_RespTime_AVG.metricId -> endpoint.respTimeAvg = metric.getInteger("value")
                            Endpoint_SLA.metricId -> endpoint.sla = metric.getInteger("value") / 100.0
                        }
                    }
                    model.fireTableDataChanged()
                }
            } else {
                log.error("Failed to add live view", it.cause())
            }
        }
    }
}
