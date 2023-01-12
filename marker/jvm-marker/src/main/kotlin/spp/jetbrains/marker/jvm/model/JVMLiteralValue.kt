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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralValue
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind.*
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType.Companion.kindToConstantElementType
import spp.jetbrains.artifact.model.ArtifactLiteralValue
import spp.jetbrains.artifact.service.isKotlin

class JVMLiteralValue(psiElement: PsiElement) : ArtifactLiteralValue(psiElement) {

    override val value: Any?
        get() {
            return when {
                psiElement is PsiLiteralValue -> psiElement.value

                psiElement.isKotlin() && psiElement is KtConstantExpression -> {
                    //todo: feels wrong
                    when {
                        elementType == kindToConstantElementType(INTEGER_CONSTANT) && text.contains("l", true) -> {
                            text.removeSuffix("L").removeSuffix("l").toLongOrNull()
                        }

                        elementType == kindToConstantElementType(INTEGER_CONSTANT) -> text.toIntOrNull()
                        elementType == kindToConstantElementType(FLOAT_CONSTANT) -> text.toFloatOrNull()
                        elementType == kindToConstantElementType(BOOLEAN_CONSTANT) -> text.toBoolean()
                        elementType == kindToConstantElementType(CHARACTER_CONSTANT) -> text.toCharArray().firstOrNull()
                        else -> psiElement.text
                    }
                }

                else -> TODO()
            }
        }

    override fun clone(): JVMLiteralValue {
        return JVMLiteralValue(psiElement)
    }
}
