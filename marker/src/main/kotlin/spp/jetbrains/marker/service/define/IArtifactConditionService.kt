/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.marker.service.define

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import spp.jetbrains.artifact.service.define.ISourceMarkerService

/**
 * Used to parse/format instrument conditions.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface IArtifactConditionService : ISourceMarkerService {

    data class ParseRange(
        val textRange: TextRange,
        val variable: PsiElement
    )

    fun getCondition(condition: String, context: PsiElement): String
}
