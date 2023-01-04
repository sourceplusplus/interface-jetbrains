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
package spp.jetbrains.marker.jvm.model

import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBinaryExpression
import spp.jetbrains.artifact.model.ArtifactBinaryExpression
import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.service.isKotlin
import spp.jetbrains.artifact.service.toArtifact

class JVMBinaryExpression(private val psiElement: PsiElement) : ArtifactBinaryExpression(psiElement) {

    override fun getLeftExpression(): ArtifactElement? {
        return when {
            psiElement is PsiBinaryExpression -> psiElement.lOperand.toArtifact()
            psiElement.isKotlin() && psiElement is KtBinaryExpression -> psiElement.left?.toArtifact()
            else -> throw IllegalArgumentException("Unsupported binary expression type: ${psiElement.javaClass}")
        }
    }

    override fun getRightExpression(): ArtifactElement? {
        return when {
            psiElement is PsiBinaryExpression -> psiElement.rOperand.toArtifact()
            psiElement.isKotlin() && psiElement is KtBinaryExpression -> psiElement.right?.toArtifact()
            else -> throw IllegalArgumentException("Unsupported binary expression type: ${psiElement.javaClass}")
        }
    }

    override fun clone(): JVMBinaryExpression {
        return JVMBinaryExpression(psiElement)
    }
}
