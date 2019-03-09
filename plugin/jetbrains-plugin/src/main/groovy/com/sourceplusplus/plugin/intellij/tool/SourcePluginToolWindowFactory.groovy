package com.sourceplusplus.plugin.intellij.tool

import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import org.jetbrains.annotations.NotNull

/**
 * todo: description
 *
 * @version 0.1.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class SourcePluginToolWindowFactory implements ToolWindowFactory {

    @Override
    void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        SourcePluginConsoleService consoleService = ServiceManager.getService(project, SourcePluginConsoleService.class)
        ConsoleView consoleView = consoleService.getConsoleView()
        Content content = toolWindow.getContentManager().getFactory().createContent(
                consoleView.getComponent(), "", true)
        toolWindow.getContentManager().addContent(content)
    }
}
