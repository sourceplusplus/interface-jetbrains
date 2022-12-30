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
package spp.jetbrains.sourcemarker.service.view.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.*
import spp.jetbrains.icons.PluginIcons
import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ChangeChartAction : AnAction(PluginIcons.chartMixed) {

    var selected = "Average"

    override fun actionPerformed(e: AnActionEvent) {
        JBPopupFactory.getInstance().createListPopup(
            object : ListPopupStep<String> {

                override fun getTitle(): String? = null
                override fun canceled() = Unit
                override fun isMnemonicsNavigationEnabled(): Boolean = false

                override fun getMnemonicNavigationFilter(): MnemonicNavigationFilter<String>? {
                    TODO("Not yet implemented")
                }

                override fun isSpeedSearchEnabled(): Boolean = false

                override fun getSpeedSearchFilter(): SpeedSearchFilter<String>? {
                    TODO("Not yet implemented")
                }

                override fun isAutoSelectionEnabled(): Boolean = false
                override fun getFinalRunnable(): Runnable? = null

                override fun getValues(): MutableList<String> {
                    return listOf(
                        "Average",
                        "Percentile",
                    ).toMutableList()
                }

                override fun getDefaultOptionIndex(): Int = 0
                override fun getSeparatorAbove(value: String?): ListSeparator? = null
                override fun getTextFor(value: String): String = value

                override fun getIconFor(value: String): Icon {
                    return if (value == selected) {
                        PluginIcons.squareCheck
                    } else {
                        PluginIcons.squareDashed
                    }
                }

                override fun isSelectable(value: String?): Boolean = true
                override fun hasSubstep(selectedValue: String?): Boolean = false

                override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
                    return null
                }
            }
        ).showUnderneathOf(e.inputEvent.component)
    }
}
