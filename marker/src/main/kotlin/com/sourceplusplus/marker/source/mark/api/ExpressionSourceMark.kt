package com.sourceplusplus.marker.source.mark.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.sourceplusplus.marker.MarkerUtils
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.component.api.SourceMarkComponent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import org.jetbrains.uast.UDeclarationsExpression
import org.jetbrains.uast.UExpression
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class ExpressionSourceMark(
    override val sourceFileMarker: SourceFileMarker,
    internal open var psiExpression: UExpression,
    override var artifactQualifiedName: String = MarkerUtils.getFullyQualifiedName(psiExpression)
) : SourceMark {

    override var editor: Editor? = null
    override lateinit var sourceMarkComponent: SourceMarkComponent
    override var visiblePopup: Disposable? = null
    override val isClassMark: Boolean = false
    override val isMethodMark: Boolean = true
    override val valid: Boolean; get() {
        return try {
            psiExpression.isPsiValid && artifactQualifiedName == MarkerUtils.getFullyQualifiedName(psiExpression)
        } catch (ex: PsiInvalidElementAccessException) {
            false
        }
    }

    override val moduleName: String
        get() = TODO("moduleName")

    override val lineNumber: Int
        get() {
            val document = getPsiElement().containingFile.viewProvider.document
            return document!!.getLineNumber(getPsiElement().textRange.startOffset) + 1
        }

    override val viewProviderBound: Boolean
        get() = TODO("viewProviderBound")

    @Synchronized
    override fun apply(sourceMarkComponent: SourceMarkComponent, addToMarker: Boolean) {
        this.sourceMarkComponent = sourceMarkComponent
        super.apply(addToMarker)
    }

    override fun apply(addToMarker: Boolean) {
        apply(configuration.componentProvider.getComponent(this), addToMarker)
    }

    override fun dispose(removeFromMarker: Boolean) {
        getPsiElement().putUserData(SourceKey.GutterMark, null)
        getPsiElement().putUserData(SourceKey.InlayMark, null)
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

    fun getPsiExpresion(): UExpression {
        return psiExpression
    }

    override fun getPsiElement(): PsiElement {
        if (psiExpression is UDeclarationsExpression) {
            //todo: support for multi-declaration statements
            return (psiExpression as UDeclarationsExpression).declarations[0].sourcePsi!!
        } else {
            return psiExpression.sourcePsi!!
        }
    }

    fun updatePsiExpression(psiExpression: UExpression): Boolean {
        this.psiExpression = psiExpression
        val newArtifactQualifiedName = MarkerUtils.getFullyQualifiedName(psiExpression)
        if (artifactQualifiedName != newArtifactQualifiedName) {
            check(sourceFileMarker.removeSourceMark(this, autoRefresh = false))
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
