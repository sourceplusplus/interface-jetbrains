package com.sourceplusplus.marker.source.mark.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiMethod
import com.sourceplusplus.marker.MarkerUtils
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.component.api.SourceMarkComponent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import org.jetbrains.uast.UMethod
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class MethodSourceMark(
    override val sourceFileMarker: SourceFileMarker,
    internal open var psiMethod: UMethod,
    override var artifactQualifiedName: String = MarkerUtils.getFullyQualifiedName(psiMethod)
) : SourceMark {

    override var editor: Editor? = null
    override lateinit var sourceMarkComponent: SourceMarkComponent
    override var visiblePopup: Disposable? = null
    override val isClassMark: Boolean = false
    override val isMethodMark: Boolean = true
    override val valid: Boolean; get() {
        return try {
            psiMethod.isPsiValid && artifactQualifiedName == MarkerUtils.getFullyQualifiedName(psiMethod)
        } catch (ex: PsiInvalidElementAccessException) {
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
            psiMethod.nameIdentifier!!.containingFile.viewProvider.document
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

    override fun dispose(removeFromMarker: Boolean) {
        psiMethod.nameIdentifier?.putUserData(SourceKey.GutterMark, null)
        psiMethod.nameIdentifier?.putUserData(SourceKey.InlayMark, null)
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

    fun getPsiMethod(): UMethod {
        return psiMethod
    }

    override fun getPsiElement(): PsiMethod {
        return psiMethod.sourcePsi as PsiMethod
    }

    fun updatePsiMethod(psiMethod: UMethod): Boolean {
        this.psiMethod = psiMethod
        val newArtifactQualifiedName = MarkerUtils.getFullyQualifiedName(psiMethod)
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

    fun isStaticMethod(): Boolean {
        return psiMethod.isStatic
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
