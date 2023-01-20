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
package spp.jetbrains.sourcemarker.status

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import spp.jetbrains.sourcemarker.status.action.SourceStatusItemAction
import spp.jetbrains.status.SourceStatus
import spp.jetbrains.status.SourceStatus.Pending
import spp.jetbrains.status.SourceStatus.Ready
import spp.jetbrains.status.SourceStatusService

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
        val toolTip = statusAndMessage.second ?: status.presentableText
        return WidgetState(toolTip, "", true).apply {
            icon = status.icon
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
                statusGroup.addAll(listOf(configuredGroup))
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
