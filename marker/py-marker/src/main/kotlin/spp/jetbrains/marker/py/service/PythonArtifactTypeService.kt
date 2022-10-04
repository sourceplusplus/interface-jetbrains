/*
 * Source++, the open-source live coding platform.
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
package spp.jetbrains.marker.py.service

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyFunction
import spp.jetbrains.marker.IArtifactTypeService
import spp.protocol.artifact.ArtifactType

class PythonArtifactTypeService : IArtifactTypeService {

    override fun getType(element: PsiElement): ArtifactType? {
        return when (element) {
            is PyClass -> ArtifactType.CLASS
            is PyFunction -> ArtifactType.METHOD
            is PyExpression -> ArtifactType.EXPRESSION

            else -> null
        }
    }
}