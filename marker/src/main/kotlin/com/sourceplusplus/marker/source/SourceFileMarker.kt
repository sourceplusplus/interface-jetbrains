package com.sourceplusplus.marker.source

import com.google.common.collect.ImmutableList
import com.google.common.collect.Sets
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.sourceplusplus.marker.source.mark.api.*
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.marker.source.mark.gutter.ClassGutterMark
import com.sourceplusplus.marker.source.mark.gutter.MethodGutterMark
import com.sourceplusplus.marker.source.mark.inlay.ExpressionInlayMark
import com.sourceplusplus.marker.source.mark.inlay.MethodInlayMark
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.slf4j.LoggerFactory

/**
 * Used to mark a source code file with SourceMarker artifact marks.
 * SourceMarker artifact marks can be used to subscribe to and collect source code runtime information.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class SourceFileMarker(val psiFile: PsiFile) : SourceMarkProvider {

    companion object {
        val KEY = Key.create<SourceFileMarker>("sm.SourceFileMarker")
        private val log = LoggerFactory.getLogger(SourceFileMarker::class.java)

        @JvmStatic
        fun isFileSupported(psiFile: PsiFile): Boolean {
            return try {
                when (psiFile) {
                    is GroovyFile -> true
                    is PsiJavaFile -> true
                    is KtFile -> true
                    else -> false
                }
            } catch (t: Throwable) {
                false
            }
        }
    }

    private val sourceMarks: MutableSet<SourceMark> = Sets.newConcurrentHashSet()
    val project: Project = psiFile.project

    /**
     * Gets the [SourceMark]s recognized in the current source code file.
     *
     * @return a list of the [SourceMark]s
     */
    open fun getSourceMarks(): List<SourceMark> {
        return ImmutableList.copyOf(sourceMarks)
    }

    open fun refresh() {
        if (!psiFile.project.isDisposed) {
            DaemonCodeAnalyzer.getInstance(psiFile.project).restart(psiFile)
        }
    }

    open fun clearSourceMarks() {
        val removed = sourceMarks.removeIf {
            it.dispose(false)
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
            if (sourceMarks.add(sourceMark)) {
                when (sourceMark) {
                    is ClassGutterMark -> sourceMark.getPsiElement().nameIdentifier!!.putUserData(
                        SourceKey.GutterMark,
                        sourceMark
                    )
                    is MethodGutterMark -> sourceMark.getPsiElement().nameIdentifier!!.putUserData(
                        SourceKey.GutterMark,
                        sourceMark
                    )
                    is MethodInlayMark -> sourceMark.getPsiElement().nameIdentifier!!.putUserData(
                        SourceKey.InlayMark,
                        sourceMark
                    )
                    is ExpressionInlayMark -> sourceMark.getPsiElement().putUserData(SourceKey.InlayMark, sourceMark)
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

    open fun getSourceMark(artifactQualifiedName: String, type: SourceMark.Type): SourceMark? {
        return sourceMarks.find { it.artifactQualifiedName == artifactQualifiedName && it.type == type }
    }

    open fun getSourceMarks(artifactQualifiedName: String): List<SourceMark> {
        return sourceMarks.filter { it.artifactQualifiedName == artifactQualifiedName }
    }

    open fun getClassSourceMark(psiClass: PsiElement, type: SourceMark.Type): ClassSourceMark? {
        return sourceMarks.find {
            it is ClassSourceMark && it.valid && it.psiClass.sourcePsi === psiClass && it.type == type
        } as ClassSourceMark?
    }

    open fun getMethodSourceMark(psiMethod: PsiElement, type: SourceMark.Type): MethodSourceMark? {
        return sourceMarks.find {
            it is MethodSourceMark && it.valid && it.psiMethod.sourcePsi === psiMethod && it.type == type
        } as MethodSourceMark?
    }

    open fun getExpressionSourceMark(psiElement: PsiElement, type: SourceMark.Type): ExpressionSourceMark? {
        return sourceMarks.find {
            it is ExpressionSourceMark && it.valid && it.psiExpression.sourcePsi === psiElement && it.type == type
        } as ExpressionSourceMark?
    }

    override fun createSourceMark(psiExpression: UExpression, type: SourceMark.Type): ExpressionSourceMark {
        log.trace("Creating source mark. Expression: $psiExpression - Type: $type")
        return when (type) {
            SourceMark.Type.GUTTER -> {
                TODO("Not yet implemented")
            }
            SourceMark.Type.INLAY -> {
                ExpressionInlayMark(this, psiExpression)
            }
        }
    }

    override fun createSourceMark(psiMethod: UMethod, type: SourceMark.Type): MethodSourceMark {
        log.trace("Creating source mark. Method: ${psiMethod.name} - Type: $type")
        return when (type) {
            SourceMark.Type.GUTTER -> {
                MethodGutterMark(this, psiMethod)
            }
            SourceMark.Type.INLAY -> {
                MethodInlayMark(this, psiMethod)
            }
        }
    }

    override fun createSourceMark(psiClass: UClass, type: SourceMark.Type): ClassSourceMark {
        log.trace("Creating source mark. Class: ${psiClass.qualifiedName} - Type: $type")
        return when (type) {
            SourceMark.Type.GUTTER -> {
                ClassGutterMark(this, psiClass)
            }
            SourceMark.Type.INLAY -> {
                TODO("Not yet implemented")
            }
        }
    }

    open fun getClassQualifiedNames(): List<String> {
        return when (psiFile) {
            is PsiClassOwner -> psiFile.classes.map { it.qualifiedName!! }.toList()
            else -> throw IllegalStateException("Unsupported file: $psiFile")
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
