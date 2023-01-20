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
package spp.jetbrains.plugin

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.protocol.instrument.LiveBreakpoint
import spp.protocol.instrument.LiveInstrument
import spp.protocol.instrument.LiveLog
import spp.protocol.instrument.LiveMeter
import spp.protocol.service.listen.LiveInstrumentListener
import spp.protocol.service.listen.LiveViewEventListener

interface LiveStatusBarManager {
    fun showBreakpointStatusBar(editor: Editor, lineNumber: Int)
    fun showLogStatusBar(editor: Editor, lineNumber: Int)
    fun showMeterStatusBar(editor: Editor, lineNumber: Int)
    fun showSpanStatusBar(editor: Editor, lineNumber: Int)
    fun showBreakpointStatusBar(liveBreakpoint: LiveBreakpoint, fileMarker: SourceFileMarker)
    fun showLogStatusBar(liveLog: LiveLog, fileMarker: SourceFileMarker)
    fun showMeterStatusIcon(liveMeter: LiveMeter, sourceFileMarker: SourceFileMarker)
    fun addStatusBar(sourceMark: SourceMark, listener: LiveInstrumentListener)

    @Deprecated("Subscribe to view id instead")
    fun addViewEventListener(sourceMark: SourceMark, listener: LiveViewEventListener)

    fun addActiveLiveInstrument(instrument: LiveInstrument)
    fun addActiveLiveInstruments(instruments: List<LiveInstrument>)
    fun removeActiveLiveInstrument(instrument: LiveInstrument)
    fun getLogData(inlayMark: InlayMark): List<*>
    fun removeLogData(inlayMark: InlayMark)

    companion object {
        val KEY = Key.create<LiveStatusBarManager>("SPP_LIVE_STATUS_BAR_MANAGER")

        @JvmStatic
        fun getInstance(project: Project): LiveStatusBarManager {
            return project.getUserData(KEY)!!
        }
    }
}
