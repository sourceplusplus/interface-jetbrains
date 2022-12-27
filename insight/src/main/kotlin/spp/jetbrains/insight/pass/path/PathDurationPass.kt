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
package spp.jetbrains.insight.pass.path

import spp.jetbrains.insight.InsightKeys
import spp.jetbrains.insight.ProceduralPath
import spp.jetbrains.insight.getDuration
import spp.jetbrains.insight.pass.ProceduralPathPass
import spp.jetbrains.marker.model.ArtifactElement
import spp.jetbrains.marker.model.IfArtifact
import spp.protocol.insight.InsightType.PATH_DURATION
import spp.protocol.insight.InsightValue

/**
 * Calculates [PATH_DURATION] based on the sum of the durations of the artifacts in the runtime path.
 */
class PathDurationPass : ProceduralPathPass {

    override fun analyze(path: ProceduralPath) {
        val duration = analyze(path.artifacts, null)
        if (duration != null) {
            path.insights.add(InsightValue.of(PATH_DURATION, duration).asDerived())
        }
    }

    private fun analyze(elements: List<ArtifactElement>, duration: Long?): Long? {
        var duration = duration
        elements.forEach {
            if (it is IfArtifact) {
                //condition always executes so add to duration regardless of condition result
                val conditionDuration = it.getDuration()
                duration = duration?.plus(conditionDuration ?: 0) ?: conditionDuration
                val conditionDescendantDuration = it.condition?.descendantArtifacts
                    ?.mapNotNull { it.getDuration() }?.takeIf { it.isNotEmpty() }?.sum()
                duration = duration?.plus(conditionDescendantDuration ?: 0) ?: conditionDescendantDuration

                //if condition passes, add duration of child artifacts
                val executionProbability = it.getData(InsightKeys.PATH_EXECUTION_PROBABILITY)
                if (executionProbability == null || executionProbability.value > 0.0) {
                    analyze(it.childArtifacts, duration)?.let { duration = it }
                }
            } else {
                val artifactDuration = it.getDuration()
                if (artifactDuration != null) {
                    duration = (duration ?: 0) + artifactDuration
                }
            }
        }
        return duration
    }
}
