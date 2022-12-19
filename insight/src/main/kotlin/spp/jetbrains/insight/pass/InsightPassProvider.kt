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
package spp.jetbrains.insight.pass

import spp.jetbrains.insight.RuntimePathImpl
import spp.jetbrains.insight.pass.artifact.CallDurationPass
import spp.jetbrains.insight.pass.artifact.ThreadSleepPass
import spp.jetbrains.insight.pass.path.PathDurationPass
import spp.jetbrains.insight.pass.path.PathProbabilityPass
import spp.jetbrains.marker.model.ArtifactElement

class InsightPassProvider {

    companion object {
        val DEFAULT = InsightPassProvider().apply {
            //artifact passes
            addArtifactPass(CallDurationPass())
            addArtifactPass(ThreadSleepPass())

            //path passes
            addRuntimePathPass(PathDurationPass())
            addRuntimePathPass(PathProbabilityPass())
        }
    }

    private val artifactPasses = mutableListOf<ArtifactPass>()
    private val runtimePathPasses = mutableListOf<RuntimePathPass>()
    private val runtimePathSetPasses = mutableListOf<RuntimePathSetPass>()

    fun addArtifactPass(pass: ArtifactPass) {
        artifactPasses.add(pass)
    }

    fun addRuntimePathPass(pass: RuntimePathPass) {
        runtimePathPasses.add(pass)
    }

    fun addRuntimePathSetPass(pass: RuntimePathSetPass) {
        runtimePathSetPasses.add(pass)
    }

    fun analyze(element: ArtifactElement) {
        artifactPasses.forEach { it.analyze(element) }
    }

    fun analyze(path: RuntimePathImpl) {
        path.forEach { analyze(it) }
        runtimePathPasses.forEach { it.analyze(path) }
    }

    fun analyze(pathSet: Set<RuntimePathImpl>) {
        pathSet.forEach { analyze(it) }
        runtimePathSetPasses.forEach { it.analyze(pathSet) }
    }
}
