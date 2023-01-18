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
package spp.jetbrains.marker.jvm.service

import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.ClassUtil
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.jvm.service.utils.JVMMarkerUtils
import spp.jetbrains.marker.service.define.IArtifactNamingService
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.protocol.artifact.ArtifactLanguage
import spp.protocol.artifact.ArtifactNameUtils
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.exception.LiveStackTraceElement
import spp.protocol.artifact.exception.qualifiedClassName
import spp.protocol.instrument.location.LiveSourceLocation

/**
 * Used to determine the naming/location of JVM artifacts.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactNamingService : IArtifactNamingService {

    companion object {
        private val log = logger<SourceMarker>()
    }

    override fun getLiveSourceLocation(
        sourceMark: SourceMark,
        lineNumber: Int,
        serviceName: String?
    ): LiveSourceLocation? {
        val locationSource = sourceMark.artifactQualifiedName.toClass()?.identifier
        if (locationSource == null) {
            log.warn("Unable to determine location source of: $sourceMark")
            return null
        }
        return LiveSourceLocation(locationSource, lineNumber, service = serviceName)
    }

    override fun getDisplayLocation(language: Language, artifactQualifiedName: ArtifactQualifiedName): String {
        var fullyQualified = artifactQualifiedName.identifier
        if (fullyQualified.contains("#")) {
            fullyQualified = fullyQualified.substring(0, fullyQualified.indexOf("#"))
        }
        val className = ArtifactNameUtils.getClassName(fullyQualified)!!
        var location = if (fullyQualified.contains("(")) {
            val shortFuncName = ArtifactNameUtils.getShortFunctionSignature(
                ArtifactNameUtils.removePackageNames(fullyQualified)!!
            )
            "$className.$shortFuncName"
        } else {
            className
        }

        //remove method params if location is too long
        if (location.length > 75 && location.contains("(") && !location.contains("()")) {
            location = location.substring(0, location.indexOf("(")) + "(...)"
        }

        //remove class name if location is still too long
        if (location.length > 75 && location.contains(".")) {
            location = location.substring(location.indexOf(".") + 1)
        }
        return location
    }

    override fun getVariableName(element: PsiElement): String? {
        return when (element) {
            is PsiDeclarationStatement -> {
                val localVar = element.firstChild as? PsiLocalVariable
                localVar?.name
            }

            is PsiLocalVariable -> element.name
            else -> null
        }
    }

    override fun getFullyQualifiedName(element: PsiElement): ArtifactQualifiedName {
        return JVMMarkerUtils.getFullyQualifiedName(element)
    }

    override fun findPsiFile(language: ArtifactLanguage, project: Project, location: String): PsiFile? {
        val psiManager = PsiManager.getInstance(project)
        val psiClass = ClassUtil.findPsiClassByJVMName(psiManager, location)
        return psiClass?.containingFile ?: super.findPsiFile(language, project, location)
    }

    override fun findPsiFile(language: ArtifactLanguage, project: Project, frame: LiveStackTraceElement): PsiFile? {
        val psiManager = PsiManager.getInstance(project)
        val psiClass = ClassUtil.findPsiClassByJVMName(psiManager, frame.qualifiedClassName())
        return psiClass?.containingFile
    }
}
