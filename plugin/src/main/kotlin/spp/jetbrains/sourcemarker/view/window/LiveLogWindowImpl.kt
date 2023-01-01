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

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import spp.jetbrains.UserData
import spp.jetbrains.view.window.LiveLogWindow
import spp.protocol.view.LiveView
import java.awt.BorderLayout
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveLogWindowImpl(
    project: Project,
    override var liveView: LiveView,
    private val consumerCreator: (LiveLogWindow) -> MessageConsumer<JsonObject>
) : LiveLogWindow {

    private val log = logger<LiveLogWindowImpl>()
    private val viewService = UserData.liveViewService(project)!!
    override var consumer: MessageConsumer<JsonObject>? = null
    override val console: ConsoleView
    val component = JPanel(BorderLayout()).apply { isFocusable = true }
    override var isRunning = false
        private set

    init {
        console = makeConsoleView(project)
        Disposer.register(this, console)

        resume()
    }

    override fun resume() {
        if (isRunning) return
        isRunning = true
        viewService.addLiveView(liveView).onSuccess {
            liveView = it
            consumer = consumerCreator.invoke(this)
        }.onFailure {
            log.error("Failed to resume live view", it)
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

    override fun dispose() = pause()
}
