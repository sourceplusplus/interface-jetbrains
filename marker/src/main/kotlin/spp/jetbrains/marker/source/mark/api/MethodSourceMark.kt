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
package spp.jetbrains.marker.source.mark.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiNameIdentifierOwner
import spp.jetbrains.marker.SourceMarker.namingService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.protocol.artifact.ArtifactQualifiedName
import java.util.*

/**
 * Represents a [SourceMark] associated to a method artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions")
abstract class MethodSourceMark(
    override val sourceFileMarker: SourceFileMarker,
    internal open var psiMethod: PsiNameIdentifierOwner,
    override var artifactQualifiedName: ArtifactQualifiedName = namingService.getFullyQualifiedName(psiMethod)
) : SourceMark {

    override var editor: Editor? = null
    override lateinit var sourceMarkComponent: SourceMarkComponent
    override var visiblePopup: Disposable? = null
    override val isClassMark: Boolean = false
    override val isMethodMark: Boolean = true
    override val isExpressionMark: Boolean = false
    override val valid: Boolean; get() {
        return try {
            psiMethod.isValid && artifactQualifiedName == namingService.getFullyQualifiedName(psiMethod)
        } catch (ignore: PsiInvalidElementAccessException) {
            false
        }
    }

    override val moduleName: String
        get() = ProjectRootManager.getInstance(sourceFileMarker.project).fileIndex
            .getModuleForFile(psiMethod.containingFile.virtualFile)!!.name

    /**
     * Line number of the gutter mark.
     * One above the method name identifier.
     * First line for class (maybe? might want to make that for package level stats in the future)
     *
     * @return gutter mark line number
     */
    override val lineNumber: Int
        get() {
            val document = psiMethod.nameIdentifier!!.containingFile.viewProvider.document
            return document!!.getLineNumber(psiMethod.nameIdentifier!!.textRange.startOffset)
        }

    override val viewProviderBound: Boolean
        get() = try {
            psiMethod.containingFile.viewProvider.document
            true
        } catch (ignore: PsiInvalidElementAccessException) {
            false
        }

    @Synchronized
    override fun apply(sourceMarkComponent: SourceMarkComponent, addToMarker: Boolean) {
        this.sourceMarkComponent = sourceMarkComponent
        super.apply(addToMarker)
    }

    override fun apply(addToMarker: Boolean) {
        apply(configuration.componentProvider.getComponent(this), addToMarker)
    }

    override fun dispose(removeFromMarker: Boolean, assertRemoval: Boolean) {
        psiMethod.nameIdentifier?.putUserData(SourceKey.GutterMark, null)
        psiMethod.nameIdentifier?.putUserData(SourceKey.InlayMark, null)
        super.dispose(removeFromMarker, assertRemoval)
    }

    private val userData = HashMap<Any, Any>()
    override fun <T> getUserData(key: SourceKey<T>): T? = userData[key] as T?
    override fun <T> putUserData(key: SourceKey<T>, value: T?) {
        if (value != null) {
            userData.put(key, value)
        } else {
            userData.remove(key)
        }
    }

    override fun hasUserData(): Boolean = userData.isNotEmpty()

    fun getPsiMethod(): PsiNameIdentifierOwner {
        return psiMethod
    }

    override fun getPsiElement(): PsiNameIdentifierOwner {
        return psiMethod
    }

    fun updatePsiMethod(psiMethod: PsiNameIdentifierOwner): Boolean {
        this.psiMethod = psiMethod
        val newArtifactQualifiedName = namingService.getFullyQualifiedName(psiMethod)
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
