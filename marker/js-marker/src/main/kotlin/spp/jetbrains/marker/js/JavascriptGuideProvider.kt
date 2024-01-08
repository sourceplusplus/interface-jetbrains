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
package spp.jetbrains.marker.js

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import spp.jetbrains.marker.service.define.AbstractSourceGuideProvider
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.guide.GuideMark

/**
 * Used to create JavaScript [GuideMark]s for high-level code constructs.
 *
 * @since 0.7.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JavascriptGuideProvider : AbstractSourceGuideProvider {

    override fun determineGuideMarks(fileMarker: SourceFileMarker) {
        fileMarker.psiFile.acceptChildren(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                if (element is JSCallExpression) {
                    ApplicationManager.getApplication().runReadAction {
                        fileMarker.createExpressionSourceMark(
                            element, SourceMark.Type.GUIDE
                        ).applyIfMissing()
                    }
                }
                if (element is JSFunction) {
                    ApplicationManager.getApplication().runReadAction {
                        if (element.nameIdentifier != null) {
                            fileMarker.createMethodSourceMark(
                                element, SourceMark.Type.GUIDE
                            ).applyIfMissing()
                        }
                    }
                }
            }
        })
    }
}
