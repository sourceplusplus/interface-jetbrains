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
package spp.jetbrains.marker.indicator

import com.apollographql.apollo3.exception.ApolloException
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.observable.util.whenDisposed
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.removeUserData
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import spp.jetbrains.UserData
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.indicator.ui.StickyNotificationPanel
import spp.jetbrains.marker.plugin.LiveStatusBarManager
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.mark.api.event.IEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.safeLaunch
import spp.jetbrains.status.SourceStatus.ConnectionError
import spp.jetbrains.status.SourceStatusService
import spp.protocol.instrument.event.LiveBreakpointHit
import spp.protocol.instrument.event.LiveInstrumentEvent
import spp.protocol.instrument.event.LiveInstrumentEventType
import spp.protocol.platform.developer.SelfInfo
import spp.protocol.service.SourceServices

@Suppress("unused")
abstract class LiveIndicator(val project: Project) : Disposable {

    val log by lazy { Logger.getInstance("spp.jetbrains.indicator." + this::class.java.simpleName) }
    open val listenForAllEvents: Boolean = false
    open val listenForEvents: List<IEventCode> = emptyList()

    private var periodicTimerId = -1L
    val dumbService = DumbService.getInstance(project)
    val vertx = UserData.vertx(project)

    @Deprecated("Use LiveViewService instead")
    val managementService = UserData.liveManagementService(project)
    val viewService = UserData.liveViewService(project)!!
    val statusManager = LiveStatusBarManager.getInstance(project)
    val instrumentService = UserData.liveInstrumentService(project)!! //todo: throw exception to all calls
    val statusService = SourceStatusService.getInstance(project)
    val selfInfo: SelfInfo
        get() = UserData.selfInfo(project)!!

    open suspend fun onRegister() {
        vertx.setPeriodic(5000) { timerId ->
            periodicTimerId = timerId
            vertx.safeLaunch {
                if (!SourceStatusService.getInstance(project).isReady()) {
                    log.debug("Not ready, ignoring indicator refresh")
                    return@safeLaunch
                }

                try {
                    refreshIndicator()
                } catch (ex: ApolloException) {
                    log.warn("Error refreshing indicator", ex)
                    SourceStatusService.getInstance(project)
                        .update(ConnectionError, "Unable to connect to platform")
                }
            }
        }
    }

    open suspend fun onUnregister() {
        vertx.cancelTimer(periodicTimerId)
    }

    open suspend fun refreshIndicator() = Unit
    open suspend fun trigger(guideMark: GuideMark, event: SourceMarkEvent) = Unit

    override fun dispose() {
        TODO("Not yet implemented")
    }

    fun addBreakpointHitListener(
        id: String,
        listener: (LiveBreakpointHit) -> Unit
    ): MessageConsumer<JsonObject> {
        val consumer = vertx.eventBus().consumer<JsonObject>(
            SourceServices.Subscribe.toLiveInstrumentSubscriberAddress(
                selfInfo.developer.id
            )
        )
        consumer.handler {
            val event = LiveInstrumentEvent.fromJson(it.body())
            if (event.instrument.id == id && event.eventType == LiveInstrumentEventType.BREAKPOINT_HIT) {
                listener.invoke(event as LiveBreakpointHit)
            }
        }
        return consumer
    }

    fun showInEditor(
        notification: StickyNotificationPanel
    ) = ApplicationManager.getApplication().invokeLater {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val editor = fileEditorManager.selectedEditor ?: return@invokeLater
        if (editor.getUserData(StickyNotificationPanel.PSI_KEY) != null) return@invokeLater
        editor.putUserData(StickyNotificationPanel.PSI_KEY, notification)
        whenDisposed { editor.removeUserData(StickyNotificationPanel.PSI_KEY) }

        notification.setCloseAction {
            fileEditorManager.removeTopComponent(editor, notification)
            editor.removeUserData(StickyNotificationPanel.PSI_KEY)
        }
        fileEditorManager.addTopComponent(editor, notification)
    }

    fun showStickyInEditor(
        notification: StickyNotificationPanel
    ) = ApplicationManager.getApplication().invokeLater {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val editor = fileEditorManager.selectedEditor ?: return@invokeLater
        if (editor.getUserData(StickyNotificationPanel.PSI_KEY) != null) return@invokeLater
        editor.putUserData(StickyNotificationPanel.PSI_KEY, notification)
        whenDisposed { editor.removeUserData(StickyNotificationPanel.PSI_KEY) }

        notification.setCloseAction {
            fileEditorManager.removeTopComponent(editor, notification)
            editor.removeUserData(StickyNotificationPanel.PSI_KEY)
        }
        fileEditorManager.addTopComponent(editor, notification)

        project.messageBus.connect(this)
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    fileEditorManager.removeTopComponent(editor, notification)
                    editor.removeUserData(StickyNotificationPanel.PSI_KEY)
                    fileEditorManager.selectedEditor?.removeUserData(StickyNotificationPanel.PSI_KEY)
                    showInEditor(notification)
                }
            })
    }

    fun clearStickyInEditor() = ApplicationManager.getApplication().invokeLater {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val editor = fileEditorManager.selectedEditor ?: return@invokeLater
        val stickyNotification = editor.getUserData(StickyNotificationPanel.PSI_KEY) ?: return@invokeLater
        fileEditorManager.removeTopComponent(editor, stickyNotification)
        editor.removeUserData(StickyNotificationPanel.PSI_KEY)
    }

//    fun findIcon(path: String): Icon {
//        val iconPath = if (File(pluginPath, path).exists()) {
//            pluginPath + File.separator + path
//        } else {
//            path
//        }
//        return IconLoader.findIcon(File(iconPath).toURL())
//    }

    fun findByEndpointName(endpointName: String): GuideMark? {
        return SourceMarker.getInstance(project).getSourceMarks().filterIsInstance<GuideMark>().firstOrNull {
            it.getUserData(EndpointDetector.DETECTED_ENDPOINTS)?.any { it.name == endpointName } == true
        }
    }
}
