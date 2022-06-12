/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.marker.py

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import spp.jetbrains.marker.AbstractArtifactScopeService
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.source.SourceFileMarker

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonArtifactScopeService : AbstractArtifactScopeService {

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
}
