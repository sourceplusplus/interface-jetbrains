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
package spp.jetbrains.marker.source.mark.api.component.jcef

import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.gutter.event.GutterMarkEventCode

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkSingleJcefComponentProvider : SourceMarkJcefComponentProvider(), SourceMarkEventListener {

    companion object {
        var singleton: SourceMarkSingleJcefComponentProvider? = null
    }

    init {
        singleton?.jcefComponent?.dispose()
        singleton = this
    }

    val jcefComponent: SourceMarkJcefComponent by lazy {
        SourceMarkJcefComponent(defaultConfiguration.copy())
    }

    override fun getComponent(sourceMark: SourceMark): SourceMarkJcefComponent {
        sourceMark.addEventListener(this)
        return jcefComponent
    }

    override fun disposeComponent(sourceMark: SourceMark) {
        //do nothing
    }

    override fun handleEvent(event: SourceMarkEvent) {
        when (event.eventCode) {
            GutterMarkEventCode.GUTTER_MARK_VISIBLE -> super.handleEvent(event)
            SourceMarkEventCode.MARK_ADDED -> super.handleEvent(event)
        }
    }
}
