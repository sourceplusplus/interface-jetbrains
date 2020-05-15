package com.sourceplusplus.plugin.intellij.tool

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NotNull

/**
 * Displays logs from the agent and plugin in a console window.
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class SourcePluginConsoleService {

    private final ConsoleView consoleView

    SourcePluginConsoleService(@NotNull Project project) {
        consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole()
    }

    @NotNull
    ConsoleView getConsoleView() {
        return consoleView
    }
}
