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
package spp.jetbrains.monitor.skywalking.bridge

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import spp.jetbrains.monitor.skywalking.SkywalkingClient

/**
 * todo: description.
 *
 * @since 0.4.7
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class GeneralBridge(private val skywalkingClient: SkywalkingClient) : CoroutineVerticle() {

    override suspend fun start() {
        vertx.eventBus().localConsumer<Boolean>(getVersionAddress) {
            launch(vertx.dispatcher()) {
                it.reply(skywalkingClient.getVersion())
            }
        }
    }

    companion object {
        private const val rootAddress = "monitor.skywalking.general"
        private const val getVersionAddress = "$rootAddress.getVersion"

        suspend fun getVersion(vertx: Vertx): String {
            return vertx.eventBus()
                .request<String>(getVersionAddress, true)
                .await().body()
        }
    }
}
