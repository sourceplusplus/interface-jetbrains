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
package spp.jetbrains.insight.pass.pathset

import spp.jetbrains.insight.RuntimePath
import spp.jetbrains.insight.pass.RuntimePathSetPass
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.marker.model.ControlStructureArtifact
import spp.jetbrains.marker.model.IfArtifact
import java.util.*

/**
 * Removes paths caused by conditional branches that are never taken.
 */
class SimplifyPathSetPass : RuntimePathSetPass {

    override fun postProcess(pathSet: Set<RuntimePath>): Set<RuntimePath> {
        val simplifiedPaths = mutableSetOf<RuntimePath>()
        for (path in pathSet) {
            path.artifacts.removeIf {
                if (it is IfArtifact) {
                    val probability = it.getData(SourceMarkerKeys.PATH_EXECUTION_PROBABILITY)
                    probability?.value == 0.0 || it.childArtifacts.isEmpty()
                } else {
                    false
                }
            }

            if (path.artifacts.isNotEmpty()) {
                simplifiedPaths.add(path)
            }
        }

        for (path in pathSet.toMutableSet()) {
            //sublist check
            val pathArtifacts = path.artifacts
            val dupePath = simplifiedPaths.any {
                it !== path && pathArtifacts.none { it is ControlStructureArtifact }
                        && Collections.indexOfSubList(it.artifacts, pathArtifacts) != -1
            }

            if (dupePath) {
                simplifiedPaths.remove(path)
            }
        }

        return simplifiedPaths
    }
}
