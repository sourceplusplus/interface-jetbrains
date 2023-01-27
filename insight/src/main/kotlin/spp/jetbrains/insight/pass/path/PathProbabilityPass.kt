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
import spp.jetbrains.artifact.model.IfArtifact
import spp.jetbrains.insight.InsightKeys
import spp.jetbrains.insight.pass.ProceduralPathPass
import spp.jetbrains.insight.path.ProceduralPath
import spp.protocol.insight.InsightType.CONTROL_STRUCTURE_PROBABILITY
import spp.protocol.insight.InsightType.PATH_EXECUTION_PROBABILITY
import spp.protocol.insight.InsightValue

/**
 * Calculates [PATH_EXECUTION_PROBABILITY] by propagating the [CONTROL_STRUCTURE_PROBABILITY] insight.
 * The probability of each artifact is calculated by multiplying the probability of each [IfArtifact]
 * in the path. For example, if the path contains two [IfArtifact]s with probability of 0.5, the
 * following artifacts will have a base probability of 0.25.
 */
class PathProbabilityPass : ProceduralPathPass {

    override fun analyze(path: ProceduralPath) {
        path.artifacts.forEach {
            it.data[InsightKeys.PATH_EXECUTION_PROBABILITY] =
                InsightValue.of(PATH_EXECUTION_PROBABILITY, 1.0)

            if (it is IfArtifact) {
                analyze(it, it.getData(InsightKeys.CONDITION_EVALUATION)!!, 1.0)
            }
        }
    }

    private fun analyze(ifArtifact: IfArtifact, condition: Boolean, probability: Double) {
        val probability = calculateProbability(ifArtifact, probability, condition)
        ifArtifact.childArtifacts.forEach {
            it.data[InsightKeys.PATH_EXECUTION_PROBABILITY] =
                InsightValue.of(PATH_EXECUTION_PROBABILITY, probability)

            if (it is IfArtifact) {
                analyze(it, it.getData(InsightKeys.CONDITION_EVALUATION)!!, probability)
            }
        }
    }

    private fun calculateProbability(element: ArtifactElement, baseProbability: Double, condition: Boolean): Double {
        var selfProbability = Double.NaN
        if (element.getData(InsightKeys.CONTROL_STRUCTURE_PROBABILITY) != null) {
            selfProbability = element.getData(InsightKeys.CONTROL_STRUCTURE_PROBABILITY)!!.value
        }

        //see if probability can be determined statically
        if (element is IfArtifact && selfProbability.isNaN()) {
            val staticProbability = element.getStaticProbability()
            if (!staticProbability.isNaN()) {
                selfProbability = staticProbability

                //todo: move to getStaticProbability()
                //flip self probability if condition is false
                if (!condition) {
                    selfProbability = 1 - selfProbability
                }

                element.data[InsightKeys.CONTROL_STRUCTURE_PROBABILITY] =
                    InsightValue.of(CONTROL_STRUCTURE_PROBABILITY, selfProbability)
            }
        }

        return if (selfProbability.isNaN()) {
            baseProbability
        } else {
            baseProbability * selfProbability
        }
    }
}
