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
package spp.jetbrains.sourcemarker.view.window.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.ui.ListTableModel
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import spp.jetbrains.sourcemarker.view.model.ServiceEndpointRow
import spp.jetbrains.view.ResumableView
import spp.protocol.artifact.metrics.MetricStep
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.service.LiveViewService
import spp.protocol.view.LiveView
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class EndpointRowView(
    private val viewService: LiveViewService,
    var liveView: LiveView,
    private val endpoint: ServiceEndpointRow,
    private val model: ListTableModel<ServiceEndpointRow>,
    private val consumerCreator: (EndpointRowView) -> MessageConsumer<JsonObject>
) : ResumableView {

    private val log = logger<EndpointRowView>()
    private var consumer: MessageConsumer<JsonObject>? = null

    override var isRunning = false
        private set
    override val refreshInterval: Int
        get() = liveView.viewConfig.refreshRateLimit

    override fun resume() {
        if (isRunning) return
        isRunning = true
        viewService.addLiveView(liveView).onSuccess {
            liveView = it
            consumer = consumerCreator.invoke(this)
        }.onFailure {
            log.error("Failed to resume live view", it)
        }

        getHistoricalData()
    }

    private fun getHistoricalData() {
        val stop = Instant.now().truncatedTo(ChronoUnit.MINUTES).minusSeconds(60L)
        val start = stop.minusSeconds(60L)
        viewService.getHistoricalMetrics(
            listOf(endpoint.endpoint.id),
            liveView.viewConfig.viewMetrics,
            MetricStep.MINUTE, start, stop
        ).onSuccess {
            for (i in 0 until it.data.size()) {
                val metric = it.data.getJsonObject(i)
                val metricId = metric.getString("metricId")
                when {
                    MetricType.Endpoint_CPM.equalsIgnoringRealtime(metricId) -> {
                        endpoint.cpm = metric.getInteger("value")
                    }

                    MetricType.Endpoint_RespTime_AVG.equalsIgnoringRealtime(metricId) -> {
                        endpoint.respTimeAvg = metric.getInteger("value")
                    }

                    MetricType.Endpoint_SLA.equalsIgnoringRealtime(metricId) -> {
                        endpoint.sla = metric.getInteger("value") / 100.0
                    }
                }
            }

            ApplicationManager.getApplication().invokeLater {
                model.fireTableDataChanged()
            }
        }.onFailure {
            log.error("Failed to get historical metrics", it)
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

    override fun setRefreshInterval(interval: Int) {
        pause()
        liveView = liveView.copy(viewConfig = liveView.viewConfig.copy(refreshRateLimit = interval))
        resume()
    }

    override fun dispose() = pause()
}