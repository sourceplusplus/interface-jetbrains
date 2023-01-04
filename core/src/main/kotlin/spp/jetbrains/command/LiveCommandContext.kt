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
package spp.jetbrains.command

import com.intellij.psi.PsiElement
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.protocol.artifact.ArtifactQualifiedName
import java.io.File

data class LiveCommandContext(
    val args: List<String>,
    val sourceFile: File,
    val lineNumber: Int,
    val artifactQualifiedName: ArtifactQualifiedName,
    val fileMarker: SourceFileMarker,
    val guideMark: GuideMark? = null,
    val psiElement: PsiElement,
    val variableName: String? = null
)
