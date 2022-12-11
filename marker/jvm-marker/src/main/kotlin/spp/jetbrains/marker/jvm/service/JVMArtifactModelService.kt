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
package spp.jetbrains.marker.jvm.service

import com.intellij.psi.*
import org.jetbrains.kotlin.psi.*
import spp.jetbrains.marker.jvm.model.*
import spp.jetbrains.marker.model.ArtifactElement
import spp.jetbrains.marker.service.define.IArtifactModelService
import spp.jetbrains.marker.service.isKotlin

/**
 * Provides language-agnostic artifact model service for JVM languages.
 *
 * @since 0.7.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactModelService : IArtifactModelService {

    override fun toArtifact(element: PsiElement): ArtifactElement? {
        if (element.isKotlin()) {
            return toArtifact(element as KtElement)
        }

        return when (element) {
            is PsiIfStatement -> JVMIfArtifact(element)
            is PsiLiteralValue -> JVMLiteralValue(element)
            is PsiBinaryExpression -> JVMBinaryExpression(element)
            is PsiMethod -> JVMFunctionArtifact(element)
            is PsiCall -> JVMCallArtifact(element)
            else -> null
        }
    }

    private fun toArtifact(element: KtElement): ArtifactElement? {
        return when (element) {
            is KtIfExpression -> JVMIfArtifact(element)
            is KtConstantExpression -> JVMLiteralValue(element)
            is KtBinaryExpression -> JVMBinaryExpression(element)
            is KtNamedFunction -> JVMFunctionArtifact(element)
            is KtCallExpression -> JVMCallArtifact(element)
            else -> null
        }
    }
}
