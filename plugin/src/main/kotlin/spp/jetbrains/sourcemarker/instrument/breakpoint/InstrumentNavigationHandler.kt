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
package spp.jetbrains.sourcemarker.instrument.breakpoint

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import spp.jetbrains.UserData
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.sourcemarker.instrument.InstrumentEventWindowService
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

/**
 * todo: description.
 *
 * @since 0.7.7
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class InstrumentNavigationHandler(
    private val gutterMark: GutterMark,
    private val canRemove: Boolean = true
) : GutterIconNavigationHandler<PsiElement> {

    private val log = logger<InstrumentNavigationHandler>()

    override fun navigate(e: MouseEvent, elt: PsiElement) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            //select in overview tab
            val instrumentId = gutterMark.getUserData(SourceMarkerKeys.INSTRUMENT_ID) ?: return
            InstrumentEventWindowService.getInstance(gutterMark.project).selectInOverviewTab(instrumentId)
        } else if (SwingUtilities.isRightMouseButton(e)) {
            //show the instrument configuration popup
            val instrumentId = gutterMark.getUserData(SourceMarkerKeys.INSTRUMENT_ID) ?: return
            val actions = mutableListOf<AnAction>(object : AnAction("Show Events") {
                override fun actionPerformed(e: AnActionEvent) {
                    InstrumentEventWindowService.getInstance(gutterMark.project).showInstrumentEvents(instrumentId)
                }
            })
            if (canRemove) {
                actions.add(object : AnAction("Remove Instrument") {
                    override fun actionPerformed(e: AnActionEvent) {
                        UserData.liveInstrumentService(gutterMark.project)?.removeLiveInstrument(instrumentId)
                            ?.onSuccess {
                                gutterMark.dispose()
                            }?.onFailure {
                                log.error("Failed to remove live instrument", it)
                            }
                    }
                })
            }

            ActionManager.getInstance()
                .createActionPopupMenu(ActionPlaces.EDITOR_POPUP, DefaultActionGroup(actions)).component
                .show(e.component, e.x, e.y)
        }
    }
}
