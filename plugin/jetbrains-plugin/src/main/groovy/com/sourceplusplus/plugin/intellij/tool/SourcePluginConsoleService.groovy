package com.sourceplusplus.plugin.intellij.tool

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.spi.LoggingEvent
import org.jetbrains.annotations.NotNull

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace

/**
 * Displays logs from the agent and plugin in a console window.
 *
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class SourcePluginConsoleService {

    private final ConsoleView consoleView

    SourcePluginConsoleService(@NotNull Project project) {
        consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole()

        //redirect loggers to console
        Logger.getLogger("com.sourceplusplus").addAppender(new AppenderSkeleton() {
            @Override
            protected void append(LoggingEvent loggingEvent) {
                Object message = loggingEvent.message
                if (loggingEvent.level.isGreaterOrEqual(Level.WARN)) {
                    if (message.toString().startsWith("[PORTAL]")) {
                        consoleView.print("$message\n", ConsoleViewContentType.ERROR_OUTPUT)
                    } else {
                        def module = loggingEvent.logger.getName().replace("com.sourceplusplus.", "")
                        module = module.substring(0, module.indexOf(".")).toUpperCase()
                        if (loggingEvent.throwableInformation) {
                            consoleView.print("[$module] - $message\n${getStackTrace(loggingEvent.throwableInformation.throwable)}\n",
                                    ConsoleViewContentType.ERROR_OUTPUT)
                        } else {
                            consoleView.print("[$module] - $message\n", ConsoleViewContentType.ERROR_OUTPUT)
                        }
                    }
                } else if (loggingEvent.level.isGreaterOrEqual(Level.INFO)) {
                    if (message.toString().startsWith("[PORTAL]")) {
                        consoleView.print("$message\n", ConsoleViewContentType.NORMAL_OUTPUT)
                    } else {
                        def module = loggingEvent.logger.getName().replace("com.sourceplusplus.", "")
                        module = module.substring(0, module.indexOf(".")).toUpperCase()
                        consoleView.print("[$module] - $message\n", ConsoleViewContentType.NORMAL_OUTPUT)
                    }
                }
            }

            @Override
            void close() {
            }

            @Override
            boolean requiresLayout() {
                return false
            }
        })
        Disposer.register(project, consoleView)
    }

    @NotNull
    ConsoleView getConsoleView() {
        return consoleView
    }
}
