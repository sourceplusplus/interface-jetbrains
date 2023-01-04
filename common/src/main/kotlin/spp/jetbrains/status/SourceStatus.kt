/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.status

import spp.jetbrains.icons.PluginIcons
import javax.swing.Icon

enum class SourceStatus {
    Ready,
    Enabled,
    Disabled,
    Pending,
    ConnectionError;

    val icon: Icon
        get() {
            return when (this) {
                Ready -> PluginIcons.statusEnabled
                Enabled -> PluginIcons.statusPending
                Disabled -> PluginIcons.statusDisabled
                Pending -> PluginIcons.statusPending
                ConnectionError -> PluginIcons.statusFailed
            }
        }
    val presentableText: String
        get() {
            return when (this) {
                Ready -> "Click to disable Source++"
                Enabled -> "Click to disable Source++"
                Disabled -> "Click to enable Source++"
                Pending -> "Booting Source++"
                ConnectionError -> "Connection error"
            }
        }

    val disposedPlugin: Boolean
        get() = this == Disabled || this == ConnectionError
}
