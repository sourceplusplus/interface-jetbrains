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
package spp.jetbrains.sourcemarker.instrument.ui.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import spp.jetbrains.UserData
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.safeLaunch
import spp.jetbrains.sourcemarker.instrument.InstrumentEventWindowService

/**
 * todo: description.
 *
 * @since 0.7.7
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class RemoveInstrumentAction(val service: InstrumentEventWindowService) : AnAction(PluginIcons.xmark) {

    private val log = logger<RemoveInstrumentAction>()

    init {
        templatePresentation.text = "Remove Instrument"
    }

    override fun update(e: AnActionEvent) {
        UserData.vertx(service.project).safeLaunch {
            val selfInfo = UserData.selfInfo(service.project)
            if (selfInfo == null) {
                e.presentation.isEnabled = false
                return@safeLaunch
            }

            val instrumentOverview = service.selectedInstrumentOverview
            if (instrumentOverview == null) {
                e.presentation.isEnabled = false
            } else {
                e.presentation.isEnabled = instrumentOverview.isRemovable(selfInfo.developer.id)
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val instrumentId = service.selectedInstrumentOverview?.instrumentId ?: return
        UserData.liveInstrumentService(service.project)?.removeLiveInstrument(instrumentId)?.onFailure {
            log.error("Failed to remove live instrument", it)
        }
    }
}
