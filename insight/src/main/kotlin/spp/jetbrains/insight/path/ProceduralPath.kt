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
package spp.jetbrains.insight.path

import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.model.CallArtifact
import spp.jetbrains.artifact.model.FunctionArtifact
import spp.jetbrains.artifact.model.IfArtifact
import spp.jetbrains.insight.InsightKeys
import spp.protocol.insight.InsightValue

/**
 * Represents an inter/intra-procedural path through the given [rootArtifact].
 */
data class ProceduralPath(
    val rootArtifact: ArtifactElement,
    val artifacts: MutableList<ArtifactElement>,
    internal val insights: MutableList<InsightValue<*>> = mutableListOf()
) : Iterable<ArtifactElement> {

    override fun iterator(): Iterator<ArtifactElement> = descendants.iterator()
    fun getInsights(): List<InsightValue<*>> = insights

    fun getResolvedCallFunctions(): List<FunctionArtifact> {
        return artifacts
            .filterIsInstance<CallArtifact>()
            .mapNotNull { it.getResolvedFunction() }
    }

    val conditions: List<Pair<Boolean, IfArtifact>>
        get() {
            val conditionals = descendants.filterIsInstance<IfArtifact>()
            val conditions = mutableListOf<Pair<Boolean, IfArtifact>>()
            for (i in conditionals.indices) {
                conditions.add(Pair(conditionals[i].getData(InsightKeys.CONDITION_EVALUATION)!!, conditionals[i]))
            }
            return conditions
        }

    val descendants: MutableList<ArtifactElement>
        get() {
            val descendants = mutableListOf<ArtifactElement>()
            for (artifact in artifacts) {
                descendants.add(artifact)
                descendants.addAll(getDescendants(artifact))
            }
            return descendants
        }

    private fun getDescendants(element: ArtifactElement): List<ArtifactElement> {
        val descendants = mutableListOf<ArtifactElement>()
        if (element is IfArtifact) {
            val children = element.childArtifacts
            for (child in children) {
                descendants.add(child)
                descendants.addAll(getDescendants(child))
            }
        } else {
            for (child in element.descendantArtifacts) {
                descendants.add(child)
                descendants.addAll(getDescendants(child))
            }
        }
        return descendants
    }
}
