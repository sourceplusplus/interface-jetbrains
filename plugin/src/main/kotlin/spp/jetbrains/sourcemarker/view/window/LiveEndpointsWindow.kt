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

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ListTableModel
import io.vertx.core.Vertx
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import spp.jetbrains.UserData
import spp.jetbrains.safeLaunch
import spp.jetbrains.view.ResumableViewCollection
import spp.jetbrains.view.column.ServiceEndpointColumnInfo
import spp.jetbrains.view.manager.LiveViewChartManager
import spp.jetbrains.view.model.ServiceEndpointRow
import spp.jetbrains.view.window.renderer.EndpointAvailabilityTableCellRenderer
import spp.jetbrains.view.window.util.EndpointRowView
import spp.protocol.artifact.metrics.MetricType.Companion.Endpoint_CPM
import spp.protocol.artifact.metrics.MetricType.Companion.Endpoint_RespTime_AVG
import spp.protocol.artifact.metrics.MetricType.Companion.Endpoint_SLA
import spp.protocol.instrument.location.LiveSourceLocation
import spp.protocol.platform.general.Service
import spp.protocol.service.SourceServices.Subscribe.toLiveViewSubscriberAddress
import spp.protocol.view.LiveView
import spp.protocol.view.LiveViewConfig
import spp.protocol.view.LiveViewEvent
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.SortOrder

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveEndpointsWindow(val project: Project, service: Service) : ResumableViewCollection() {

    private val model = ListTableModel<ServiceEndpointRow>(
        arrayOf(
            ServiceEndpointColumnInfo("Name"),
            ServiceEndpointColumnInfo("Latency"),
            ServiceEndpointColumnInfo("Availability"),
            ServiceEndpointColumnInfo("Throughput")
        ),
        ArrayList(), 0, SortOrder.DESCENDING
    )
    val component: JPanel = JPanel(BorderLayout())
    private val viewService = UserData.liveViewService(project)!!
    private var initialFocus = true
    private var refreshRate: Int = 1000
    override val refreshInterval: Int
        get() = refreshRate

    init {
        val table = JBTable(model)
        table.setShowColumns(true)
        table.setDefaultRenderer(Icon::class.java, EndpointAvailabilityTableCellRenderer())
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(mouseEvent: MouseEvent) {
                val row = table.rowAtPoint(mouseEvent.point)
                if (mouseEvent.clickCount == 2 && row >= 0) {
                    val endpointRow = model.items[table.rowSorter.convertRowIndexToModel(row)]
                    LiveViewChartManager.getInstance(project).showEndpointActivity(endpointRow.endpoint)
                }
            }
        })
        component.add(JBScrollPane(table), "Center")

        val vertx = UserData.vertx(project)
        vertx.safeLaunch {
            UserData.liveManagementService(project).getEndpoints(service.id, 1000).await().forEach {
                val endpointRow = ServiceEndpointRow(it)
                model.addRow(endpointRow)
                addView(vertx, service, endpointRow)
            }
        }
    }

    private fun addView(vertx: Vertx, service: Service, endpoint: ServiceEndpointRow) {
        val listenMetrics = listOf(
            Endpoint_CPM.metricId,
            Endpoint_RespTime_AVG.metricId,
            Endpoint_SLA.metricId
        )
        val liveView = LiveView(
            null,
            mutableSetOf(endpoint.endpoint.name),
            null,
            LiveSourceLocation("", 0, service.id),
            LiveViewConfig("LiveEndpointsWindow", listenMetrics, refreshInterval)
        )
        addView(EndpointRowView(project, viewService, liveView, endpoint, model) {
            consumerCreator(vertx, it, endpoint)
        })
    }

    private fun consumerCreator(
        vertx: Vertx,
        endpointRow: EndpointRowView,
        endpoint: ServiceEndpointRow
    ): MessageConsumer<JsonObject> {
        val developerId = UserData.developerId(endpointRow.project)
        val consumer = vertx.eventBus().consumer<JsonObject>(toLiveViewSubscriberAddress(developerId))
        consumer.handler {
            val viewEvent = LiveViewEvent(it.body())
            if (viewEvent.subscriptionId != endpointRow.liveView.subscriptionId) return@handler

            val metricArr = JsonArray(viewEvent.metricsData)
            for (i in 0 until metricArr.size()) {
                val metric = metricArr.getJsonObject(i)
                val metricsName = metric.getJsonObject("meta").getString("metricsName")
                when {
                    Endpoint_CPM.equalsIgnoringRealtime(metricsName) -> {
                        endpoint.cpm = metric.getInteger("value")
                    }

                    Endpoint_RespTime_AVG.equalsIgnoringRealtime(metricsName) -> {
                        endpoint.respTimeAvg = metric.getInteger("value")
                    }

                    Endpoint_SLA.equalsIgnoringRealtime(metricsName) -> {
                        endpoint.sla = metric.getInteger("value") / 100.0
                    }
                }
            }
            model.fireTableDataChanged()
        }
        return consumer
    }

    override fun onFocused() {
        if (initialFocus) {
            initialFocus = false
            resume()
        }
    }

    override fun setRefreshInterval(interval: Int) {
        if (interval == -1) {
            val listenMetrics = listOf(
                Endpoint_CPM.asRealtime().metricId,
                Endpoint_RespTime_AVG.asRealtime().metricId,
                Endpoint_SLA.asRealtime().metricId
            )
            getViews().map { it as EndpointRowView }.forEach {
                it.liveView = it.liveView.copy(viewConfig = it.liveView.viewConfig.copy(viewMetrics = listenMetrics))
            }
        } else {
            val listenMetrics = listOf(
                Endpoint_CPM.metricId,
                Endpoint_RespTime_AVG.metricId,
                Endpoint_SLA.metricId
            )
            getViews().map { it as EndpointRowView }.forEach {
                it.liveView = it.liveView.copy(viewConfig = it.liveView.viewConfig.copy(viewMetrics = listenMetrics))
            }
        }

        refreshRate = interval
        super.setRefreshInterval(interval)
    }

    override fun supportsRealtime(): Boolean = true
}
