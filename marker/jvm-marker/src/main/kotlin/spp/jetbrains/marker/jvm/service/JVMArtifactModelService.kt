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
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.impl.GrMethodReferenceExpressionImpl
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.service.define.IArtifactModelService
import spp.jetbrains.artifact.service.isGroovy
import spp.jetbrains.artifact.service.isKotlin
import spp.jetbrains.artifact.service.isScala
import spp.jetbrains.marker.jvm.model.*

/**
 * Provides language-agnostic artifact model service for JVM languages.
 *
 * @since 0.7.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactModelService : IArtifactModelService {

    @Suppress("CyclomaticComplexMethod")
    override fun toArtifact(element: PsiElement): ArtifactElement? {
        if (element.isKotlin()) {
            val fromKotlin = fromKotlin(element)
            if (fromKotlin != null) return fromKotlin
        }
        if (element.isScala()) {
            val fromScala = fromScala(element)
            if (fromScala != null) return fromScala
        }
        if (element.isGroovy()) {
            val fromGroovy = fromGroovy(element)
            if (fromGroovy != null) return fromGroovy
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

            is PsiReferenceExpression -> {
                if (element.resolve() != null) {
                    JVMReferenceArtifact(element)
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
            is KtStringTemplateExpression -> JVMLiteralValue(element)
            is KtConstantExpression -> JVMLiteralValue(element)
            is KtBinaryExpression -> JVMBinaryExpression(element)
            is KtNamedFunction -> JVMFunctionArtifact(element)
            is KtCallExpression -> JVMCallArtifact(element)
            is KtBlockExpression -> JVMBlockArtifact(element)
            is KtCallableReferenceExpression -> JVMCallArtifact(element)
            is KtDotQualifiedExpression -> {
                if (element.selectorExpression is KtCallExpression) {
                    JVMCallArtifact(element)
                } else {
                    null
                }
            }

            is KtNameReferenceExpression -> {
                //todo: easier way to determine if this is a variable or literal reference (Boolean, String, etc)
                if (element.mainReference.resolve() != null) {
                    JVMReferenceArtifact(element)
                } else {
                    null
                }
            }

            else -> null
        }
    }

    private fun fromScala(element: PsiElement): ArtifactElement? {
        return when (element) {
            is ScMethodCall -> JVMCallArtifact(element)
            is ScReferenceExpression -> {
                if (element.resolve() != null) {
                    JVMReferenceArtifact(element)
                } else {
                    null
                }
            }

            else -> null
        }
    }

    private fun fromGroovy(element: PsiElement): ArtifactElement? {
        return when (element) {
            is GrMethodCall -> JVMCallArtifact(element)
            is GrMethodReferenceExpressionImpl -> {
                if (element.resolve() != null) {
                    JVMReferenceArtifact(element)
                } else {
                    null
                }
            }

            else -> null
        }
    }
}
