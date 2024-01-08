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
package spp.jetbrains.marker.source.mark.gutter

import com.intellij.openapi.util.Key
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.gutter.config.GutterMarkConfiguration

/**
 * A [SourceMark] which adds visualizations in the panel to the left of source code.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface GutterMark : SourceMark {

    companion object {
        val KEY = Key.create<GutterMark>("sm.GutterMark")
    }

    override val type: SourceMark.Type
        get() = SourceMark.Type.GUTTER
    override val configuration: GutterMarkConfiguration

    override fun isVisible(): Boolean
    override fun setVisible(visible: Boolean)
}
