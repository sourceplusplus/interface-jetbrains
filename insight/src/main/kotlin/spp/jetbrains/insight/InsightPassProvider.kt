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
package spp.jetbrains.insight

import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.insight.pass.ArtifactPass
import spp.jetbrains.insight.pass.IPass
import spp.jetbrains.insight.pass.ProceduralMultiPathPass
import spp.jetbrains.insight.pass.ProceduralPathPass
import spp.jetbrains.insight.pass.artifact.CallDurationPass
import spp.jetbrains.insight.pass.artifact.LoadPsiPass
import spp.jetbrains.insight.pass.artifact.RandomConditionalPass
import spp.jetbrains.insight.pass.artifact.ThreadSleepPass
import spp.jetbrains.insight.pass.multipath.SavePsiMultiPathPass
import spp.jetbrains.insight.pass.multipath.SimplifyMultiPathPass
import spp.jetbrains.insight.pass.multipath.StaticDfaMultiPathPass
import spp.jetbrains.insight.pass.path.PathDurationPass
import spp.jetbrains.insight.pass.path.PathProbabilityPass
import spp.jetbrains.insight.pass.path.PruneArtifactsPass
import spp.jetbrains.insight.pass.path.RecursivePathPass
import spp.jetbrains.insight.path.ProceduralMultiPath
import spp.jetbrains.insight.path.ProceduralPath

/**
 * Used to process passes over [ProceduralMultiPath]s, [ProceduralPath]s, and [ArtifactElement]s.
 */
class InsightPassProvider {

    companion object {
        val ALL_PASSES = listOf(
            //artifact passes
            LoadPsiPass(),
            RandomConditionalPass(),
            CallDurationPass(),
            ThreadSleepPass(),

            //path passes
            PruneArtifactsPass(),
            PathProbabilityPass(),
            PathDurationPass(),
            RecursivePathPass(),

            //multi path passes
            StaticDfaMultiPathPass(),
            SimplifyMultiPathPass(),
            SavePsiMultiPathPass()
        )

        val FULL = InsightPassProvider().apply {
            ALL_PASSES.forEach { registerPass(it) }
        }
        val FULL_NO_SIMPLIFY = InsightPassProvider().apply {
            ALL_PASSES.filter { it !is SimplifyMultiPathPass }.forEach { registerPass(it) }
        }
    }

    private val artifactPasses = mutableListOf<ArtifactPass>()
    private val pathPasses = mutableListOf<ProceduralPathPass>()
    private val multiPathPasses = mutableListOf<ProceduralMultiPathPass>()

    fun registerPass(pass: IPass) {
        when (pass) {
            is ArtifactPass -> artifactPasses.add(pass)
            is ProceduralPathPass -> pathPasses.add(pass)
            is ProceduralMultiPathPass -> multiPathPasses.add(pass)
            else -> throw IllegalArgumentException("Unknown pass type: ${pass::class}")
        }
    }

    private fun analyze(element: ArtifactElement) {
        artifactPasses.forEach { it.analyze(element) }
    }

    private fun analyze(path: ProceduralPath) {
        path.forEach { analyze(it) }
        pathPasses.forEach { it.analyze(path) }
    }

    fun analyze(multiPath: ProceduralMultiPath): ProceduralMultiPath {
        val preProcessedMultiPath = multiPathPasses.fold(multiPath) { acc, pass ->
            pass.preProcess(acc)
        }
        preProcessedMultiPath.map { analyze(it) }
        val analyzedMultiPath = multiPathPasses.fold(preProcessedMultiPath) { acc, pass ->
            pass.analyze(acc)
        }
        return multiPathPasses.fold(analyzedMultiPath) { acc, pass ->
            pass.postProcess(acc)
        }
    }
}
