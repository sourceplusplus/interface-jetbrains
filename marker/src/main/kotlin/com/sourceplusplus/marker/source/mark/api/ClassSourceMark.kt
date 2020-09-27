package com.sourceplusplus.marker.source.mark.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiInvalidElementAccessException
import com.sourceplusplus.marker.MarkerUtils
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.component.api.SourceMarkComponent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import org.jetbrains.uast.UClass
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class ClassSourceMark(
    override val sourceFileMarker: SourceFileMarker,
    internal open var psiClass: UClass,
    override var artifactQualifiedName: String = psiClass.qualifiedName!!
) : SourceMark {

    override var editor: Editor? = null
    override lateinit var sourceMarkComponent: SourceMarkComponent
    override var visiblePopup: Disposable? = null
    override val isClassMark: Boolean = true
    override val isMethodMark: Boolean = false
    override val valid: Boolean; get() {
        return try {
            psiClass.isPsiValid && artifactQualifiedName == psiClass.qualifiedName!!
        } catch (ex: PsiInvalidElementAccessException) {
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
            psiClass.nameIdentifier!!.containingFile.viewProvider.document
            true
        } catch (ignore: PsiInvalidElementAccessException) {
            false
        }

    override fun apply(sourceMarkComponent: SourceMarkComponent, addToMarker: Boolean) {
        this.sourceMarkComponent = sourceMarkComponent
        super.apply(addToMarker)
    }

    override fun apply(addToMarker: Boolean) {
        apply(configuration.componentProvider.getComponent(this), addToMarker)
    }

    override fun dispose(removeFromMarker: Boolean) {
        psiClass.nameIdentifier?.putUserData(SourceKey.GutterMark, null)
        psiClass.nameIdentifier?.putUserData(SourceKey.InlayMark, null)
        super.dispose(removeFromMarker)
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

    fun getPsiClass(): UClass {
        return psiClass
    }

    override fun getPsiElement(): PsiClass {
        return psiClass.sourcePsi as PsiClass
    }

    fun updatePsiClass(psiClass: UClass): Boolean {
        this.psiClass = psiClass
        val newArtifactQualifiedName = MarkerUtils.getFullyQualifiedName(psiClass)
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
