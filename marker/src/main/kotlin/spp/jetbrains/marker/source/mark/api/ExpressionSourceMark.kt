package spp.jetbrains.marker.source.mark.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import spp.jetbrains.marker.SourceMarker.namingService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.protocol.artifact.ArtifactQualifiedName
import java.util.*

/**
 * Represents a [SourceMark] associated to an expression artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions")
abstract class ExpressionSourceMark(
    override val sourceFileMarker: SourceFileMarker,
    internal open var psiExpression: PsiElement,
    override var artifactQualifiedName: ArtifactQualifiedName = namingService.getFullyQualifiedName(psiExpression)
) : SourceMark {

    override var editor: Editor? = null
    override lateinit var sourceMarkComponent: SourceMarkComponent
    override var visiblePopup: Disposable? = null
    override val isClassMark: Boolean = false
    override val isMethodMark: Boolean = false
    override val isExpressionMark: Boolean = true
    override val valid: Boolean; get() {
        return try {
            psiExpression.isValid && artifactQualifiedName == namingService.getFullyQualifiedName(psiExpression)
        } catch (ignore: PsiInvalidElementAccessException) {
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
        get() = try {
            psiExpression.containingFile.viewProvider.document
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
        when (this) {
            is GutterMark -> getPsiElement().putUserData(SourceKey.GutterMark, null)
            is InlayMark -> getPsiElement().putUserData(SourceKey.InlayMark, null)
            else -> throw IllegalStateException("ExpressionSourceMark is not a GutterMark or InlayMark")
        }
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

    fun getPsiExpression(): PsiElement {
        return psiExpression
    }

    override fun getPsiElement(): PsiElement {
        return psiExpression
    }

    fun updatePsiExpression(psiExpression: PsiElement, newArtifactQualifiedName: ArtifactQualifiedName): Boolean {
        this.psiExpression = psiExpression
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
