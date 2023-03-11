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
package spp.jetbrains.insight.pass.path

import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.model.CallArtifact
import spp.jetbrains.artifact.model.ControlStructureArtifact
import spp.jetbrains.insight.pass.ProceduralPathPass
import spp.jetbrains.insight.path.ProceduralPath

/**
 * Remove non-control structure, non-call artifacts from the paths.
 */
class PruneArtifactsPass : ProceduralPathPass() {

    override fun analyze(path: ProceduralPath) {
        removeArtifacts(path.artifacts)
    }

    private fun removeArtifacts(artifacts: MutableList<ArtifactElement>) {
        artifacts.removeIf { artifact ->
            if (artifact is ControlStructureArtifact) {
                removeArtifacts(artifact.childArtifacts as MutableList<ArtifactElement>)
            }
            artifact !is ControlStructureArtifact && artifact !is CallArtifact
        }
    }
}
