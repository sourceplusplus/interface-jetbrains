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
package spp.jetbrains.sourcemarker.view.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.*
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.view.manager.LiveViewChartManager
import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ChangeTimeAction(private val viewManager: LiveViewChartManager) : AnAction(PluginIcons.clockRotateLeft) {

    init {
        templatePresentation.text = "Change Time"
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = viewManager.currentView != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        JBPopupFactory.getInstance().createListPopup(
            object : ListPopupStep<String> {
                override fun getTitle(): String? = null
                override fun canceled() = Unit
                override fun isMnemonicsNavigationEnabled(): Boolean = false
                override fun getMnemonicNavigationFilter(): MnemonicNavigationFilter<String>? = null
                override fun isSpeedSearchEnabled(): Boolean = false
                override fun getSpeedSearchFilter(): SpeedSearchFilter<String>? = null
                override fun isAutoSelectionEnabled(): Boolean = false
                override fun getFinalRunnable(): Runnable? = null

                override fun getValues(): MutableList<String> {
                    return listOf(
                        "Last 5 Minutes",
                        "Last 15 Minutes",
                        "Last 30 Minutes",
                        "Last 1 Hour",
                        "Last 4 Hours",
                        "Last 12 Hours",
                        "Last 24 Hours",
                    ).toMutableList()
                }

                override fun getDefaultOptionIndex(): Int = 0
                override fun getSeparatorAbove(value: String?): ListSeparator? = null
                override fun getTextFor(value: String): String = value

                override fun getIconFor(value: String): Icon {
                    val selected = when (viewManager.getHistoricalMinutes()) {
                        5 -> "Last 5 Minutes"
                        15 -> "Last 15 Minutes"
                        30 -> "Last 30 Minutes"
                        60 -> "Last 1 Hour"
                        240 -> "Last 4 Hours"
                        720 -> "Last 12 Hours"
                        1440 -> "Last 24 Hours"
                        else -> "Last 5 Minutes"
                    }
                    return if (value == selected) {
                        PluginIcons.squareCheck
                    } else {
                        PluginIcons.squareDashed
                    }
                }

                override fun isSelectable(value: String?): Boolean = true
                override fun hasSubstep(selectedValue: String?): Boolean = false

                override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
                    val minutes = when (selectedValue) {
                        "Last 5 Minutes" -> 5
                        "Last 15 Minutes" -> 15
                        "Last 30 Minutes" -> 30
                        "Last 1 Hour" -> 60
                        "Last 4 Hours" -> 240
                        "Last 12 Hours" -> 720
                        "Last 24 Hours" -> 1440
                        else -> 5
                    }

                    if (minutes != viewManager.getHistoricalMinutes()) {
                        viewManager.setHistoricalMinutes(minutes)
                    }
                    return null
                }
            }
        ).showUnderneathOf(e.inputEvent.component)
    }
}
