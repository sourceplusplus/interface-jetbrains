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
package spp.jetbrains.marker.py.service

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.*
import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.service.define.IArtifactModelService
import spp.jetbrains.marker.py.model.*

/**
 * Provides language-agnostic artifact model service for Python.
 *
 * @since 0.7.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonArtifactModelService : IArtifactModelService {

    override fun toArtifact(element: PsiElement): ArtifactElement? {
        return when (element) {
            is PyIfStatement -> PythonIfArtifact(element)
            is PyBinaryExpression -> PythonBinaryExpression(element)
            is PyLiteralExpression -> PythonLiteralValue(element)
            is PyFunction -> PythonFunctionArtifact(element)
            is PyCallExpression -> PythonCallArtifact(element)
            is PyStatementList -> PythonBlockArtifact(element)
            is PyReferenceExpression -> {
                if (element.reference.resolve() != null) {
                    PythonReferenceArtifact(element)
                } else {
                    null
                }
            }
            else -> null
        }
    }
}
