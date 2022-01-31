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
package spp.jetbrains.marker.jvm

import com.intellij.psi.*
import com.intellij.psi.scope.processor.VariablesProcessor
import com.intellij.psi.scope.util.PsiScopesUtil
import spp.jetbrains.marker.ArtifactScopeService
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.source.SourceFileMarker

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactScopeService : ArtifactScopeService {

    fun isMethodAtLine(psiFile: PsiFile, lineNumber: Int): Boolean {
        var el = SourceMarkerUtils.getElementAtLine(psiFile, lineNumber)
        while (el is PsiKeyword || el is PsiModifierList) {
            el = el.parent
        }
        return el is PsiMethod
    }

    override fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String> {
        //determine available vars
        var checkLine = lineNumber
        val scopeVars = mutableListOf<String>()
        var minScope: PsiElement? = null
        while (minScope == null) {
            minScope = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, --checkLine)
        }
        val variablesProcessor: VariablesProcessor = object : VariablesProcessor(false) {
            override fun check(`var`: PsiVariable, state: ResolveState): Boolean = true
        }
        PsiScopesUtil.treeWalkUp(variablesProcessor, minScope, null)
        for (i in 0 until variablesProcessor.size()) {
            scopeVars.add(variablesProcessor.getResult(i).name!!)
        }
        return scopeVars
    }
}
