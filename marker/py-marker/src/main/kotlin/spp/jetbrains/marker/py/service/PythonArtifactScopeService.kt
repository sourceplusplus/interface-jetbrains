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
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.parentOfTypes
import com.jetbrains.python.psi.PyFunction
import spp.jetbrains.marker.service.define.IArtifactScopeService
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.source.SourceFileMarker

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonArtifactScopeService : IArtifactScopeService {

    //todo: shouldn't need to use reflection
    private val getScopeOwnerMethod = Class.forName("com.jetbrains.python.codeInsight.dataflow.scope.ScopeUtil")
        .getMethod("getScopeOwner", PsiElement::class.java)
    private val getScopeMethod = Class.forName("com.jetbrains.python.codeInsight.controlflow.ControlFlowCache")
        .getMethod("getScope", Class.forName("com.jetbrains.python.codeInsight.controlflow.ScopeOwner"))

    override fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String> {
        val position = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber)!!
        val scope = getScopeOwnerMethod.invoke(null, position)
        val els = getScopeMethod.invoke(null, scope)
        val vars = Class.forName("com.jetbrains.python.codeInsight.dataflow.scope.Scope")
            .getMethod("getNamedElements").invoke(els) as Collection<PsiNamedElement>
        return vars.mapNotNull { it.name }
    }

    override fun isInsideFunction(element: PsiElement): Boolean {
        return element.parentOfTypes(PyFunction::class) != null
    }

    override fun isInsideEndlessLoop(element: PsiElement): Boolean {
        return false
    }

    override fun isJVM(element: PsiElement): Boolean = false
}
