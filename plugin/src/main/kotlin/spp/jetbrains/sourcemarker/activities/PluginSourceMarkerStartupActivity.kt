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
package spp.jetbrains.sourcemarker.activities

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import spp.jetbrains.ScopeExtensions.safeRunBlocking
import spp.jetbrains.marker.plugin.SourceMarkerStartupActivity
import spp.jetbrains.sourcemarker.SourceMarkerPlugin

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PluginSourceMarkerStartupActivity : SourceMarkerStartupActivity() {

    override fun runActivity(project: Project) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return //tests manually set up necessary components
        }

        //setup plugin
        safeRunBlocking {
            SourceMarkerPlugin.getInstance(project).init()
        }
        super.runActivity(project)
    }
}
