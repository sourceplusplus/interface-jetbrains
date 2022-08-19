/*
 * Source++, the open-source live coding platform.
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
package spp.jetbrains.sourcemarker.statusBar

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import spp.jetbrains.sourcemarker.status.SourceStatus
import spp.jetbrains.sourcemarker.status.SourceStatus.Pending
import spp.jetbrains.sourcemarker.status.SourceStatus.Ready
import spp.jetbrains.sourcemarker.status.SourceStatusService

class SourceStatusBarWidget(project: Project) : EditorBasedStatusBarPopup(project, false) {

    companion object {
        const val WIDGET_ID = "spp.jetbrains.sourceWidget"

        fun update(project: Project) {
            val widget = findWidget(project)
            widget?.update { widget.myStatusBar.updateWidget(WIDGET_ID) }
        }

        private fun findWidget(project: Project): SourceStatusBarWidget? {
            val bar = WindowManager.getInstance().getStatusBar(project)
            if (bar != null) {
                val widget = bar.getWidget(WIDGET_ID)
                if (widget is SourceStatusBarWidget) {
                    return widget
                }
            }
            return null
        }
    }

    init {
        //this class is called before StartupActivity so need to manually initialize plugin services
        SourceMarkerPlugin.getInstance(project)
    }

    override fun ID(): String = WIDGET_ID

    override fun getWidgetState(file: VirtualFile?): WidgetState {
        val statusAndMessage = SourceStatusService.getInstance(project).getCurrentStatus()
        val status = statusAndMessage.first
        return if (status.isIconAlwaysShown()) {
            val toolTip = statusAndMessage.second ?: status.presentableText
            val state = WidgetState(toolTip, "", true)
            state.icon = status.icon
            state
        } else {
            if (file == null) {
                WidgetState.HIDDEN
            } else {
                val enabled = true //todo: false
                if (enabled == null) {
                    WidgetState.HIDDEN
                } else {
                    val toolTip = "todothis"
                    val state = WidgetState(toolTip, "", true)
                    state.setIcon(if (enabled) status.icon else status.icon)
                    state
                }
            }
        }
    }

    override fun createPopup(context: DataContext): ListPopup? {
        return createPopup(context, false)
    }

    private fun createPopup(context: DataContext, withStatusItem: Boolean): ListPopup? {
        val currentStatus = SourceStatusService.getInstance(project).getCurrentStatus().first
        val configuredGroup = ActionManager.getInstance().getAction(findPopupMenuId(currentStatus))
        return if (configuredGroup !is ActionGroup) {
            null
        } else {
            val group = if (withStatusItem) {
                val statusGroup = DefaultActionGroup()
                statusGroup.add(SourceStatusItemAction())
                statusGroup.addSeparator()
                statusGroup.addAll(*arrayOf<AnAction>(configuredGroup))
                statusGroup
            } else {
                configuredGroup
            }
            JBPopupFactory.getInstance().createActionGroupPopup(
                "Source++ Status",
                group,
                context,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                withStatusItem
            )
        }
    }

    private fun findPopupMenuId(currentStatus: SourceStatus): String {
        return if (currentStatus == Ready || currentStatus == Pending) {
            "spp.enabled.statusBarPopup"
        } else {
            "spp.disabled.statusBarPopup"
        }
    }

    override fun createInstance(project: Project): StatusBarWidget {
        return SourceStatusBarWidget(project)
    }
}
