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
package spp.jetbrains.marker.py.service

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfTypes
import com.jetbrains.python.codeInsight.controlflow.ControlFlowCache
import com.jetbrains.python.codeInsight.dataflow.scope.ScopeUtil
import com.jetbrains.python.psi.PyFunction
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.service.define.IArtifactScopeService
import spp.jetbrains.marker.source.SourceFileMarker

/**
 * Used to determine the scope of Python artifacts.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonArtifactScopeService : IArtifactScopeService {

    override fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String> {
        val position = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber) ?: return emptyList()
        val scope = ScopeUtil.getScopeOwner(position) ?: return emptyList()
        return ControlFlowCache.getScope(scope).namedElements.mapNotNull { it.name }
    }

    override fun isInsideFunction(element: PsiElement): Boolean {
        return element.parentOfTypes(PyFunction::class) != null
    }

    override fun isInsideEndlessLoop(element: PsiElement): Boolean {
        return false
    }
}
