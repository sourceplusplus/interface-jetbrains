package spp.jetbrains.marker.py

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import spp.jetbrains.marker.ArtifactScopeService
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.source.SourceFileMarker

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonArtifactScopeService : ArtifactScopeService {

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
