package com.sourceplusplus.sourcemarker.console

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.AppenderBase
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType

/**
 * Displays logs from the SourceMarker plugin to a console window.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerAppender<E> : AppenderBase<E>() {

    companion object {
        var consoleView: ConsoleView? = null
    }

    override fun append(eventObject: E) {
        if (consoleView == null) return

        val loggingEvent = eventObject as LoggingEvent
        val message = loggingEvent.formattedMessage
        if (loggingEvent.level.isGreaterOrEqual(Level.WARN)) {
            if (message.toString().startsWith("[PORTAL]")) {
                consoleView!!.print("$message\n", ConsoleViewContentType.ERROR_OUTPUT)
            } else {
                var module = loggingEvent.loggerName.replace("com.sourceplusplus.", "")
                module = module.substring(0, module.indexOf(".")).toUpperCase()
                if (loggingEvent.throwableProxy != null) {
                    consoleView!!.print(
                        "[$module] - $message\n${ThrowableProxyUtil.asString(loggingEvent.throwableProxy)}\n",
                        ConsoleViewContentType.ERROR_OUTPUT
                    )
                } else {
                    consoleView!!.print("[$module] - $message\n", ConsoleViewContentType.ERROR_OUTPUT)
                }
            }
        } else {
            if (message.toString().startsWith("[PORTAL]")) {
                consoleView!!.print("$message\n", ConsoleViewContentType.NORMAL_OUTPUT)
            } else {
                var module = loggingEvent.loggerName.replace("com.sourceplusplus.", "")
                module = module.substring(0, module.indexOf(".")).toUpperCase()
                consoleView!!.print("[$module] - $message\n", ConsoleViewContentType.NORMAL_OUTPUT)
            }
        }
    }
}
