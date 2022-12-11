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
package spp.jetbrains.marker.jvm.model

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralValue
import org.jetbrains.kotlin.psi.KtConstantExpression
import spp.jetbrains.marker.model.ArtifactLiteralValue
import spp.jetbrains.marker.service.isKotlin

class JVMLiteralValue(private val psiElement: PsiElement) : ArtifactLiteralValue(psiElement) {
    override val value: Any?
        get() {
            return when {
                psiElement is PsiLiteralValue -> psiElement.value

                psiElement.isKotlin() && psiElement is KtConstantExpression -> {
                    psiElement.text //todo:
                }

                else -> TODO()
            }
        }
}
