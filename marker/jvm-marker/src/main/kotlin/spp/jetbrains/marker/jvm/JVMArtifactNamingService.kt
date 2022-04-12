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

import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import spp.jetbrains.marker.ArtifactNamingService
import spp.jetbrains.marker.source.JVMMarkerUtils
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactNamingService : ArtifactNamingService {

    override fun getFullyQualifiedName(element: PsiElement): ArtifactQualifiedName {
        return when (val uElement = element.toUElement()) {
            is UClass -> JVMMarkerUtils.getFullyQualifiedName(uElement)
            is UMethod -> JVMMarkerUtils.getFullyQualifiedName(uElement)
            is UExpression -> JVMMarkerUtils.getFullyQualifiedName(element)
            else -> TODO("Not yet implemented")
        }
    }

    override fun getQualifiedClassNames(psiFile: PsiFile): List<ArtifactQualifiedName> {
        return when (psiFile) {
            is PsiClassOwner -> psiFile.classes.map {
                ArtifactQualifiedName(it.qualifiedName!!, type = ArtifactType.CLASS)
            }.toList()
            else -> throw IllegalStateException("Unsupported file: $psiFile")
        }
    }
}
