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
package spp.jetbrains.marker.py

import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import spp.jetbrains.marker.service.define.AbstractSourceGuideProvider
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.guide.GuideMark

/**
 * Used to create Python [GuideMark]s for high-level code constructs.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonGuideProvider : AbstractSourceGuideProvider {

    override fun determineGuideMarks(fileMarker: SourceFileMarker) {
        fileMarker.psiFile.acceptChildren(object : PyRecursiveElementVisitor() {
            override fun visitPyFunction(element: PyFunction) {
                super.visitPyFunction(element)

                ApplicationManager.getApplication().runReadAction {
                    fileMarker.createMethodSourceMark(
                        element, SourceMark.Type.GUIDE
                    ).applyIfMissing()
                }
            }
        })
    }
}
