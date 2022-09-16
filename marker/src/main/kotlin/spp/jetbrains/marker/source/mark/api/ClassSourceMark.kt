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
package spp.jetbrains.marker.source.mark.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiNameIdentifierOwner
import spp.jetbrains.marker.impl.ArtifactNamingService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import java.util.*

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
    override val valid: Boolean; get() {
        return try {
            psiClass.isValid && artifactQualifiedName == ArtifactNamingService.getFullyQualifiedName(psiClass)
        } catch (ignore: PsiInvalidElementAccessException) {
            false
        }
    }

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

    override val viewProviderBound: Boolean
        get() = try {
            psiClass.containingFile.viewProvider.document
            true
        } catch (ignore: PsiInvalidElementAccessException) {
            false
        }

    override fun apply(sourceMarkComponent: SourceMarkComponent, addToMarker: Boolean, editor: Editor?) {
        this.sourceMarkComponent = sourceMarkComponent
        super.apply(addToMarker, editor)
    }

    override fun apply(addToMarker: Boolean, editor: Editor?) {
        apply(configuration.componentProvider.getComponent(this), addToMarker, editor)
    }

    override fun dispose(removeFromMarker: Boolean, assertRemoval: Boolean) {
        when (this) {
            is GutterMark -> psiClass.nameIdentifier?.putUserData(SourceKey.GutterMark, null)
            is InlayMark -> psiClass.nameIdentifier?.putUserData(SourceKey.InlayMark, null)
            is GuideMark -> psiClass.nameIdentifier?.putUserData(SourceKey.GuideMark, null)
            else -> error("Unsupported source mark type: $this")
        }
        super.dispose(removeFromMarker, assertRemoval)
    }

    override suspend fun disposeSuspend(removeFromMarker: Boolean, assertRemoval: Boolean) {
        when (this) {
            is GutterMark -> psiClass.nameIdentifier?.putUserData(SourceKey.GutterMark, null)
            is InlayMark -> psiClass.nameIdentifier?.putUserData(SourceKey.InlayMark, null)
            is GuideMark -> psiClass.nameIdentifier?.putUserData(SourceKey.GuideMark, null)
            else -> error("Unsupported source mark type: $this")
        }
        super.disposeSuspend(removeFromMarker, assertRemoval)
    }

    private val userData = HashMap<Any, Any>()
    override fun getUserData() = userData
    override fun <T> getUserData(key: SourceKey<T>): T? = userData[key] as T?
    override fun <T> putUserData(key: SourceKey<T>, value: T?) {
        if (value != null) {
            userData.put(key, value)
        } else {
            userData.remove(key)
        }
        triggerEvent(SourceMarkEvent(this, SourceMarkEventCode.MARK_USER_DATA_UPDATED, key, value))
    }

    override fun hasUserData(): Boolean = userData.isNotEmpty()

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

    private val eventListeners = ArrayList<SourceMarkEventListener>()
    override fun clearEventListeners() = eventListeners.clear()
    override fun getEventListeners(): List<SourceMarkEventListener> = eventListeners.toList()
    override fun addEventListener(listener: SourceMarkEventListener) {
        eventListeners += listener
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (hashCode() != other.hashCode()) return false
        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(artifactQualifiedName, type)
    }

    override fun toString(): String = "${javaClass.simpleName}: $artifactQualifiedName"
}
