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
package spp.jetbrains.insight.pass.multipath

import com.intellij.codeInspection.dataFlow.interpreter.RunnerResult
import com.intellij.codeInspection.dataFlow.interpreter.StandardDataFlowInterpreter
import com.intellij.codeInspection.dataFlow.java.anchor.JavaExpressionAnchor
import com.intellij.codeInspection.dataFlow.lang.DfaAnchor
import com.intellij.codeInspection.dataFlow.lang.DfaListener
import com.intellij.codeInspection.dataFlow.lang.ir.DataFlowIRProvider
import com.intellij.codeInspection.dataFlow.lang.ir.DfaInstructionState
import com.intellij.codeInspection.dataFlow.memory.DfaMemoryState
import com.intellij.codeInspection.dataFlow.memory.DfaMemoryStateImpl
import com.intellij.codeInspection.dataFlow.types.DfTypes
import com.intellij.codeInspection.dataFlow.value.DfaValue
import com.intellij.codeInspection.dataFlow.value.DfaValueFactory
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.kotlin.idea.inspections.dfa.KotlinAnchor
import spp.jetbrains.artifact.model.FunctionArtifact
import spp.jetbrains.artifact.model.IfArtifact
import spp.jetbrains.insight.InsightKeys
import spp.jetbrains.insight.pass.ProceduralMultiPathPass
import spp.jetbrains.insight.path.ProceduralMultiPath
import spp.protocol.insight.InsightType
import spp.protocol.insight.InsightValue

class StaticDfaMultiPathPass : ProceduralMultiPathPass() {

    private val log = logger<StaticDfaMultiPathPass>()

    override fun preProcess(multiPath: ProceduralMultiPath): ProceduralMultiPath {
        val project = multiPath.first().rootArtifact.project
        val factory = DfaValueFactory(project)
        val rootArtifact = multiPath.first().rootArtifact as? FunctionArtifact ?: return multiPath
        val bodyBlock = rootArtifact.bodyBlock?.psiElement ?: return multiPath
        val flow = DataFlowIRProvider.forElement(bodyBlock, factory) ?: return multiPath

        val listener = ConstantConditionDfaListener()
        val interpreter = StandardDataFlowInterpreter(flow, listener)
        val states = listOf(DfaMemoryStateImpl(factory))
        if (interpreter.interpret(states.map { s ->
                DfaInstructionState(flow.getInstruction(0), s)
            }) != RunnerResult.OK) {
            log.warn("Failed to interpret function ${rootArtifact.name}")
        }

        listener.constantConditions.forEach {
            if (it.key is KotlinAnchor.KotlinExpressionAnchor && it.value != ConstantValue.UNKNOWN) {
                val anchor = it.key as KotlinAnchor.KotlinExpressionAnchor
                val value = it.value
                val expression = anchor.expression
                multiPath.forEach {
                    it.artifacts.forEach {
                        if (it is IfArtifact) {
                            if (it.condition?.psiElement == expression) {
                                val probability = if (value == ConstantValue.TRUE) 1.0 else 0.0
                                val conditionEvaluation = it.getConditionEvaluation()!!
                                if (conditionEvaluation) {
                                    it.data[InsightKeys.CONTROL_STRUCTURE_PROBABILITY] =
                                        InsightValue(InsightType.CONTROL_STRUCTURE_PROBABILITY, probability)
                                } else {
                                    it.data[InsightKeys.CONTROL_STRUCTURE_PROBABILITY] =
                                        InsightValue(InsightType.CONTROL_STRUCTURE_PROBABILITY, 1 - probability)
                                }
                            }
                        }
                    }
                }
            } else if (it.key is JavaExpressionAnchor && it.value != ConstantValue.UNKNOWN) {
                val anchor = it.key as JavaExpressionAnchor
                val value = it.value
                val expression = anchor.expression
                multiPath.forEach {
                    it.artifacts.forEach {
                        if (it is IfArtifact) {
                            if (it.condition?.psiElement == expression) {
                                val probability = if (value == ConstantValue.TRUE) 1.0 else 0.0
                                val conditionEvaluation = it.getConditionEvaluation()!!
                                if (conditionEvaluation) {
                                    it.data[InsightKeys.CONTROL_STRUCTURE_PROBABILITY] =
                                        InsightValue(InsightType.CONTROL_STRUCTURE_PROBABILITY, probability)
                                } else {
                                    it.data[InsightKeys.CONTROL_STRUCTURE_PROBABILITY] =
                                        InsightValue(InsightType.CONTROL_STRUCTURE_PROBABILITY, 1 - probability)
                                }
                            }
                        }
                    }
                }
            }
        }

        return multiPath
    }

    enum class ConstantValue {
        TRUE, FALSE, NULL, ZERO, UNKNOWN
    }

    class ConstantConditionDfaListener : DfaListener {

        internal val constantConditions = hashMapOf<DfaAnchor, ConstantValue>()

        override fun beforePush(args: Array<out DfaValue>, value: DfaValue, anchor: DfaAnchor, state: DfaMemoryState) {
            recordExpressionValue(anchor, state, value)
        }

        private fun recordExpressionValue(anchor: DfaAnchor, state: DfaMemoryState, value: DfaValue) {
            val oldVal = constantConditions[anchor]
            if (oldVal == ConstantValue.UNKNOWN) return
            var newVal = when (val dfType = state.getDfType(value)) {
                DfTypes.TRUE -> ConstantValue.TRUE
                DfTypes.FALSE -> ConstantValue.FALSE
                DfTypes.NULL -> ConstantValue.NULL
                else -> {
                    val constVal: Number? = dfType.getConstantOfType(Number::class.java)
                    if (constVal != null && (constVal == 0 || constVal == 0L)) ConstantValue.ZERO
                    else ConstantValue.UNKNOWN
                }
            }
            if (oldVal != null && oldVal != newVal) {
                newVal = ConstantValue.UNKNOWN
            }
            constantConditions[anchor] = newVal
        }
    }
}
