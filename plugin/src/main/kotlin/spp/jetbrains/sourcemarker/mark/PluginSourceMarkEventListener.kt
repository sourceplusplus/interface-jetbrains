/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022 CodeBrig, Inc.
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
package spp.jetbrains.sourcemarker.mark

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import io.vertx.kotlin.coroutines.CoroutineVerticle
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarkerKeys.VCS_MODIFIED
import spp.jetbrains.marker.service.ArtifactVersionService
import spp.jetbrains.marker.service.SourceGuideProvider
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode.CODE_CHANGED
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.monitor.skywalking.bridge.ServiceBridge
import spp.jetbrains.safeLaunch

/**
 * Manages the [VCS_MODIFIED] flag and emits corresponding [CODE_CHANGED] events.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PluginSourceMarkEventListener(val project: Project) : CoroutineVerticle(), Disposable {

    private val log = logger<PluginSourceMarkEventListener>()
    private val codeChangeQueue = MergingUpdateQueue(
        "SPP_CODE_CHANGED", 500, true, null, this
    ).setRestartTimerOnAdd(true)
    private val lastModificationStamp = Key.create<Long>(this::class.simpleName + "_LAST_MODIFICATION_STAMP")

    override suspend fun start() {
        //todo: find a better place for this
        //refresh all source marks on service changes
        ServiceBridge.currentServiceConsumer(vertx).handler {
            vertx.safeLaunch {
                SourceMarker.getInstance(project).clearAvailableSourceFileMarkers()
                FileEditorManager.getInstance(project).allEditors.forEach {
                    ApplicationManager.getApplication().runReadAction {
                        PsiManager.getInstance(project).findFile(it.file)?.let {
                            SourceMarker.getSourceFileMarker(it)
                            DaemonCodeAnalyzer.getInstance(project).restart(it)
                        }
                    }
                }
            }
        }

        //refresh guide marks on code changes
        val messageBusConnection = this.project.messageBus.connect(this)
        messageBusConnection.subscribe(PsiModificationTracker.TOPIC, PsiModificationTracker.Listener {
            SourceMarker.getInstance(project).getSourceFileMarkers().forEach { fileMarker ->
                codeChangeQueue.queue(Update.create(fileMarker.psiFile) {
                    detectChanges(fileMarker)
                })
            }
        })

        //run detectChanges on files already opened
        ApplicationManager.getApplication().runReadAction {
            FileEditorManager.getInstance(project).selectedEditors.forEach {
                val psiFile = PsiManager.getInstance(project).findFile(it.file) ?: return@forEach
                val fileMarker = SourceMarker.getSourceFileMarker(psiFile) ?: return@forEach
                codeChangeQueue.queue(Update.create(fileMarker.psiFile) {
                    fileMarker.psiFile.putUserData(lastModificationStamp, null) //reset
                    detectChanges(fileMarker)
                })
            }
        }

        //run detectChanges on file open
        messageBusConnection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return
                    val fileMarker = SourceMarker.getSourceFileMarker(psiFile) ?: return
                    codeChangeQueue.queue(Update.create(fileMarker.psiFile) {
                        fileMarker.psiFile.putUserData(lastModificationStamp, null) //reset
                        detectChanges(fileMarker)
                    })
                }
            })
    }

    private fun detectChanges(fileMarker: SourceFileMarker) {
        log.trace("Detecting changes in $fileMarker")
        val count = fileMarker.psiFile.getUserData(lastModificationStamp) ?: 0L
        val newCount = fileMarker.psiFile.modificationStamp
        if (newCount != count) {
            fileMarker.psiFile.putUserData(lastModificationStamp, newCount)
            updateChanges(fileMarker)
        }
    }

    private fun updateChanges(fileMarker: SourceFileMarker) {
        log.info("Updating changes in $fileMarker")

        //add new guide marks
        SourceGuideProvider.determineGuideMarks(fileMarker)

        //detect changed guide marks
        val changedElements = ArtifactVersionService.getChangedFunctions(fileMarker.psiFile)
        val changedGuideMarks = changedElements
            .mapNotNull { it.nameIdentifier?.getUserData(SourceKey.GuideMark) }
        changedGuideMarks.forEach {
            it.putUserData(VCS_MODIFIED, true)
        }
        changedGuideMarks.forEach {
            it.triggerEvent(CODE_CHANGED, listOf())
        }
    }

    override suspend fun stop() = Disposer.dispose(this)
    override fun dispose() = Unit
}
