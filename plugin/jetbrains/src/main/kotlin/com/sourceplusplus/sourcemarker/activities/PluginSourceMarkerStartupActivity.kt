package com.sourceplusplus.sourcemarker.activities

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.sourceplusplus.marker.plugin.SourceMarkerStartupActivity
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import kotlinx.coroutines.runBlocking
import org.apache.log4j.FileAppender
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PluginSourceMarkerStartupActivity : SourceMarkerStartupActivity() {

    init {
        if (System.getProperty("sourcemarker.debug.capture_logs", "false")!!.toBoolean()) {
            val fa = FileAppender()
            fa.file = "/tmp/sourcemarker.log"
            fa.layout = PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n")
            fa.activateOptions()
            Logger.getLogger("com.sourceplusplus").addAppender(fa)
        }
    }

    override fun runActivity(project: Project) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return //tests manually set up necessary components
        }

        //setup plugin
        runBlocking {
            SourceMarkerPlugin.init(project)
        }
        super.runActivity(project)
    }
}
