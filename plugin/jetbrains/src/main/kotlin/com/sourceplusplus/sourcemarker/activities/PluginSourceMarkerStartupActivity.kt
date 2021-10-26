package com.sourceplusplus.sourcemarker.activities

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.sourceplusplus.marker.plugin.SourceMarkerStartupActivity
import com.sourceplusplus.sourcemarker.PluginBundle
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import kotlinx.coroutines.runBlocking
import org.apache.log4j.FileAppender
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PluginSourceMarkerStartupActivity : SourceMarkerStartupActivity() {

    companion object {
        @JvmField
        val PYCHARM_PRODUCT_CODES = setOf("PY", "PC", "PE")

        @JvmField
        val INTELLIJ_PRODUCT_CODES = setOf("IC", "IU")
        val SUPPORTED_PRODUCT_CODES = PYCHARM_PRODUCT_CODES + INTELLIJ_PRODUCT_CODES
    }

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
        } else if (!SUPPORTED_PRODUCT_CODES.contains(ApplicationInfo.getInstance().build.productCode)) {
            val pluginName = PluginBundle.message("plugin_name")
            Notifications.Bus.notify(
                Notification(
                    pluginName,
                    "Unsupported product code",
                    "Unsupported product code: ${ApplicationInfo.getInstance().build.productCode}",
                    NotificationType.ERROR
                )
            )
            return
        }

        //setup plugin
        runBlocking {
            SourceMarkerPlugin.init(project)
        }
        super.runActivity(project)
    }
}
