/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.sourcemarker.console

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
                var module = loggingEvent.loggerName.replace("spp.jetbrains.", "")
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
                var module = loggingEvent.loggerName.replace("spp.jetbrains.", "")
                module = module.substring(0, module.indexOf(".")).toUpperCase()
                consoleView!!.print("[$module] - $message\n", ConsoleViewContentType.NORMAL_OUTPUT)
            }
        }
    }
}
