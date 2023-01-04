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
package spp.jetbrains.marker.source.mark.inlay

import com.intellij.openapi.util.Key
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.inlay.config.InlayMarkConfiguration
import spp.jetbrains.marker.source.mark.inlay.event.InlayMarkEventCode
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [SourceMark] which adds visualizations inside source code text.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface InlayMark : SourceMark {

    companion object {
        val KEY = Key.create<Set<InlayMark>>("sm.InlayMarks")
    }

    override val type: SourceMark.Type
        get() = SourceMark.Type.INLAY
    override val configuration: InlayMarkConfiguration
    val visible: AtomicBoolean

    override fun isVisible(): Boolean {
        return visible.get()
    }

    override fun setVisible(visible: Boolean) {
        val previousVisibility = this.visible.getAndSet(visible)
        if (visible && !previousVisibility) {
            triggerEvent(SourceMarkEvent(this, InlayMarkEventCode.INLAY_MARK_VISIBLE))
        } else if (!visible && previousVisibility) {
            triggerEvent(SourceMarkEvent(this, InlayMarkEventCode.INLAY_MARK_HIDDEN))
        }
    }
}
