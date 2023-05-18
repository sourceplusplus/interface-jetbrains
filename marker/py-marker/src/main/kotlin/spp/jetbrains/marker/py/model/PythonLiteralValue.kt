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
package spp.jetbrains.marker.py.model

import com.jetbrains.python.psi.PyBoolLiteralExpression
import com.jetbrains.python.psi.PyLiteralExpression
import com.jetbrains.python.psi.PyNumericLiteralExpression
import spp.jetbrains.artifact.model.ArtifactLiteralValue

class PythonLiteralValue(override val psiElement: PyLiteralExpression) : ArtifactLiteralValue(psiElement) {
    override val value: Any?
        get() {
            return when (psiElement) {
                is PyNumericLiteralExpression -> {
                    val numericValue = psiElement.bigDecimalValue
                    if (numericValue?.scale() == 0) {
                        numericValue.toLong()
                    } else {
                        numericValue?.toDouble()
                    }
                }

                is PyBoolLiteralExpression -> psiElement.value
                else -> psiElement.text
            }
        }

    override fun clone(): PythonLiteralValue {
        return PythonLiteralValue(psiElement)
    }
}
