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
package spp.jetbrains.view

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.platform.general.ServiceEndpoint

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface LiveViewChartManager : ResumableViewManager {
    companion object {
        val KEY = Key.create<LiveViewChartManager>("SPP_LIVE_VIEW_CHART_MANAGER")

        fun getInstance(project: Project): LiveViewChartManager {
            return project.getUserData(KEY)!!
        }
    }

    fun getHistoricalMinutes(): Int?
    fun setHistoricalMinutes(historicalMinutes: Int)
    fun showOverviewActivity()
    fun showEndpointActivity(endpoint: ServiceEndpoint)

    fun showChart(
        entityId: String,
        name: String,
        scope: String,
        metricTypes: List<MetricType>,
        labels: List<String> = emptyList()
    )
}
