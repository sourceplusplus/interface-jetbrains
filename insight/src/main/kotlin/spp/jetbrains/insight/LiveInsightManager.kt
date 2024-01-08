/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.insight

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import spp.jetbrains.insight.contributor.FunctionDurationContributor
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener

/**
 * When remote insights are unavailable, we fall back on local insights. Local insights require the
 * developer to continuously perform insight calculations on their own machine and do not have
 * the ability to share insights with other developers. Local insights also cannot trigger automatic
 * insight instrumentation to perform more precise calculations.
 */
class LiveInsightManager(
    private val remoteInsightsAvailable: Boolean
) : CoroutineVerticle(), SourceMarkEventListener, Disposable {

//    private val log = logger<LiveInsightManager>()

//    private lateinit var insightService: LiveInsightService
//    private lateinit var workspace: InsightWorkspace
    private val contributors = listOf(
        FunctionDurationContributor(remoteInsightsAvailable)
    )

    override suspend fun start() {
        if (remoteInsightsAvailable) {
            //startRemoteInsights()
        }
    }

    override fun handleEvent(event: SourceMarkEvent) {
        when (event.eventCode) {
            SourceMarkEventCode.MARK_ADDED -> {
                if (event.sourceMark.isClassMark) {
                    if (remoteInsightsAvailable) {
                        //vertx.safeLaunch { downloadInsights() }
                    }
                }
            }
        }

        contributors.forEach { it.handleEvent(event) }
    }

    override suspend fun stop() = Disposer.dispose(this)
    override fun dispose() = Unit
}
