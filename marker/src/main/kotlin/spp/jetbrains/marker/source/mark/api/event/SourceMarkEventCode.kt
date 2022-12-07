/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022 CodeBrig, Inc.
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
package spp.jetbrains.marker.source.mark.api.event

import spp.jetbrains.marker.source.mark.api.SourceMark

/**
 * Represents general [SourceMark] events.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
enum class SourceMarkEventCode(private val code: Int) : IEventCode {
    MARK_ADDED(1000),
    MARK_BEFORE_ADDED(1001),
    MARK_REMOVED(1002),
    NAME_CHANGED(1003),
    PORTAL_OPENING(1004),
    PORTAL_OPENED(1005),
    PORTAL_CLOSED(1006),
    UPDATE_PORTAL_CONFIG(1007),
    MARK_USER_DATA_UPDATED(1008),
    CHILD_USER_DATA_UPDATED(1009),
    CUSTOM_EVENT(1010),
    CODE_CHANGED(1011);

    override fun code(): Int {
        return this.code
    }

    companion object {
        fun fromName(name: String): SourceMarkEventCode? {
            return values().firstOrNull { it.name == name }
        }
    }
}
