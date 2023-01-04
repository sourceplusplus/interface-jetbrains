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
package spp.jetbrains.marker.source.mark.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiNameIdentifierOwner
import spp.jetbrains.marker.service.ArtifactNamingService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.guide.GuideMark
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Represents a [SourceMark] associated to a class artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions")
abstract class ClassSourceMark(
    override val sourceFileMarker: SourceFileMarker,
    internal var psiClass: PsiNameIdentifierOwner,
) : SourceMark {

    override var artifactQualifiedName = ArtifactNamingService.getFullyQualifiedName(psiClass)
    override var editor: Editor? = null
    override lateinit var sourceMarkComponent: SourceMarkComponent
    override var visiblePopup: Disposable? = null
    override val isClassMark: Boolean = true
    override val isMethodMark: Boolean = false
    override val isExpressionMark: Boolean = false

    override val moduleName: String
        get() = ProjectRootManager.getInstance(sourceFileMarker.project).fileIndex
            .getModuleForFile(psiClass.containingFile.virtualFile)!!.name

    /**
     * Line number of the gutter mark.
     * One above the method name identifier.
     * First line for class (maybe? might want to make that for package level stats in the future)
     *
     * @return gutter mark line number
     */
    override val lineNumber: Int
        get() {
            val document = psiClass.nameIdentifier!!.containingFile.viewProvider.document
            return document!!.getLineNumber(psiClass.nameIdentifier!!.textRange.startOffset)
        }

    override fun apply(sourceMarkComponent: SourceMarkComponent, addToMarker: Boolean, editor: Editor?) {
        this.sourceMarkComponent = sourceMarkComponent
        super.apply(addToMarker, editor)
    }

    override fun apply(addToMarker: Boolean, editor: Editor?) {
        apply(configuration.componentProvider.getComponent(this), addToMarker, editor)
    }

    override val userData = HashMap<Any, Any>()

    override fun getPsiElement(): PsiNameIdentifierOwner {
        return psiClass
    }

    fun getNameIdentifier(): PsiElement {
        return psiClass.nameIdentifier
            ?: throw PsiInvalidElementAccessException(psiClass, "No name identifier. Artifact: $artifactQualifiedName")
    }

    fun updatePsiClass(psiClass: PsiNameIdentifierOwner): Boolean {
        this.psiClass = psiClass
        val newArtifactQualifiedName = ArtifactNamingService.getFullyQualifiedName(psiClass)
        if (artifactQualifiedName != newArtifactQualifiedName) {
            check(sourceFileMarker.removeSourceMark(this, autoRefresh = false, autoDispose = false))
            val oldArtifactQualifiedName = artifactQualifiedName
            artifactQualifiedName = newArtifactQualifiedName
            return if (sourceFileMarker.applySourceMark(this, autoRefresh = false)) {
                triggerEvent(SourceMarkEvent(this, SourceMarkEventCode.NAME_CHANGED, oldArtifactQualifiedName))
                true
            } else false
        }
        return true
    }

    override val eventListeners = CopyOnWriteArrayList<SourceMarkEventListener>()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (hashCode() != other.hashCode()) return false
        return true
    }

    override fun hashCode(): Int {
        if (this is GuideMark) {
            //compare by artifactQualifiedName
            return artifactQualifiedName.hashCode()
        } else {
            //compare by identity
            return super.hashCode()
        }
    }

    override fun toString(): String = "${javaClass.simpleName}: $artifactQualifiedName"
}
