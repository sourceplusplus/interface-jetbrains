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

import spp.jetbrains.insight.RuntimePath
import spp.jetbrains.insight.pass.artifact.CallDurationPass
import spp.jetbrains.insight.pass.artifact.ThreadSleepPass
import spp.jetbrains.insight.pass.path.PathDurationPass
import spp.jetbrains.insight.pass.path.PathProbabilityPass
import spp.jetbrains.insight.pass.path.PruneArtifactsPass
import spp.jetbrains.insight.pass.pathset.ReversePathSetPass
import spp.jetbrains.marker.model.ArtifactElement

/**
 * Used to process passes over [RuntimePath] sets, [RuntimePath]s, and [ArtifactElement]s.
 */
class InsightPassProvider {

    companion object {
        val FULL = InsightPassProvider().apply {
            //artifact passes
            addArtifactPass(CallDurationPass())
            addArtifactPass(ThreadSleepPass())

            //path passes
            addRuntimePathPass(PruneArtifactsPass())
            addRuntimePathPass(PathDurationPass())
            addRuntimePathPass(PathProbabilityPass())

            //path set passes
            addRuntimePathSetPass(ReversePathSetPass())
            //addRuntimePathSetPass(SimplifyPathSetPass())
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

    private fun analyze(element: ArtifactElement) {
        artifactPasses.forEach { it.analyze(element) }
    }

    private fun analyze(path: RuntimePath): RuntimePath {
        path.forEach { analyze(it) }
        return runtimePathPasses.fold(path) { acc, pass ->
            pass.analyze(acc)
        }
    }

    fun analyze(pathSet: Set<RuntimePath>): Set<RuntimePath> {
        val preProcessedPathSet = runtimePathSetPasses.fold(pathSet) { acc, pass ->
            pass.preProcess(acc)
        }
        val analyzedPathSet = preProcessedPathSet.map { analyze(it) }.toSet()
        val analyzedPathSetSet = runtimePathSetPasses.fold(analyzedPathSet) { acc, pass ->
            pass.analyze(acc)
        }
        return runtimePathSetPasses.fold(analyzedPathSetSet) { acc, pass ->
            pass.postProcess(acc)
        }
    }
}
