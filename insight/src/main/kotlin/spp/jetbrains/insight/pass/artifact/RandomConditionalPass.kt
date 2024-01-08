/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.insight.pass.artifact

import spp.jetbrains.artifact.model.ArtifactBinaryExpression
import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.model.ArtifactLiteralValue
import spp.jetbrains.artifact.model.IfArtifact
import spp.jetbrains.insight.InsightKeys
import spp.jetbrains.insight.pass.ArtifactPass
import spp.protocol.insight.InsightType
import spp.protocol.insight.InsightValue

class RandomConditionalPass : ArtifactPass() {

    override fun analyze(element: ArtifactElement) {
        if (element !is IfArtifact) return //only interested in ifs

        if (element.condition is ArtifactBinaryExpression) {
            val condition = element.condition as ArtifactBinaryExpression

            //todo: verify better
            var randomCheck = condition.getLeftExpression()?.text?.endsWith("Math.random()") == true
            if (!randomCheck) {
                randomCheck = condition.getLeftExpression()?.text?.endsWith("random.random()") == true
            }

            if (randomCheck) {
                if (condition.getRightExpression()?.isLiteral() == true) {
                    val literalValue = condition.getRightExpression() as ArtifactLiteralValue
                    calculateProbability(element, condition.getOperator(), literalValue)
                }
            }
        }
    }

    private fun calculateProbability(ifArtifact: IfArtifact, operator: String, literal: ArtifactLiteralValue) {
        val literalValue = literal.value?.toString()?.toDoubleOrNull() ?: return
        val probability = when (operator) {
            "<" -> literalValue
            ">" -> 1 - literalValue
            "<=" -> literalValue + 0.01
            ">=" -> 1 - literalValue + 0.01

            //todo: doubt these are correct, but likely sufficient anyway
            "==" -> Double.MIN_VALUE
            "!=" -> Double.MIN_VALUE

            else -> 0.0
        }.let { if (it > 1.0) 1.0 else if (it < 0.0) 0.0 else it }

        val conditionEvaluation = ifArtifact.getConditionEvaluation()!!
        if (conditionEvaluation) {
            ifArtifact.data[InsightKeys.CONTROL_STRUCTURE_PROBABILITY] =
                InsightValue(InsightType.CONTROL_STRUCTURE_PROBABILITY, probability)
        } else {
            ifArtifact.data[InsightKeys.CONTROL_STRUCTURE_PROBABILITY] =
                InsightValue(InsightType.CONTROL_STRUCTURE_PROBABILITY, 1 - probability)
        }
    }
}
