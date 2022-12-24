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
package spp.jetbrains.insight

import spp.jetbrains.insight.pass.ArtifactPass
import spp.jetbrains.insight.pass.IPass
import spp.jetbrains.insight.pass.RuntimePathPass
import spp.jetbrains.insight.pass.RuntimePathSetPass
import spp.jetbrains.insight.pass.artifact.CallDurationPass
import spp.jetbrains.insight.pass.artifact.ThreadSleepPass
import spp.jetbrains.insight.pass.path.PathDurationPass
import spp.jetbrains.insight.pass.path.PathProbabilityPass
import spp.jetbrains.insight.pass.path.PruneArtifactsPass
import spp.jetbrains.insight.pass.path.RecursivePathPass
import spp.jetbrains.insight.pass.pathset.SimplifyPathSetPass
import spp.jetbrains.marker.model.ArtifactElement

/**
 * Used to process passes over [RuntimePath] sets, [RuntimePath]s, and [ArtifactElement]s.
 */
class InsightPassProvider {

    companion object {
        val ALL_PASSES = listOf(
            //artifact passes
            CallDurationPass(),
            ThreadSleepPass(),

            //path passes
            PruneArtifactsPass(),
            PathProbabilityPass(),
            PathDurationPass(),
            RecursivePathPass(),

            //path set passes
            SimplifyPathSetPass()
        )

        val FULL = InsightPassProvider().apply {
            ALL_PASSES.forEach { registerPass(it) }
        }
        val FULL_NO_SIMPLIFY = InsightPassProvider().apply {
            ALL_PASSES.filter { it !is SimplifyPathSetPass }.forEach { registerPass(it) }
        }
    }

    private val artifactPasses = mutableListOf<ArtifactPass>()
    private val runtimePathPasses = mutableListOf<RuntimePathPass>()
    private val runtimePathSetPasses = mutableListOf<RuntimePathSetPass>()

    fun registerPass(pass: IPass) {
        when (pass) {
            is ArtifactPass -> artifactPasses.add(pass)
            is RuntimePathPass -> runtimePathPasses.add(pass)
            is RuntimePathSetPass -> runtimePathSetPasses.add(pass)
            else -> throw IllegalArgumentException("Unknown pass type: ${pass::class}")
        }
    }

    private fun analyze(element: ArtifactElement) {
        artifactPasses.forEach { it.analyze(element) }
    }

    private fun analyze(path: RuntimePath) {
        path.forEach { analyze(it) }
        runtimePathPasses.forEach { it.analyze(path) }
    }

    fun analyze(pathSet: Set<RuntimePath>): Set<RuntimePath> {
        val preProcessedPathSet = runtimePathSetPasses.fold(pathSet) { acc, pass ->
            pass.preProcess(acc)
        }
        preProcessedPathSet.map { analyze(it) }.toSet()
        val analyzedPathSetSet = runtimePathSetPasses.fold(preProcessedPathSet) { acc, pass ->
            pass.analyze(acc)
        }
        return runtimePathSetPasses.fold(analyzedPathSetSet) { acc, pass ->
            pass.postProcess(acc)
        }
    }
}
