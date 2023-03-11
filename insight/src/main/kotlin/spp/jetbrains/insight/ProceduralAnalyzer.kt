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

import spp.jetbrains.artifact.model.*
import spp.jetbrains.artifact.service.getParentFunction
import spp.jetbrains.artifact.service.toArtifact
import spp.jetbrains.insight.path.ProceduralMultiPath
import spp.jetbrains.insight.path.ProceduralPath
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream

/**
 * Provides intra-procedural analysis of [ArtifactElement] trees.
 */
class ProceduralAnalyzer {

    var passProvider: InsightPassProvider = InsightPassProvider.FULL
    val passConfig: InsightPassConfig = InsightPassConfig()

    /**
     * Performs an intraprocedural analysis of the given [ArtifactElement].
     *
     * @param element the [ArtifactElement] to analyze
     */
    fun analyze(element: ArtifactElement): ProceduralMultiPath {
        val parent = mutableListOf<Any>()
        walkDown(element, parent)

        val ifCount = getIfArtifactCount(parent, 0)
        val boolPermutations = makeBoolPermutations(ifCount)
        val paths = mutableListOf<ProceduralPath>()
        for (boolArray in boolPermutations) {
            val path = ProceduralPath(element, mutableListOf())
            processPath(path, path.artifacts, parent, boolArray.iterator())
            paths.add(path)
        }

        return passProvider.analyze(this, ProceduralMultiPath(paths))
    }

    /**
     * Performs an intraprocedural analysis of the given [ArtifactElement] and returns the [ProceduralPath]s that
     * contain the given [ArtifactElement].
     *
     * @param element the [ArtifactElement] to analyze
     */
    fun analyzeUp(element: ArtifactElement): List<ProceduralPath> {
        //todo: do this more efficiently
        val paths = analyze(element.getParentFunction().toArtifact()!!)
        return paths.filter {
            it.descendants.contains(element)
        }
    }

    private fun walkDown(element: ArtifactElement, parent: MutableList<Any>) {
        if (element is FunctionArtifact || element is BlockArtifact || element is IfArtifact) {
            val descendants = element.descendantArtifacts
            for (descendant in descendants) {
                if (descendant !is FunctionArtifact && descendant !is BlockArtifact) {
                    parent.add(descendant)
                }

                if (descendant is IfArtifact) {
                    val thenBranch = descendant.thenBranch
                    val thenArtifacts = mutableListOf<Any>()
                    if (thenBranch != null) {
                        walkDown(thenBranch, thenArtifacts)
                    }
                    parent.add(thenArtifacts)

                    val elseBranch = descendant.elseBranch
                    val elseArtifacts = mutableListOf<Any>()
                    if (elseBranch != null) {
                        walkDown(elseBranch, elseArtifacts)
                    }
                    parent.add(elseArtifacts)
                } else if (descendant is LoopArtifact) {
                    val loopBody = descendant.body
                    val loopBodyArtifacts = mutableListOf<Any>()
                    if (loopBody != null) {
                        walkDown(loopBody, loopBodyArtifacts)
                    }
                    parent.add(loopBodyArtifacts)
                } else {
                    walkDown(descendant, parent)
                }
            }
        }
    }

    private fun processPath(
        path: ProceduralPath,
        nextArtifacts: MutableList<ArtifactElement>,
        processArtifacts: List<Any>,
        boolIterator: BooleanIterator
    ) {
        processArtifacts.forEachIndexed { index, it ->
            var artifactElement: ArtifactElement? = null
            if (it is ArtifactElement) {
                artifactElement = it.clone()
                nextArtifacts.add(artifactElement)
            }

            if (artifactElement is IfArtifact) {
                val bool = boolIterator.next()
                artifactElement.setConditionEvaluation(bool)

                if (bool) {
                    val childArtifacts = processArtifacts[index + 1] as List<Any>
                    processPath(path, artifactElement.childArtifacts, childArtifacts, boolIterator)
                } else {
                    val childArtifacts = processArtifacts[index + 2] as List<Any>
                    processPath(path, artifactElement.childArtifacts, childArtifacts, boolIterator)
                }
            } else if (artifactElement is LoopArtifact) {
                val childArtifacts = processArtifacts[index + 1] as List<Any>
                processPath(path, artifactElement.childArtifacts, childArtifacts, boolIterator)
            }
        }
    }

    private fun makeBoolPermutations(n: Int): List<BooleanArray> {
        return IntStream.range(0, 1 shl n)
            .mapToObj { BitSet.valueOf(longArrayOf(it.toLong())) }
            .map {
                val a = BooleanArray(n)
                for (i in 0 until n) {
                    a[n - i - 1] = it.get(i)
                }
                a
            }.collect(Collectors.toList())
    }

    private fun getIfArtifactCount(element: Any, count: Int): Int {
        var count = count
        if (element is List<*>) {
            for (e in element) {
                count = getIfArtifactCount(e!!, count)
            }
        } else if (element is IfArtifact) {
            count++
        }
        return count
    }
}
