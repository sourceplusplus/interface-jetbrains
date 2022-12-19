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

import spp.jetbrains.insight.RuntimePathImpl
import spp.jetbrains.insight.pass.RuntimePathPass
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.marker.model.ArtifactElement
import spp.jetbrains.marker.model.IfArtifact
import spp.protocol.insight.InsightType.CONTROL_STRUCTURE_PROBABILITY
import spp.protocol.insight.InsightType.PATH_EXECUTION_PROBABILITY
import spp.protocol.insight.InsightValue

/**
 * Calculates [PATH_EXECUTION_PROBABILITY] by propagating the [CONTROL_STRUCTURE_PROBABILITY] insight.
 * The probability of each artifact is calculated by multiplying the probability of each [IfArtifact]
 * in the path. For example, if the path contains two [IfArtifact]s with probability of 0.5, the
 * following artifacts will have a base probability of 0.25.
 */
class PathProbabilityPass : RuntimePathPass {

    override fun analyze(path: RuntimePathImpl) {
        var probability = 1.0
        path.forEach {
            probability = calculateProbability(it, probability)
        }
    }

    private fun calculateProbability(element: ArtifactElement, baseProbability: Double): Double {
        var selfProbability = 1.0
        if (element.getUserData(SourceMarkerKeys.CONTROL_STRUCTURE_PROBABILITY.asPsiKey()) != null) {
            selfProbability = element.getUserData(SourceMarkerKeys.CONTROL_STRUCTURE_PROBABILITY.asPsiKey())!!.value
        }

        //see if probability can be determined statically
        if (element is IfArtifact) {
            val staticProbability = element.getStaticProbability()
            if (!staticProbability.isNaN()) {
                selfProbability = staticProbability
            }
        }

        element.putUserData(
            SourceMarkerKeys.PATH_EXECUTION_PROBABILITY.asPsiKey(),
            InsightValue.of(PATH_EXECUTION_PROBABILITY, baseProbability)
        )
        return baseProbability * selfProbability
    }
}
