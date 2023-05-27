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
package spp.jetbrains.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.observable.util.whenDisposed
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.removeUserData
import com.intellij.ui.EditorNotificationPanel
import com.intellij.util.messages.SimpleMessageBusConnection
import spp.jetbrains.SourceKey
import java.awt.Color

@Suppress("unused", "MemberVisibilityCanBePrivate")
class StickyNotificationPanel(
    parentDisposable: Disposable,
    val project: Project,
    background: Color? = null,
    status: Status = Status.Info
) : EditorNotificationPanel(background, status), Disposable {

    private val selfKey = SourceKey<StickyNotificationPanel>(
        this::class.simpleName.toString() + "@" + System.identityHashCode(this)
    ).asPsiKey()
    private var display = false
    private val busConnection: SimpleMessageBusConnection

    init {
        Disposer.register(parentDisposable, this)
        whenDisposed { hideSticky() }
        setCloseAction { hideSticky() }

        busConnection = project.messageBus.connect(this)
        busConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) {
                if (display) {
                    showSticky()
                }
            }
        })
    }

    fun addCloseAction() {
        setCloseAction { hideSticky() }
    }

    fun showSticky() {
        display = true
        val fileEditorManager = FileEditorManager.getInstance(project)
        hideFromNonDisplayingTabs(fileEditorManager)
        showInDisplayingTabs(fileEditorManager)
    }

    fun hideSticky() {
        display = false
        val fileEditorManager = FileEditorManager.getInstance(project)
        hideFromNonDisplayingTabs(fileEditorManager)
    }

    private fun hideFromNonDisplayingTabs(fileEditorManager: FileEditorManager) {
        for (editor in fileEditorManager.allEditors) {
            if (editor.removeUserData(selfKey) != null) {
                fileEditorManager.removeTopComponent(editor, this)
            }
        }
    }

    private fun showInDisplayingTabs(fileEditorManager: FileEditorManager) {
        val editor = fileEditorManager.selectedEditor ?: return
        if (editor.getUserData(selfKey) != null) return
        editor.putUserData(selfKey, this)
        fileEditorManager.addTopComponent(editor, this)
    }

    override fun dispose() {
        hideSticky()
        busConnection.disconnect()
    }
}
