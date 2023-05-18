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
package spp.jetbrains.marker.js.service

import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecma6.TypeScriptVariable
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralValue
import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.service.define.IArtifactModelService
import spp.jetbrains.marker.js.model.*

/**
 * Provides language-agnostic artifact model service for JavaScript-based languages.
 *
 * @since 0.7.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JavascriptArtifactModelService : IArtifactModelService {

    override fun toArtifact(element: PsiElement): ArtifactElement? {
        return when (element) {
            is JSIfStatement -> JavascriptIfArtifact(element)
            is JSBinaryExpression -> JavascriptBinaryExpression(element)
            is PsiLiteralValue -> JavascriptLiteralValue(element)
            is JSFunction -> JavascriptFunctionArtifact(element)
            is JSCallExpression -> JavascriptCallArtifact(element)
            is JSBlockStatement -> JavascriptBlockArtifact(element)
            is JSReferenceExpression -> {
                val resolve = element.resolve()
                if (resolve != null) {
                    val projectFileIndex = ProjectFileIndex.getInstance(element.project)
                    if (projectFileIndex.isInSource(resolve.containingFile.virtualFile)) {
                        JavascriptReferenceArtifact(element)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
            else -> null
        }
    }
}
