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
package spp.jetbrains.marker.plugin.action

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.mark.guide.GuideMark

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerVisibilityAction : AnAction() {

    companion object {
        var globalVisibility = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        globalVisibility = !globalVisibility
        val currentMarks = SourceMarker.getInstance(e.project!!).getSourceMarks().filter { it !is GuideMark }
        if (currentMarks.isNotEmpty()) {
            currentMarks.forEach { it.setVisible(globalVisibility) }
            DaemonCodeAnalyzer.getInstance(e.project).restart()
        }
    }
}
