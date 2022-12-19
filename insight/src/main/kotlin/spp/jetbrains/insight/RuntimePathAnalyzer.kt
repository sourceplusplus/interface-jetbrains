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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import spp.jetbrains.insight.pass.InsightPassProvider
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.marker.model.ArtifactElement
import spp.jetbrains.marker.model.CallArtifact
import spp.jetbrains.marker.model.ControlStructureArtifact
import spp.jetbrains.marker.model.IfArtifact
import spp.jetbrains.marker.model.analysis.IRuntimePath
import spp.jetbrains.marker.service.getParentFunction
import spp.jetbrains.marker.service.toArtifact
import java.util.*

class RuntimePathAnalyzer {

    private val queue: Queue<PathSplit> = LinkedList()
    private val paths: MutableList<RuntimePathImpl> = mutableListOf()
    var savePathsToPsi: Boolean = true

    fun analyze(element: ArtifactElement): Set<IRuntimePath> {
        analyze(mutableListOf(), mutableListOf(), element)

        while (queue.isNotEmpty()) {
            val pathSplit = queue.remove()
            analyze(
                pathSplit.conditions.toMutableList(),
                pathSplit.artifacts.toMutableList(),
                pathSplit.artifacts.last()
            )
        }

        val paths = clean(paths)
        InsightPassProvider.DEFAULT.analyze(paths)

        if (savePathsToPsi) {
            element.putUserData(SourceMarkerKeys.RUNTIME_PATHS.asPsiKey(), paths)
        }
        return paths
    }

    fun analyzeUp(psi: ArtifactElement): Set<IRuntimePath> {
        //todo: do this more efficiently
        val paths = analyze(psi.getParentFunction().toArtifact()!!)
        return paths.filter {
            it.artifacts.contains(psi)
        }.toSet()
    }

    /**
     * Remove non-control structure, non-call artifacts from the paths.
     */
    private fun clean(paths: MutableList<RuntimePathImpl>): Set<RuntimePathImpl> {
        return paths.map { path ->
            path.copy(
                artifacts = path.artifacts.filter { artifact ->
                    artifact is ControlStructureArtifact || artifact is CallArtifact
                }
            )
        }.reversed().toSet()
    }

    private fun analyze(
        conditions: MutableList<Boolean>,
        artifacts: MutableList<ArtifactElement>,
        element: ArtifactElement
    ) {
        var finished = true
        element.acceptChildren(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (!finished) return

                val artifact = element.toArtifact()
                artifact?.let { artifacts.add(it) }

                if (artifact is IfArtifact) {
                    if (artifact.thenBranch != null) {
                        val newPath = artifacts.toMutableList()
                        newPath.add(artifact.thenBranch!!)
                        val newConditions = conditions.toMutableList()
                        newConditions.add(true)
                        queue.add(PathSplit(newConditions, newPath))
                    } else {
                        //path terminated
                        val newConditions = conditions.toMutableList()
                        newConditions.add(true)
                        paths.add(RuntimePathImpl(newConditions, artifacts))
                    }

                    if (artifact.elseBranch != null) {
                        val newPath = artifacts.toMutableList()
                        newPath.add(artifact.elseBranch!!)
                        val newConditions = conditions.toMutableList()
                        newConditions.add(false)
                        queue.add(PathSplit(newConditions, newPath))
                    } else {
                        //path terminated
                        val newConditions = conditions.toMutableList()
                        newConditions.add(false)
                        paths.add(RuntimePathImpl(newConditions, artifacts))
                    }

                    finished = false
                } else {
                    super.visitElement(element)
                }
            }
        })

        if (finished) {
            paths.add(RuntimePathImpl(conditions.toList(), artifacts.toList()))
        }
    }

    private data class PathSplit(
        val conditions: List<Boolean>,
        val artifacts: List<ArtifactElement>
    )
}
