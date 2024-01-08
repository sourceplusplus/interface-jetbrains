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
package spp.jetbrains.marker.source.mark.api.component.jcef

import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponentProvider
import spp.jetbrains.marker.source.mark.api.component.jcef.config.SourceMarkJcefComponentConfiguration
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.gutter.event.GutterMarkEventCode
import spp.jetbrains.marker.source.mark.inlay.InlayMark

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class SourceMarkJcefComponentProvider : SourceMarkComponentProvider, SourceMarkEventListener {

    override val defaultConfiguration = SourceMarkJcefComponentConfiguration()

    override fun getComponent(sourceMark: SourceMark): SourceMarkJcefComponent {
        sourceMark.addEventListener(this)
        return SourceMarkJcefComponent(defaultConfiguration.copy())
    }

    override fun disposeComponent(sourceMark: SourceMark) {
        sourceMark.sourceMarkComponent.dispose()
    }

    private fun initializeComponent(sourceMark: SourceMark) {
        val jcefComponent = sourceMark.sourceMarkComponent as SourceMarkJcefComponent
        if (jcefComponent.configuration.preloadJcefBrowser) {
            jcefComponent.initialize()
        }
    }

    override fun handleEvent(event: SourceMarkEvent) {
        when (event.eventCode) {
            SourceMarkEventCode.MARK_REMOVED -> {
                disposeComponent(event.sourceMark)
            }
            GutterMarkEventCode.GUTTER_MARK_VISIBLE -> {
                initializeComponent(event.sourceMark)
            }
            SourceMarkEventCode.MARK_ADDED -> {
                if (event.sourceMark.configuration.activateOnKeyboardShortcut || event.sourceMark is InlayMark) {
                    initializeComponent(event.sourceMark)
                }
            }
        }
    }
}
