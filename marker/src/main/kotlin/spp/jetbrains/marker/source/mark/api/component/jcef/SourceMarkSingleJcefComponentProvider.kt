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
