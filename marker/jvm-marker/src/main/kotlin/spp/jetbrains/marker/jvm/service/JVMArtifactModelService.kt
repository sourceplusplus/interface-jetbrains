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
package spp.jetbrains.marker.jvm.service

import com.intellij.psi.*
import com.siyeh.ig.psiutils.CountingLoop
import org.jetbrains.kotlin.psi.*
import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.service.define.IArtifactModelService
import spp.jetbrains.artifact.service.isKotlin
import spp.jetbrains.marker.jvm.model.*

/**
 * Provides language-agnostic artifact model service for JVM languages.
 *
 * @since 0.7.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactModelService : IArtifactModelService {

    override fun toArtifact(element: PsiElement): ArtifactElement? {
        if (element.isKotlin()) {
            return fromKotlin(element)
        }

        return when (element) {
            is PsiIfStatement -> JVMIfArtifact(element)
            is PsiLiteralValue -> JVMLiteralValue(element)
            is PsiBinaryExpression -> JVMBinaryExpression(element)
            is PsiMethod -> JVMFunctionArtifact(element)
            is PsiCall -> JVMCallArtifact(element)
            is PsiCodeBlock -> JVMBlockArtifact(element)
            is PsiBlockStatement -> JVMBlockArtifact(element.codeBlock)
            is PsiForStatement -> {
                val countingLoop = CountingLoop.from(element)
                if (countingLoop != null) {
                    JVMCountingLoop(countingLoop)
                } else {
                    null
                }
            }

            else -> null
        }
    }

    private fun fromKotlin(element: PsiElement): ArtifactElement? {
        return when (element) {
            is KtIfExpression -> JVMIfArtifact(element)
            is KtConstantExpression -> JVMLiteralValue(element)
            is KtBinaryExpression -> JVMBinaryExpression(element)
            is KtNamedFunction -> JVMFunctionArtifact(element)
            is KtCallExpression -> JVMCallArtifact(element)
            is KtBlockExpression -> JVMBlockArtifact(element)
            is KtDotQualifiedExpression -> {
                if (element.selectorExpression is KtCallExpression) {
                    JVMCallArtifact(element)
                } else {
                    null
                }
            }

            else -> null
        }
    }
}
