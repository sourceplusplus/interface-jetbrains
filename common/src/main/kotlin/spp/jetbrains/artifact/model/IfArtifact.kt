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
package spp.jetbrains.artifact.model

import com.intellij.psi.PsiElement

abstract class IfArtifact(psiElement: PsiElement) : ArtifactElement(psiElement), ControlStructureArtifact {

    abstract override val condition: ArtifactElement?
    abstract val thenBranch: ArtifactElement?
    abstract val elseBranch: ArtifactElement?

    override val childArtifacts: MutableList<ArtifactElement> = mutableListOf()

    fun getStaticProbability(): Double {
        if (condition == null) return Double.NaN
        return when (val artifact = condition) {
            is ArtifactLiteralValue -> constant(artifact)
            is ArtifactBinaryExpression -> binary(artifact)
            else -> Double.NaN
        }
    }

    private fun binary(element: ArtifactBinaryExpression): Double {
        if (element.getLeftExpression().isLiteral() && element.getRightExpression().isLiteral()) {
            val left = element.getLeftExpression() as ArtifactLiteralValue?
            val right = element.getRightExpression() as ArtifactLiteralValue?
            if (left?.value == right?.value) {
                return 1.0
            }
        }

        return Double.NaN
    }

    private fun constant(element: ArtifactLiteralValue): Double {
        return when (element.value?.toString()?.lowercase()) {
            "true" -> 1.0
            "false" -> 0.0
            else -> Double.NaN
        }
    }

    abstract override fun clone(): IfArtifact
}
