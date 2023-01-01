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
package spp.jetbrains.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import spp.jetbrains.view.LogWindow
import spp.protocol.view.LiveView

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface LiveViewLogService {
    companion object {
        val KEY = Key.create<LiveViewLogService>("SPP_LIVE_VIEW_LOG_SERVICE")

        fun getInstance(project: Project): LiveViewLogService {
            return project.getUserData(KEY)!!
        }
    }

    fun getOrCreateLogWindow(
        liveView: LiveView,
        consumer: (LogWindow) -> MessageConsumer<JsonObject>,
        title: String
    ): LogWindow
}
