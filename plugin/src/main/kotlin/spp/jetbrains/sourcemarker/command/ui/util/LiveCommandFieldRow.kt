/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.sourcemarker.command.ui.util

import spp.jetbrains.marker.command.LiveCommand
import spp.jetbrains.marker.command.LiveLocationContext
import spp.jetbrains.sourcemarker.command.util.AutocompleteFieldRow
import javax.swing.Icon

class LiveCommandFieldRow(
    val liveCommand: LiveCommand,
    private val context: LiveLocationContext
) : AutocompleteFieldRow {
    override fun getText(): String = liveCommand.getTriggerName()
    override fun getDescription(): String = liveCommand.getDescription(context)
    override fun getSelectedIcon(): Icon? = liveCommand.selectedIcon
    override fun getUnselectedIcon(): Icon? = liveCommand.unselectedIcon
}
