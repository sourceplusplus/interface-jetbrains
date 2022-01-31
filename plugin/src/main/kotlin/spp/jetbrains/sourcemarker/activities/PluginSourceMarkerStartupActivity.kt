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
package spp.jetbrains.sourcemarker.activities

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import spp.jetbrains.marker.plugin.SourceMarkerStartupActivity
import spp.jetbrains.sourcemarker.PluginBundle
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
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
            Logger.getLogger("spp.jetbrains").addAppender(fa)
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
