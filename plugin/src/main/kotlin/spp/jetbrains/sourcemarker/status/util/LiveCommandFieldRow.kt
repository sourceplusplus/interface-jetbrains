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
package spp.jetbrains.sourcemarker.status.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.Key
import spp.command.LiveCommand
import java.io.File
import javax.swing.Icon

class LiveCommandFieldRow(val liveCommand: LiveCommand, val project: Project) : AutocompleteFieldRow {

    private val basePath = project.basePath?.let { File(it, ".spp${File.separatorChar}commands").absolutePath } ?: ""
    private val internalBasePath = Key.findKeyByName("SPP_COMMANDS_LOCATION")
        ?.let { key -> project.getUserData(key).toString() } ?: ""

    override fun getText(): String = liveCommand.name
    override fun getDescription(): String = liveCommand.description

    override fun getSelectedIcon(): Icon? {
        return liveCommand.selectedIcon?.let {
            val iconPath = if (File(internalBasePath, it).exists()) {
                internalBasePath + File.separator + it
            } else if (File(basePath, it).exists()) {
                basePath + File.separator + it
            } else {
                it
            }
            IconLoader.findIcon(File(iconPath).toURL())
        }
    }

    override fun getUnselectedIcon(): Icon? {
        return liveCommand.unselectedIcon?.let {
            val iconPath = if (File(internalBasePath, it).exists()) {
                internalBasePath + File.separator + it
            } else if (File(basePath, it).exists()) {
                basePath + File.separator + it
            } else {
                it
            }
            IconLoader.findIcon(File(iconPath).toURL())
        }
    }
}
