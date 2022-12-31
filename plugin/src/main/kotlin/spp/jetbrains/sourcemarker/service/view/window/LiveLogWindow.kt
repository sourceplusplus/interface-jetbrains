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

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveLogWindow(project: Project) : Disposable {

    val layoutComponent: JComponent
        get() = component

    val component: JPanel = JPanel(BorderLayout())

    fun showInConsole(
        message: String,
        project: Project,
        contentType: ConsoleViewContentType = ConsoleViewContentType.NORMAL_OUTPUT,
        scrollTo: Int = -1
    ): ConsoleView {
        val result: AtomicReference<ConsoleView> = AtomicReference()

        ApplicationManager.getApplication().invokeAndWait {
            val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
            console.print(message, contentType)

            val toolbarActions = DefaultActionGroup()
            component.add(console.component, BorderLayout.CENTER)

            console.createConsoleActions().forEach { toolbarActions.add(it) }

            result.set(console)
            if (scrollTo >= 0) console.scrollTo(scrollTo)
        }
        return result.get()
    }

    override fun dispose() = Unit
}
