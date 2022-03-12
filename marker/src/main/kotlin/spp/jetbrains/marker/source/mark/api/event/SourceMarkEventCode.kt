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
    MARK_USER_DATA_UPDATED(1008);

    override fun code(): Int {
        return this.code
    }
}
