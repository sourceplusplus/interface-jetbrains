package com.sourceplusplus.plugin.intellij.patcher.tail

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.components.ServiceManager
import com.sourceplusplus.plugin.intellij.IntelliJStartupActivity
import com.sourceplusplus.plugin.intellij.tool.SourcePluginConsoleService
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListenerAdapter

/**
 * Used to tail agent/skywalking logs and send them to the plugin console.
 *
 * @version 0.2.6
 * @since 0.2.4
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class LogTailer implements Runnable {

    private File logFile
    private String logSource

    LogTailer(File logFile, String logSource) {
        this.logFile = logFile
        this.logSource = logSource
    }

    @Override
    void run() {
        def consoleView = ServiceManager.getService(IntelliJStartupActivity.currentProject,
                SourcePluginConsoleService.class).getConsoleView()
        new Tailer(logFile, new TailerListenerAdapter() {
            @Override
            void handle(Exception ex) {
                ex.stackTrace.each {
                    consoleView.print("[$logSource] - " + it.toString() + "\n", ConsoleViewContentType.ERROR_OUTPUT)
                }
            }

            @Override
            void handle(String line) {
                consoleView.print("[$logSource] - " + line + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
            }
        }).run()
    }
}
