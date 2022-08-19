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
package liveplugin.implementation.plugin

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
import spp.protocol.service.listen.LiveInstrumentEventListener
import spp.protocol.service.listen.LiveViewEventListener

interface LiveStatusManager {
    fun showBreakpointStatusBar(editor: Editor, lineNumber: Int)
    fun showLogStatusBar(editor: Editor, lineNumber: Int, watchExpression: Boolean)
    fun showMeterStatusBar(editor: Editor, lineNumber: Int)
    fun showSpanStatusBar(editor: Editor, lineNumber: Int)
    fun showBreakpointStatusBar(liveBreakpoint: LiveBreakpoint, fileMarker: SourceFileMarker)
    fun showLogStatusBar(liveLog: LiveLog, fileMarker: SourceFileMarker)
    fun showMeterStatusIcon(liveMeter: LiveMeter, sourceFileMarker: SourceFileMarker)

    fun addStatusBar(sourceMark: SourceMark, listener: LiveInstrumentEventListener)
    fun addViewEventListener(sourceMark: SourceMark, listener: LiveViewEventListener)
    fun addActiveLiveInstrument(instrument: LiveInstrument)
    fun addActiveLiveInstruments(instruments: List<LiveInstrument>)
    fun removeActiveLiveInstrument(instrument: LiveInstrument)
    fun getLogData(inlayMark: InlayMark): List<*>
    fun removeLogData(inlayMark: InlayMark)

    companion object {
        val KEY = Key.create<LiveStatusManager>("SPP_LIVE_STATUS_MANAGER")

        @JvmStatic
        fun getInstance(project: Project): LiveStatusManager {
            return project.getUserData(KEY)!!
        }
    }
}
