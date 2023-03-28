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

/**
 * Represents the current status of the Source++ plugin.
 *
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class SourceStatus {
    Enabled,
    Disabled,
    Pending,
    ConnectionError,
    ServiceChange,
    PluginsLoaded;

    val icon: Icon
        get() {
            return when (this) {
                Enabled, Pending -> PluginIcons.statusPending
                Disabled -> PluginIcons.statusDisabled
                ConnectionError -> PluginIcons.statusFailed
                else -> PluginIcons.statusEnabled
            }
        }
    val presentableText: String
        get() {
            return when (this) {
                Disabled -> "Click to enable Source++"
                Pending -> "Booting Source++"
                ConnectionError -> "Connection error"
                else -> "Click to disable Source++"
            }
        }

    val disposedPlugin: Boolean
        get() = this in DISPOSED_STATUSES

    val isConnected: Boolean
        get() = this in CONNECTED_STATUSES

    val isEnabled: Boolean
        get() = this != Disabled

    val isReady: Boolean
        get() = this in READY_STATUSES

    companion object {
        val READY_STATUSES = setOf(PluginsLoaded, ServiceChange)
        val CONNECTED_STATUSES = setOf(PluginsLoaded, ServiceChange, Pending)
        val DISPOSED_STATUSES = setOf(Disabled, ConnectionError)
    }
}
