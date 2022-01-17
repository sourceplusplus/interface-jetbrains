package spp.jetbrains.sourcemarker.command

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import spp.jetbrains.sourcemarker.status.LiveStatusManager

class EscapeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        LiveStatusManager.removeInactiveStatusBars()
    }

}