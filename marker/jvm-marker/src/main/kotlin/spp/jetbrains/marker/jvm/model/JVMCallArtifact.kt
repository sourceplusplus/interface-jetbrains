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

import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import scala.collection.JavaConverters
import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.model.CallArtifact
import spp.jetbrains.artifact.model.FunctionArtifact
import spp.jetbrains.artifact.service.toArtifact

class JVMCallArtifact(override val psiElement: PsiElement) : CallArtifact(psiElement) {

    override fun getName(): String? {
        return when (psiElement) {
            is PsiCall -> psiElement.firstChild.text
            is KtCallExpression -> psiElement.calleeExpression?.text
            is KtCallableReferenceExpression -> psiElement.name

            is KtDotQualifiedExpression -> {
                if (psiElement.selectorExpression is KtCallExpression) {
                    (psiElement.selectorExpression as KtCallExpression).calleeExpression?.text
                } else {
                    null
                }
            }

            is GrMethodCallExpression -> {
                psiElement.callReference?.methodName
            }

            is ScMethodCall -> {
                psiElement.deepestInvokedExpr().lastChild.text
            }

            else -> null
        }
    }

    override fun resolveFunction(): FunctionArtifact? {
        val function = when (psiElement) {
            is PsiCall -> psiElement.resolveMethod()?.toArtifact() as? FunctionArtifact
            is KtCallExpression -> {
                (psiElement.calleeExpression as KtNameReferenceExpression).resolve()
                    ?.toArtifact() as? FunctionArtifact
            }

            is KtCallableReferenceExpression -> {
                (psiElement.callableReference.resolve() as? PsiElement)?.toArtifact() as? FunctionArtifact
            }

            is KtDotQualifiedExpression -> {
                if (psiElement.selectorExpression is KtCallExpression) {
                    val test = (psiElement.selectorExpression as KtCallExpression).calleeExpression
                    if (test is KtNameReferenceExpression) {
                        test.resolve()?.toArtifact() as? FunctionArtifact
                    } else {
                        null
                    }
                } else {
                    null
                }
            }

            is GrMethodCallExpression -> {
                psiElement.resolveMethod()?.toArtifact() as? FunctionArtifact
            }

            is ScMethodCall -> {
                (psiElement.deepestInvokedExpr() as? ScReferenceExpression)?.resolve()
                    ?.toArtifact() as? FunctionArtifact
            }

            else -> null
        }

        //propagate call arguments to function parameters
        if (function != null) {
            getArguments().forEach {
                function.parameters.add(it)
            }
        }

        return function
    }

    override fun getArguments(): List<ArtifactElement> {
        return when (psiElement) {
            is PsiCall -> psiElement.argumentList?.expressions?.mapNotNull { it.toArtifact() } ?: emptyList()
            is KtCallExpression -> {
                try {
                    psiElement.valueArguments.map { it.getArgumentExpression()?.toArtifact()!! }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                psiElement.valueArguments.mapNotNull { it.getArgumentExpression()?.toArtifact() }
            }

            is KtDotQualifiedExpression -> {
                if (psiElement.selectorExpression is KtCallExpression) {
                    (psiElement.selectorExpression as KtCallExpression).valueArguments.mapNotNull {
                        it.getArgumentExpression()?.toArtifact()
                    }
                } else {
                    emptyList()
                }
            }

            is GrMethodCallExpression -> {
                psiElement.argumentList.expressionArguments.mapNotNull { it.toArtifact() }
            }

            is ScMethodCall -> {
                JavaConverters.asJavaCollection(psiElement.argumentExpressions()).mapNotNull { it.toArtifact() }
            }

            else -> emptyList()
        }
    }

    override fun isSameArtifact(element: ArtifactElement): Boolean {
        if (element is JVMCallArtifact) {
            if (psiElement is KtCallExpression && element.psiElement is KtDotQualifiedExpression) {
                return psiElement == (element.psiElement as KtDotQualifiedExpression).selectorExpression
            } else if (psiElement is KtDotQualifiedExpression && element.psiElement is KtCallExpression) {
                return psiElement.selectorExpression == element.psiElement
            }
        }
        return super.isSameArtifact(element)
    }

    override fun clone(): JVMCallArtifact {
        return JVMCallArtifact(psiElement)
    }
}
