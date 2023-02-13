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

import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiStatement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.imports.GrImportStatement
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.packaging.GrPackageDefinition
import spp.jetbrains.artifact.service.ArtifactTypeService
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.jvm.service.utils.JVMMarkerUtils
import spp.jetbrains.marker.service.define.IArtifactCreationService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.ExpressionSourceMark
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.guide.ExpressionGuideMark
import spp.jetbrains.marker.source.mark.gutter.ExpressionGutterMark
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.marker.source.mark.gutter.MethodGutterMark
import spp.jetbrains.marker.source.mark.inlay.ExpressionInlayMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.marker.source.mark.inlay.MethodInlayMark
import java.util.*

/**
 * Used to create Java [SourceMark]s.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions") // public API
class JVMArtifactCreationService : IArtifactCreationService {

    companion object {
        private val log = logger<JVMArtifactCreationService>()
    }

    override fun getOrCreateExpressionGutterMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): Optional<ExpressionGutterMark> {
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber)
        return if (element is PsiStatement) {
            Optional.ofNullable(getOrCreateExpressionGutterMark(fileMarker, element, autoApply))
        } else Optional.empty()
    }

    override fun getOrCreateMethodGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): MethodGutterMark? {
        var gutterMark = element.getUserData(GutterMark.KEY) as MethodGutterMark?
        if (gutterMark == null) {
            gutterMark = fileMarker.getMethodSourceMark(element.parent, SourceMark.Type.GUTTER) as MethodGutterMark?
            if (gutterMark != null) {
                if (gutterMark.updatePsiMethod(element.parent as PsiNameIdentifierOwner)) {
                    element.putUserData(GutterMark.KEY, gutterMark)
                } else {
                    gutterMark = null
                }
            }
        }

        if (gutterMark == null) {
            gutterMark = fileMarker.createMethodSourceMark(
                element.parent as PsiNameIdentifierOwner,
                SourceMark.Type.GUTTER
            ) as MethodGutterMark
            return if (autoApply) {
                gutterMark.apply(true)
                gutterMark
            } else {
                gutterMark
            }
        } else {
            return when {
                fileMarker.removeIfInvalid(gutterMark) -> {
                    element.putUserData(GutterMark.KEY, null)
                    null
                }

                gutterMark.configuration.icon != null -> {
                    gutterMark.setVisible(true)
                    gutterMark
                }

                else -> {
                    gutterMark.setVisible(false)
                    gutterMark
                }
            }
        }
    }

    override fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): Optional<ExpressionInlayMark> {
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber)
        if (element is LeafPsiElement && ArtifactTypeService.isGroovy(element)) {
            if (element.parent is GrImportStatement) {
                return Optional.empty()
            } else if (element.parent is GrPackageDefinition) {
                return Optional.empty()
            }
        }

        return when (element) {
            is PsiStatement -> Optional.ofNullable(getOrCreateExpressionInlayMark(fileMarker, element, autoApply))
            is PsiElement -> Optional.ofNullable(getOrCreateExpressionInlayMark(fileMarker, element, autoApply))
            else -> Optional.empty()
        }
    }

    override fun createMethodGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): MethodGutterMark {
        return fileMarker.createMethodGutterMark(element.parent as PsiNameIdentifierOwner, autoApply)
    }

    override fun createMethodInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): MethodInlayMark {
        return fileMarker.createMethodInlayMark(element.parent as PsiNameIdentifierOwner, autoApply)
    }

    override fun createExpressionGutterMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): ExpressionGutterMark {
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber) as PsiElement
        return fileMarker.createExpressionGutterMark(element, autoApply)
    }

    override fun createExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): ExpressionInlayMark {
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber) as PsiElement
        return createExpressionSourceMark(fileMarker, element, SourceMark.Type.INLAY, autoApply) as ExpressionInlayMark
    }

    override fun createExpressionGuideMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): ExpressionGuideMark {
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber) as PsiElement
        return createExpressionSourceMark(fileMarker, element, SourceMark.Type.GUIDE, autoApply) as ExpressionGuideMark
    }

    private fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiStatement,
        autoApply: Boolean = false
    ): ExpressionInlayMark? {
        log.trace("getOrCreateExpressionInlayMark: $element")
        var inlayMark = element.getUserData(InlayMark.KEY)?.firstOrNull() as ExpressionInlayMark?
        if (inlayMark == null) {
            inlayMark = fileMarker.getExpressionSourceMark(
                element,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark?
            if (inlayMark != null) {
                if (inlayMark.updatePsiExpression(
                        element,
                        JVMMarkerUtils.getFullyQualifiedName(element)
                    )
                ) {
                    element.putUserData(
                        InlayMark.KEY,
                        element.getUserData(InlayMark.KEY)?.plus(inlayMark) ?: setOf(inlayMark)
                    )
                } else {
                    inlayMark = null
                }
            }
        }

        return if (inlayMark == null) {
            inlayMark = fileMarker.createExpressionSourceMark(
                element,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark
            return if (autoApply) {
                inlayMark.apply(true)
                inlayMark
            } else {
                inlayMark
            }
        } else {
            if (fileMarker.removeIfInvalid(inlayMark)) {
                element.putUserData(InlayMark.KEY, null)
                null
            } else {
                inlayMark
            }
        }
    }

    private fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = false
    ): ExpressionInlayMark? {
        log.trace("getOrCreateExpressionInlayMark: $element")
        var inlayMark = element.getUserData(InlayMark.KEY)?.firstOrNull() as ExpressionInlayMark?
        if (inlayMark == null) {
            inlayMark = fileMarker.getExpressionSourceMark(
                element,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark?
            if (inlayMark != null) {
                if (inlayMark.updatePsiExpression(element, JVMMarkerUtils.getFullyQualifiedName(element))) {
                    element.putUserData(
                        InlayMark.KEY,
                        element.getUserData(InlayMark.KEY)?.plus(inlayMark) ?: setOf(inlayMark)
                    )
                } else {
                    inlayMark = null
                }
            }
        }

        return if (inlayMark == null) {
            inlayMark = fileMarker.createExpressionSourceMark(
                element,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark
            return if (autoApply) {
                inlayMark.apply(true)
                inlayMark
            } else {
                inlayMark
            }
        } else {
            if (fileMarker.removeIfInvalid(inlayMark)) {
                element.putUserData(InlayMark.KEY, null)
                null
            } else {
                inlayMark
            }
        }
    }

    private fun createExpressionSourceMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        type: SourceMark.Type,
        autoApply: Boolean = false
    ): ExpressionSourceMark {
        log.trace("createExpressionSourceMark: $element")
        val inlayMark = fileMarker.createExpressionSourceMark(element, type)
        return if (autoApply) {
            inlayMark.apply(true)
            inlayMark
        } else {
            inlayMark
        }
    }

    private fun getOrCreateExpressionGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiStatement,
        autoApply: Boolean = false
    ): ExpressionGutterMark? {
        log.trace("getOrCreateExpressionGutterMark: $element")
        var gutterMark = element.getUserData(GutterMark.KEY) as ExpressionGutterMark?
        if (gutterMark == null) {
            gutterMark = fileMarker.getExpressionSourceMark(
                element,
                SourceMark.Type.GUTTER
            ) as ExpressionGutterMark?
            if (gutterMark != null) {
                if (gutterMark.updatePsiExpression(
                        element,
                        JVMMarkerUtils.getFullyQualifiedName(element)
                    )
                ) {
                    element.putUserData(GutterMark.KEY, gutterMark)
                } else {
                    gutterMark = null
                }
            }
        }

        return if (gutterMark == null) {
            gutterMark = fileMarker.createExpressionSourceMark(
                element,
                SourceMark.Type.GUTTER
            ) as ExpressionGutterMark
            return if (autoApply) {
                gutterMark.apply(true)
                gutterMark
            } else {
                gutterMark
            }
        } else {
            if (fileMarker.removeIfInvalid(gutterMark)) {
                element.putUserData(GutterMark.KEY, null)
                null
            } else {
                gutterMark
            }
        }
    }
}
