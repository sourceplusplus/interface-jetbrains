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

import com.intellij.psi.PsiElement
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.plugin.action.SourceMarkerVisibilityAction
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.ExpressionSourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.gutter.event.GutterMarkEventCode
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Represents a [GutterMark] associated to an expression artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ExpressionGutterMark(
    override val sourceFileMarker: SourceFileMarker,
    psiExpression: PsiElement
) : ExpressionSourceMark(sourceFileMarker, psiExpression), GutterMark {

    override val id = UUID.randomUUID().toString()
    override val configuration = SourceMarker.getInstance(project).configuration.gutterMarkConfiguration.copy()
    private var visible: AtomicBoolean = AtomicBoolean(SourceMarkerVisibilityAction.globalVisibility)

    override fun isVisible(): Boolean {
        return visible.get()
    }

    override fun setVisible(visible: Boolean) {
        val previousVisibility = this.visible.getAndSet(visible)
        if (visible && !previousVisibility) {
            triggerEvent(SourceMarkEvent(this, GutterMarkEventCode.GUTTER_MARK_VISIBLE))
        } else if (!visible && previousVisibility) {
            triggerEvent(SourceMarkEvent(this, GutterMarkEventCode.GUTTER_MARK_HIDDEN))
        }
    }
}
