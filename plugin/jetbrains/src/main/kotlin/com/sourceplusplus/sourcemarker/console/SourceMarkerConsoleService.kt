package com.sourceplusplus.sourcemarker.console

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.spi.LoggingEvent

/**
 * Displays logs from the SourceMarker plugin to a console window.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerConsoleService(project: Project) {

    private var consoleView: ConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console

    init {
        //redirect loggers to console
        Logger.getLogger("com.sourceplusplus").addAppender(object : AppenderSkeleton() {
            override fun append(loggingEvent: LoggingEvent) {
                val message = loggingEvent.message
                if (loggingEvent.level.isGreaterOrEqual(Level.WARN)) {
                    if (message.toString().startsWith("[PORTAL]")) {
                        consoleView.print("$message\n", ConsoleViewContentType.ERROR_OUTPUT)
                    } else {
                        var module = loggingEvent.logger.name.replace("com.sourceplusplus.", "")
                        module = module.substring(0, module.indexOf(".")).toUpperCase()
                        if (loggingEvent.throwableInformation != null) {
                            consoleView.print(
                                "[$module] - $message\n${getStackTrace(loggingEvent.throwableInformation.throwable)}\n",
                                ConsoleViewContentType.ERROR_OUTPUT
                            )
                        } else {
                            consoleView.print("[$module] - $message\n", ConsoleViewContentType.ERROR_OUTPUT)
                        }
                    }
                } else if (loggingEvent.level.isGreaterOrEqual(Level.INFO)) {
                    if (message.toString().startsWith("[PORTAL]")) {
                        consoleView.print("$message\n", ConsoleViewContentType.NORMAL_OUTPUT)
                    } else {
                        var module = loggingEvent.logger.name.replace("com.sourceplusplus.", "")
                        module = module.substring(0, module.indexOf(".")).toUpperCase()
                        consoleView.print("[$module] - $message\n", ConsoleViewContentType.NORMAL_OUTPUT)
                    }
                }
            }

            override fun close() = Unit
            override fun requiresLayout(): Boolean = false
        })
        Disposer.register(project, consoleView)
    }

    fun getConsoleView(): ConsoleView {
        return consoleView
    }
}
