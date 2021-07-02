package com.sourceplusplus.marker.plugin

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.sourceplusplus.marker.SourceMarker

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerVisibilityAction : AnAction() {

    var visible = true

    override fun actionPerformed(e: AnActionEvent) {
        visible = !visible
        val currentMarks = SourceMarker.getSourceMarks()
        if (currentMarks.isNotEmpty()) {
            currentMarks.forEach { it.setVisible(visible) }
            DaemonCodeAnalyzer.getInstance(e.project).restart()
        }
    }
}
