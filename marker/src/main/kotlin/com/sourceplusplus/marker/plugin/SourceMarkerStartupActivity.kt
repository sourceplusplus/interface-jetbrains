package com.sourceplusplus.marker.plugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class SourceMarkerStartupActivity : StartupActivity {

    override fun runActivity(project: Project) {
        DumbService.getInstance(project).runReadActionInSmartMode {
            ApplicationManager.getApplication().invokeLater {
                val editorManager = FileEditorManager.getInstance(project)
                editorManager.allEditors.forEach { editor ->
                    FileActivityListener.triggerFileOpened(editorManager, editor.file!!)
                }
            }
        }
    }
}
