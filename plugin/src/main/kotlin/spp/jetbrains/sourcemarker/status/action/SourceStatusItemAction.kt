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
package spp.jetbrains.sourcemarker.status.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import spp.jetbrains.status.SourceStatusService

class SourceStatusItemAction : AnAction(), DumbAware {

    companion object {
        private val log = logger<SourceStatusItemAction>()
    }

    override fun update(e: AnActionEvent) {
        if (e.project == null) {
            log.warn("Ignoring update for action without project")
            return
        }

        e.presentation.isEnabled = false
        val status = SourceStatusService.getInstance(e.project!!).getCurrentStatus()
        e.presentation.disabledIcon = status.first.icon
        e.presentation.text = status.second ?: status.first.name
    }

    //override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    override fun actionPerformed(e: AnActionEvent) = Unit
}
