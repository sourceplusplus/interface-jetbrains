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
package spp.jetbrains.marker.source.mark.guide

import com.intellij.openapi.util.Key
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.component.tooltip.LiveTooltip
import spp.jetbrains.marker.source.mark.api.component.tooltip.TextLiveTooltip
import spp.jetbrains.marker.source.mark.guide.config.GuideMarkConfiguration
import javax.swing.JPanel

/**
 * A [SourceMark] with no visual display used to associating high-level structures with their underlying code.
 * For example, a [GuideMark] can be used to associate a method with the REST endpoint(s) it provides.
 *
 * @since 0.4.7
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface GuideMark : SourceMark {

    companion object {
        val KEY = Key.create<GuideMark>("sm.GuideMark")
    }

    override val type: SourceMark.Type
        get() = SourceMark.Type.GUIDE
    override val configuration: GuideMarkConfiguration

    //todo: remove
    override fun isVisible(): Boolean = false
    override fun setVisible(visible: Boolean): Unit = throw UnsupportedOperationException()

    fun setLiveDisplay(panel: JPanel) {
        configuration.liveTooltip = LiveTooltip(this, panel)
    }

    fun setLiveDisplay(text: String) {
        configuration.liveTooltip = TextLiveTooltip(this, text)
    }
}
