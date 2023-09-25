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
import spp.jetbrains.view.ResumableViewManager
import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SetRefreshIntervalAction(private val viewManager: ResumableViewManager) : AnAction(PluginIcons.rotate) {

    init {
        templatePresentation.text = "Refresh Interval"
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = viewManager.currentView?.isRunning == true
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
                    return if (viewManager.currentView?.supportsRealtime() == true) {
                        mutableListOf("Realtime", "1s", "5s", "15s", "30s")
                    } else {
                        mutableListOf("1s", "5s", "15s", "30s")
                    }
                }

                override fun getDefaultOptionIndex(): Int = 0
                override fun getSeparatorAbove(value: String?): ListSeparator? = null
                override fun getTextFor(value: String): String = value

                override fun getIconFor(value: String): Icon {
                    val refreshIntervalStr = when (viewManager.refreshInterval) {
                        -1 -> "Realtime"
                        1_000 -> "1s"
                        5_000 -> "5s"
                        15_000 -> "15s"
                        30_000 -> "30s"
                        else -> null
                    }
                    return if (value == refreshIntervalStr) {
                        PluginIcons.squareCheck
                    } else {
                        PluginIcons.squareDashed
                    }
                }

                override fun isSelectable(value: String?): Boolean = true
                override fun hasSubstep(selectedValue: String?): Boolean = false

                override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
                    val refreshInterval = when (selectedValue) {
                        "Realtime" -> -1
                        "1s" -> 1_000
                        "5s" -> 5_000
                        "15s" -> 15_000
                        "30s" -> 30_000
                        else -> null
                    } ?: return null
                    viewManager.currentView?.setRefreshInterval(refreshInterval)

                    return null
                }
            }
        ).apply { e.inputEvent?.let { showUnderneathOf(it.component) } }
    }
}
