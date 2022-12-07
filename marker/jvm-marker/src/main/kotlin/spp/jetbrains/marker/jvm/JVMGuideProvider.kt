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
package spp.jetbrains.marker.jvm

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import spp.jetbrains.marker.service.ArtifactTypeService
import spp.jetbrains.marker.service.define.AbstractSourceGuideProvider
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.guide.GuideMark

/**
 * Used to create JVM [GuideMark]s for high-level code constructs.
 *
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMGuideProvider : AbstractSourceGuideProvider {

    override fun determineGuideMarks(fileMarker: SourceFileMarker) {
        fileMarker.psiFile.acceptChildren(object : JavaRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                if (element is PsiClass) {
                    makeClassGuideMark(fileMarker, element)
                } else if (element is PsiMethod) {
                    makeMethodGuideMark(fileMarker, element)
                }

                if (ArtifactTypeService.isKotlin(element)) {
                    if (element is KtClass) {
                        makeClassGuideMark(fileMarker, element)
                    } else if (element is KtNamedFunction && !element.isExtensionDeclaration()) {
                        makeMethodGuideMark(fileMarker, element)
                    }
                }
            }
        })
    }

    private fun makeClassGuideMark(fileMarker: SourceFileMarker, element: PsiElement) {
        ApplicationManager.getApplication().runReadAction {
            val nameIdentifierOwner = element as PsiNameIdentifierOwner
            if (nameIdentifierOwner.nameIdentifier == null) {
                return@runReadAction //anonymous class
            }

            fileMarker.createClassSourceMark(
                nameIdentifierOwner, SourceMark.Type.GUIDE
            ).applyIfMissing()
        }
    }

    private fun makeMethodGuideMark(fileMarker: SourceFileMarker, element: PsiNameIdentifierOwner) {
        ApplicationManager.getApplication().runReadAction {
            fileMarker.createMethodSourceMark(
                element, SourceMark.Type.GUIDE
            ).applyIfMissing()
        }
    }
}
