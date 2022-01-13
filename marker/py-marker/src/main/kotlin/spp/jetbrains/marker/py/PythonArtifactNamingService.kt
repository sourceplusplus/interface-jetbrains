package spp.jetbrains.marker.py

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import spp.jetbrains.marker.ArtifactNamingService
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonArtifactNamingService : ArtifactNamingService {

    override fun getFullyQualifiedName(element: PsiElement): ArtifactQualifiedName {
        return ArtifactQualifiedName(
            element.containingFile.name, null, ArtifactType.CLASS
        ) //todo: include method name when possible?
    }

    //todo: method name could be better
    override fun getClassQualifiedNames(psiFile: PsiFile): List<ArtifactQualifiedName> {
        return listOf(ArtifactQualifiedName(psiFile.virtualFile.path, type = ArtifactType.CLASS))
    }
}
