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

import com.intellij.codeInsight.lookup.impl.LookupCellRenderer.MATCHED_FOREGROUND_COLOR
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import spp.jetbrains.UserData
import spp.jetbrains.view.LogWindow
import spp.protocol.artifact.ArtifactNameUtils
import spp.protocol.artifact.log.Log
import spp.protocol.service.SourceServices
import spp.protocol.view.LiveView
import spp.protocol.view.LiveViewConfig
import spp.protocol.view.LiveViewEvent
import java.awt.BorderLayout
import java.awt.Font
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveLogWindow(private val project: Project) : LogWindow {

    private val log = logger<LiveLogWindow>()
    private val liveOutputType = ConsoleViewContentType(
        "LIVE_OUTPUT",
        TextAttributes(MATCHED_FOREGROUND_COLOR, null, null, null, Font.PLAIN)
    )

    private val vertx = UserData.vertx(project)
    private val viewService = UserData.liveViewService(project)!!
    override var liveView: LiveView? = null
    override var consumer: MessageConsumer<JsonObject>? = null
    override var console: ConsoleView? = null
    val component = JPanel(BorderLayout()).apply { isFocusable = true }

    var isRunning = true
        private set

    override fun pause() {
        isRunning = false
        consumer?.unregister()
        consumer = null
        liveView?.subscriptionId?.let { viewService.removeLiveView(it) }
        liveView = null
    }

    override fun resume() {
        isRunning = true
        viewService.addLiveView(
            LiveView(
                entityIds = mutableSetOf("*"),
                viewConfig = LiveViewConfig("VIEW_LOGS", listOf("endpoint_logs"))
            )
        ).onSuccess { sub ->
            liveView = sub
            if (console == null) {
                console = makeConsoleView(project)
            }

            consumer = vertx.eventBus().consumer(
                SourceServices.Subscribe.toLiveViewSubscriberAddress("system")
            )
            consumer!!.handler {
                val liveViewEvent = LiveViewEvent(it.body())
                if (liveViewEvent.subscriptionId != sub.subscriptionId) return@handler

                val rawLog = Log(JsonObject(liveViewEvent.metricsData).getJsonObject("log"))
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
                    "LIVE" -> console?.print(logLine, liveOutputType)
                    "WARN", "ERROR" -> console?.print(logLine, ConsoleViewContentType.ERROR_OUTPUT)
                    else -> console?.print(logLine, ConsoleViewContentType.NORMAL_OUTPUT)
                }
            }
        }.onFailure {
            log.error("Failed to start service logs tail", it)
        }
    }

    private fun makeConsoleView(project: Project): ConsoleView {
        val result: AtomicReference<ConsoleView> = AtomicReference()
        ApplicationManager.getApplication().invokeAndWait {
            val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
            val toolbarActions = DefaultActionGroup()
            component.add(console.component, BorderLayout.CENTER)
            console.createConsoleActions().forEach { toolbarActions.add(it) }
            result.set(console)
        }
        return result.get()
    }

    override fun dispose() {
        consumer?.unregister()
        liveView?.subscriptionId?.let { viewService.removeLiveView(it) }
    }
}
