/*
 * Source++, the open-source live coding platform.
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
package spp.jetbrains.marker.source

import com.google.common.collect.ImmutableList
import com.google.common.collect.MapMaker
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import spp.jetbrains.ScopeExtensions.safeRunBlocking
import spp.jetbrains.marker.source.mark.api.*
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.guide.ClassGuideMark
import spp.jetbrains.marker.source.mark.guide.ExpressionGuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.jetbrains.marker.source.mark.gutter.ClassGutterMark
import spp.jetbrains.marker.source.mark.gutter.ExpressionGutterMark
import spp.jetbrains.marker.source.mark.gutter.MethodGutterMark
import spp.jetbrains.marker.source.mark.inlay.ExpressionInlayMark
import spp.jetbrains.marker.source.mark.inlay.MethodInlayMark
import spp.protocol.artifact.ArtifactQualifiedName
import java.util.*

/**
 * Used to mark a source code file with SourceMarker artifact marks.
 * SourceMarker artifact marks can be used to subscribe to and collect source code runtime information.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions")
open class SourceFileMarker(val psiFile: PsiFile) : SourceMarkProvider {

    companion object {
        val KEY = Key.create<SourceFileMarker>("sm.SourceFileMarker")
        private val log = logger<SourceFileMarker>()
        val SUPPORTED_FILE_TYPES = mutableListOf<Class<out PsiFile>>()

        @JvmStatic
        fun isFileSupported(psiFile: PsiFile): Boolean {
            return SUPPORTED_FILE_TYPES.any { it.isInstance(psiFile) }
        }
    }

    val project: Project = psiFile.project
    private val sourceMarks: MutableSet<SourceMark> = Collections.newSetFromMap(MapMaker().weakKeys().makeMap())

    /**
     * Gets the [SourceMark]s recognized in the current source code file.
     *
     * @return a list of the [SourceMark]s
     */
    open fun getSourceMarks(): List<SourceMark> {
        return ImmutableList.copyOf(sourceMarks)
    }

    open fun refresh() {
        if (!psiFile.project.isDisposed && !ApplicationManager.getApplication().isUnitTestMode) {
            try {
                DaemonCodeAnalyzer.getInstance(psiFile.project).restart(psiFile)
            } catch (ignored: ProcessCanceledException) {
            }
        }
    }

    open suspend fun clearSourceMarks() {
        val removed = sourceMarks.removeIf {
            safeRunBlocking {
                it.disposeSuspend(false)
            }
            true
        }
        if (removed) refresh()
    }

    open fun removeIfInvalid(sourceMark: SourceMark): Boolean {
        var removedMark = false
        if (!sourceMark.valid) {
            check(removeSourceMark(sourceMark))
            removedMark = true
        }
        if (removedMark) refresh()
        return removedMark
    }

    open fun removeInvalidSourceMarks(): Boolean {
        var removedMark = false
        sourceMarks.forEach {
            if (!it.valid) {
                check(removeSourceMark(it))
                removedMark = true
            }
        }
        if (removedMark) refresh()
        return removedMark
    }

    @JvmOverloads
    open fun removeSourceMark(
        sourceMark: SourceMark,
        autoRefresh: Boolean = false,
        autoDispose: Boolean = true
    ): Boolean {
        log.trace("Removing source mark for artifact: $sourceMark")
        return if (sourceMarks.remove(sourceMark)) {
            if (autoDispose) sourceMark.dispose(false)
            if (autoRefresh) refresh()
            log.trace("Removed source mark for artifact: $sourceMark")
            true
        } else false
    }

    @JvmOverloads
    open fun applySourceMark(
        sourceMark: SourceMark,
        autoRefresh: Boolean = false,
        overrideFilter: Boolean = false
    ): Boolean {
        if (overrideFilter || sourceMark.canApply()) {
            log.trace("Applying source mark for artifact: $sourceMark")
            sourceMark.triggerEvent(SourceMarkEvent(sourceMark, SourceMarkEventCode.MARK_BEFORE_ADDED))
            if (sourceMarks.add(sourceMark)) {
                when (sourceMark) {
                    is ClassGutterMark -> sourceMark.getNameIdentifier().putUserData(SourceKey.GutterMark, sourceMark)
                    is ClassGuideMark -> sourceMark.getNameIdentifier().putUserData(SourceKey.GuideMark, sourceMark)
                    is MethodGutterMark -> sourceMark.getNameIdentifier().putUserData(SourceKey.GutterMark, sourceMark)
                    is MethodInlayMark -> sourceMark.getNameIdentifier().putUserData(SourceKey.InlayMark, sourceMark)
                    is MethodGuideMark -> sourceMark.getNameIdentifier().putUserData(SourceKey.GuideMark, sourceMark)
                    is ExpressionGutterMark -> sourceMark.getPsiElement().putUserData(SourceKey.GutterMark, sourceMark)
                    is ExpressionInlayMark -> sourceMark.getPsiElement().putUserData(SourceKey.InlayMark, sourceMark)
                    is ExpressionGuideMark -> sourceMark.getPsiElement().putUserData(SourceKey.GuideMark, sourceMark)
                }

                if (autoRefresh) refresh()
                log.trace("Applied source mark for artifact: $sourceMark")
                return true
            }
        }
        return false
    }

    fun containsSourceMark(sourceMark: SourceMark): Boolean {
        return sourceMarks.contains(sourceMark)
    }

    fun containsPsiElement(psiElement: PsiElement): Boolean {
        return sourceMarks.find { it.getPsiElement() === psiElement } != null
    }

    open fun getSourceMark(artifactQualifiedName: ArtifactQualifiedName, type: SourceMark.Type): SourceMark? {
        return sourceMarks.find { it.artifactQualifiedName == artifactQualifiedName && it.type == type }
    }

    open fun getSourceMarks(artifactQualifiedName: ArtifactQualifiedName): List<SourceMark> {
        return sourceMarks.filter { it.artifactQualifiedName == artifactQualifiedName }
    }

    open fun getClassSourceMark(psiClass: PsiElement, type: SourceMark.Type): ClassSourceMark? {
        return sourceMarks.find {
            it is ClassSourceMark && it.valid && it.psiClass === psiClass && it.type == type
        } as ClassSourceMark?
    }

    open fun getMethodSourceMark(psiMethod: PsiElement, type: SourceMark.Type): MethodSourceMark? {
        return sourceMarks.find {
            it is MethodSourceMark && it.valid && it.psiMethod === psiMethod && it.type == type
        } as MethodSourceMark?
    }

    open fun getExpressionSourceMark(psiElement: PsiElement, type: SourceMark.Type): ExpressionSourceMark? {
        return sourceMarks.find {
            it is ExpressionSourceMark && it.valid && it.psiExpression === psiElement && it.type == type
        } as ExpressionSourceMark?
    }

    open fun getMethodSourceMarks(): List<MethodSourceMark> {
        return sourceMarks.filterIsInstance<MethodSourceMark>()
    }

    open fun getClassSourceMarks(): List<ClassSourceMark> {
        return sourceMarks.filterIsInstance<ClassSourceMark>()
    }

    open fun getMethodExpressionSourceMark(methodSourceMark: MethodSourceMark): List<ExpressionSourceMark> {
        return sourceMarks.filterIsInstance<ExpressionSourceMark>().filter {
            it.valid && it.psiExpression == methodSourceMark.psiMethod
        }
    }

    override fun createExpressionSourceMark(psiExpression: PsiElement, type: SourceMark.Type): ExpressionSourceMark {
        log.trace("Creating source mark. Expression: $psiExpression - Type: $type")
        return when (type) {
            SourceMark.Type.GUTTER -> ExpressionGutterMark(this, psiExpression)
            SourceMark.Type.INLAY -> ExpressionInlayMark(this, psiExpression)
            SourceMark.Type.GUIDE -> ExpressionGuideMark(this, psiExpression)
        }
    }

    override fun createMethodSourceMark(psiMethod: PsiNameIdentifierOwner, type: SourceMark.Type): MethodSourceMark {
        return when (type) {
            SourceMark.Type.GUTTER -> MethodGutterMark(this, psiMethod)
            SourceMark.Type.INLAY -> MethodInlayMark(this, psiMethod)
            SourceMark.Type.GUIDE -> MethodGuideMark(this, psiMethod)
        }
    }

    fun createMethodGutterMark(
        element: PsiNameIdentifierOwner,
        autoApply: Boolean = false
    ): MethodGutterMark {
        log.trace("createMethodGutterMark: $element")
        val gutterMark = createMethodSourceMark(
            element,
            SourceMark.Type.GUTTER
        ) as MethodGutterMark
        return if (autoApply) {
            if (gutterMark.canApply()) {
                gutterMark.apply(true)
                gutterMark
            } else {
                error("Could not apply gutter mark: $gutterMark")
            }
        } else {
            gutterMark
        }
    }

    fun createMethodInlayMark(
        element: PsiNameIdentifierOwner,
        autoApply: Boolean = false
    ): MethodInlayMark {
        log.trace("createMethodInlayMark: $element")
        val inlayMark = createMethodSourceMark(
            element,
            SourceMark.Type.INLAY
        ) as MethodInlayMark
        return if (autoApply) {
            if (inlayMark.canApply()) {
                inlayMark.apply(true)
                inlayMark
            } else {
                error("Could not apply inlay mark: $inlayMark")
            }
        } else {
            inlayMark
        }
    }

    override fun createClassSourceMark(psiClass: PsiNameIdentifierOwner, type: SourceMark.Type): ClassSourceMark {
        return when (type) {
            SourceMark.Type.GUTTER -> ClassGutterMark(this, psiClass)
            SourceMark.Type.INLAY -> TODO("Not yet implemented")
            SourceMark.Type.GUIDE -> ClassGuideMark(this, psiClass)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SourceFileMarker
        if (psiFile != other.psiFile) return false
        return true
    }

    override fun hashCode(): Int {
        return psiFile.hashCode()
    }

    override fun toString(): String {
        return "SourceFileMarker:${psiFile.name}"
    }
}
