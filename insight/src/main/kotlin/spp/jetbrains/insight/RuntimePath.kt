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

import spp.jetbrains.marker.model.ArtifactElement
import spp.jetbrains.marker.model.CallArtifact
import spp.jetbrains.marker.model.FunctionArtifact
import spp.jetbrains.marker.model.IfArtifact
import spp.jetbrains.marker.model.analysis.IRuntimePath
import spp.protocol.insight.InsightValue

data class RuntimePath(
    val evaluations: List<Boolean>,
    override val artifacts: MutableList<ArtifactElement>,
    internal val insights: MutableList<InsightValue<*>> = mutableListOf()
) : IRuntimePath {

    override fun iterator(): Iterator<ArtifactElement> = descendants.iterator()
    override fun getInsights(): List<InsightValue<*>> = insights

    override fun getResolvedCallFunctions(): List<FunctionArtifact> {
        return artifacts
            .filterIsInstance<CallArtifact>()
            .mapNotNull { it.getResolvedFunction() }
    }

    override val conditions: List<Pair<Boolean, IfArtifact>>
        get() {
            val conditionals = descendants.filterIsInstance<IfArtifact>()
            val conditions = mutableListOf<Pair<Boolean, IfArtifact>>()
            for (i in conditionals.indices) {
                conditions.add(Pair(this.evaluations[i], conditionals[i]))
            }
            return conditions
        }
}
