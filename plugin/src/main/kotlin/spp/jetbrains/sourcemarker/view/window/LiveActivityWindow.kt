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
import io.vertx.core.Vertx
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import spp.jetbrains.UserData
import spp.jetbrains.view.window.util.TabbedResumableView
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.service.LiveViewService
import spp.protocol.service.SourceServices.Subscribe.toLiveViewSubscriberAddress
import spp.protocol.view.LiveView
import spp.protocol.view.LiveViewConfig
import spp.protocol.view.LiveViewEvent

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveActivityWindow(
    project: Project,
    private val viewService: LiveViewService,
    entityId: String,
    entityName: String,
    private val scope: String,
    metrics: List<MetricType>,
    labels: List<String> = listOf(),
    private var refreshRate: Int = -1
) : TabbedResumableView() {

    private var initialFocus = true
    override val refreshInterval: Int
        get() = refreshRate

    init {
        val vertx = UserData.vertx(project)
        metrics.forEach {
            val respTimeChart = LiveViewChartWindowImpl(
                project, viewService, LiveView(
                    entityIds = mutableSetOf(entityId),
                    viewConfig = LiveViewConfig(
                        "${scope.uppercase()}_ACTIVITY_CHART",
                        listOf(it.metricId),
                        refreshRate
                    )
                ), entityName, labels
            ) { consumerCreator(it, vertx) }
            addTab("$scope ${it.simpleName}", respTimeChart, respTimeChart.component)
        }
    }

    override fun onFocused() {
        if (initialFocus) {
            initialFocus = false
            resume()
        }
    }

    fun getHistoricalMinutes(): Int? {
        return (getViews().firstOrNull() as? LiveViewChartWindowImpl)?.getHistoricalMinutes()
    }

    fun setHistoricalMinutes(historicalMinutes: Int) {
        getViews().forEach {
            it as LiveViewChartWindowImpl
            it.setHistoricalMinutes(historicalMinutes)
        }
    }

    override fun setRefreshInterval(interval: Int) {
        refreshRate = interval
        super.setRefreshInterval(interval)
    }

    override fun supportsRealtime(): Boolean = scope != "Service"

    private fun consumerCreator(window: LiveViewChartWindowImpl, vertx: Vertx): MessageConsumer<JsonObject> {
        val developerId = UserData.developerId(window.project)
        val consumer = vertx.eventBus().consumer<JsonObject>(toLiveViewSubscriberAddress(developerId))
        return consumer.handler {
            val liveViewEvent = LiveViewEvent(it.body())
            if (window.liveView.subscriptionId != liveViewEvent.subscriptionId) return@handler
            window.addMetric(liveViewEvent)
        }
    }
}
