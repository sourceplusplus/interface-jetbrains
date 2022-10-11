/*
 * Source++, the open-source live coding platform.
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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import io.vertx.core.Vertx
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SynchronousSourceMarkEventListener
import spp.jetbrains.monitor.skywalking.bridge.ServiceBridge
import spp.jetbrains.safeLaunch

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PluginSourceMarkEventListener(val project: Project, val vertx: Vertx) : SynchronousSourceMarkEventListener {

    //todo: find a better place for this
    init {
        //refresh source marks on service changes
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
    }

    override fun handleEvent(event: SourceMarkEvent) = Unit
}
