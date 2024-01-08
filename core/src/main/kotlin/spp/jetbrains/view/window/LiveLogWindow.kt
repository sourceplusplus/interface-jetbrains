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
package spp.jetbrains.view.window

import com.intellij.codeInsight.lookup.impl.LookupCellRenderer
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.editor.markup.TextAttributes
import io.vertx.core.json.JsonObject
import spp.jetbrains.view.ResumableView
import spp.protocol.artifact.ArtifactNameUtils
import spp.protocol.artifact.log.Log
import spp.protocol.view.LiveView
import spp.protocol.view.LiveViewEvent
import java.awt.Font
import java.time.LocalTime
import java.time.ZoneId

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface LiveLogWindow : ResumableView {

    companion object {
        val LIVE_OUTPUT_TYPE = ConsoleViewContentType(
            "LIVE_OUTPUT",
            TextAttributes(LookupCellRenderer.MATCHED_FOREGROUND_COLOR, null, null, null, Font.PLAIN)
        )
    }

    val liveView: LiveView
    val console: ConsoleView

    fun handleEvent(viewEvent: LiveViewEvent) {
        val rawLog = Log(JsonObject(viewEvent.metricsData).getJsonObject("log"))
        val localTime = LocalTime.ofInstant(rawLog.timestamp, ZoneId.systemDefault())
        val logLine = buildString {
            append(localTime)
            append(" [").append(rawLog.thread).append("] ")
            append(rawLog.level.uppercase()).append(" - ")
            rawLog.logger?.let { append(ArtifactNameUtils.getShortQualifiedClassName(it)).append(" - ") }
            append(rawLog.toFormattedMessage())
            appendLine()
        }

        when (rawLog.level.uppercase()) {
            "LIVE" -> console.print(logLine, LIVE_OUTPUT_TYPE)
            "WARN", "ERROR" -> console.print(logLine, ConsoleViewContentType.ERROR_OUTPUT)
            else -> console.print(logLine, ConsoleViewContentType.NORMAL_OUTPUT)
        }
    }
}
