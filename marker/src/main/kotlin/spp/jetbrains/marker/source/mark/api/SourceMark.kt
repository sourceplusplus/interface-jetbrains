/*
 * Source++, the continuous feedback platform for developers.
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

import com.intellij.codeInsight.hints.InlayHintsPassFactory
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.ui.BalloonImpl
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import spp.jetbrains.ScopeExtensions.safeGlobalLaunch
import spp.jetbrains.ScopeExtensions.safeRunBlocking
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.plugin.SourceInlayComponentProvider
import spp.jetbrains.marker.plugin.SourceInlayHintProvider
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponent
import spp.jetbrains.marker.source.mark.api.config.SourceMarkConfiguration
import spp.jetbrains.marker.source.mark.api.event.*
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.marker.source.mark.gutter.event.GutterMarkEventCode
import spp.jetbrains.marker.source.mark.inlay.ExpressionInlayMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.marker.source.mark.inlay.event.InlayMarkEventCode.INLAY_MARK_HIDDEN
import spp.jetbrains.marker.source.mark.inlay.event.InlayMarkEventCode.INLAY_MARK_VISIBLE
import spp.protocol.artifact.ArtifactQualifiedName
import java.awt.event.ComponentEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities
import kotlin.concurrent.schedule

/**
 * Used to associate visualizations near and/or inside of source code.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
interface SourceMark : JBPopupListener, MouseMotionListener, VisibleAreaListener {

    /**
     * todo: description.
     */
    enum class Type {
        GUTTER,
        INLAY,
        GUIDE
    }

    companion object {
        private val log = logger<SourceMark>()
        private val buildingPopup = AtomicBoolean()
        private var openedMarks: MutableList<SourceMark> = ArrayList()

        @JvmStatic
        fun closeOpenPopups() {
            openedMarks.toList().forEach {
                it.closePopup()
            }
        }
    }

    val id: String
    val type: Type
    val isClassMark: Boolean
    val isMethodMark: Boolean
    val isExpressionMark: Boolean
    val moduleName: String
    val artifactQualifiedName: ArtifactQualifiedName
    val sourceFileMarker: SourceFileMarker
    val lineNumber: Int
    var editor: Editor?
    var visiblePopup: Disposable?
    val configuration: SourceMarkConfiguration
    var sourceMarkComponent: SourceMarkComponent
    val project: Project; get() = sourceFileMarker.project
    val language: Language
        get() = getPsiElement().language

    val valid: Boolean
        get() = try {
            getPsiElement().isValid
        } catch (ignore: PsiInvalidElementAccessException) {
            false
        }

    val viewProviderBound: Boolean
        get() = try {
            getPsiElement().containingFile.viewProvider.document
            true
        } catch (ignore: PsiInvalidElementAccessException) {
            false
        }

    fun getPsiElement(): PsiElement

    fun isVisible(): Boolean
    fun setVisible(visible: Boolean)

    fun applyIfMissing() {
        if (!sourceFileMarker.containsSourceMark(this)) {
            apply(true)
        }
    }

    fun apply(sourceMarkComponent: SourceMarkComponent, addToMarker: Boolean = true, editor: Editor? = null)
    fun apply(addToMarker: Boolean = true, editor: Editor? = null) {
        SourceMarker.getInstance(project).getGlobalSourceMarkEventListeners().forEach(::addEventListener)

        if (addToMarker && sourceFileMarker.applySourceMark(this, autoRefresh = true)) {
            triggerEvent(SourceMarkEvent(this, SourceMarkEventCode.MARK_ADDED))

            if (this is GutterMark) {
                setVisible(isVisible() && configuration.icon != null)
                if (configuration.icon != null) {
                    if (isVisible()) {
                        setVisible(true)

                        //initial mark visible event
                        triggerEvent(SourceMarkEvent(this, GutterMarkEventCode.GUTTER_MARK_VISIBLE))
                    } else {
                        setVisible(false)
                    }
                } else {
                    setVisible(false)
                }
            } else if (this is InlayMark) {
                if (configuration.showComponentInlay) {
                    val selectedEditor = editor ?: FileEditorManager.getInstance(project).selectedTextEditor!!
                    val provider = SourceInlayComponentProvider.from(selectedEditor)
                    val viewport = (selectedEditor as? EditorImpl)?.scrollPane?.viewport!!
                    var displayLineIndex = artifactQualifiedName.lineNumber!! - 1
                    if (this is ExpressionInlayMark) {
                        if (showAboveExpression) {
                            displayLineIndex--
                        }
                    }
                    if (displayLineIndex < 0) {
                        displayLineIndex = 0
                    }

                    if (isVisible()) {
                        val inlay = provider.insertAfter(
                            displayLineIndex,
                            configuration.componentProvider.getComponent(this).getComponent()
                        )
                        configuration.inlayRef = Ref.create()
                        configuration.inlayRef!!.set(inlay)
                        viewport.dispatchEvent(ComponentEvent(viewport, ComponentEvent.COMPONENT_RESIZED))

                        //initial mark visible event
                        triggerEvent(SourceMarkEvent(this, INLAY_MARK_VISIBLE))
                    } else {
                        setVisible(false)
                    }

                    addEventListener(SynchronousSourceMarkEventListener {
                        when (it.eventCode) {
                            INLAY_MARK_VISIBLE -> {
                                ApplicationManager.getApplication().invokeLater {
                                    configuration.inlayRef = Ref.create()
                                    configuration.inlayRef!!.set(
                                        provider.insertAfter(
                                            displayLineIndex,
                                            configuration.componentProvider.getComponent(this).getComponent()
                                        )
                                    )
                                    viewport.dispatchEvent(
                                        ComponentEvent(viewport, ComponentEvent.COMPONENT_RESIZED)
                                    )
                                }
                            }

                            INLAY_MARK_HIDDEN -> {
                                ApplicationManager.getApplication().invokeLater {
                                    configuration.inlayRef?.get()?.dispose()
                                    configuration.inlayRef = null
                                }
                            }
                        }
                    })
                } else {
                    ApplicationManager.getApplication().invokeLater {
                        InlayHintsPassFactory.forceHintsUpdateOnNextPass()
                    }
                }
            }
        }
    }

    fun dispose() {
        dispose(true)
    }

    fun dispose(removeFromMarker: Boolean = true) {
        dispose(removeFromMarker, true)
    }

    fun dispose(removeFromMarker: Boolean = true, assertRemoval: Boolean = true) {
        doDispose(removeFromMarker, assertRemoval)

        triggerEvent(SourceMarkEvent(this, SourceMarkEventCode.MARK_REMOVED)) {
            clearEventListeners()
        }
    }

    suspend fun disposeSuspend(removeFromMarker: Boolean = true, assertRemoval: Boolean = true) {
        doDispose(removeFromMarker, assertRemoval)

        triggerEventSuspend(SourceMarkEvent(this, SourceMarkEventCode.MARK_REMOVED))
        clearEventListeners()
    }

    private fun doDispose(removeFromMarker: Boolean, assertRemoval: Boolean) {
        removeMarkFromUserData()

        if (this is InlayMark) {
            configuration.inlayRef?.get()?.let { Disposer.dispose(it) }
            configuration.inlayRef = null
        }
        closePopup()

        if (removeFromMarker) {
            if (assertRemoval) {
                check(sourceFileMarker.removeSourceMark(this, autoRefresh = true, autoDispose = false))
            } else {
                sourceFileMarker.removeSourceMark(this, autoRefresh = true, autoDispose = false)
            }
        }
    }

    private fun removeMarkFromUserData() {
        when (this) {
            is ExpressionSourceMark -> {
                removeMarkFromUserData(psiExpression)
            }

            is MethodSourceMark -> {
                removeMarkFromUserData(ReadAction.compute(ThrowableComputable { getNameIdentifier() }))
            }

            is ClassSourceMark -> {
                removeMarkFromUserData(ReadAction.compute(ThrowableComputable { getNameIdentifier() }))
            }

            else -> error("Unsupported source mark type: $this")
        }
    }

    private fun removeMarkFromUserData(element: PsiElement) {
        element.putUserData(SourceKey.GutterMark, null)
        element.putUserData(SourceKey.InlayMarks, null)
        element.putUserData(SourceKey.GuideMark, null)
    }

    fun getParent(): GuideMark? {
        return artifactQualifiedName.asParent()?.let {
            sourceFileMarker.getSourceMark(it, Type.GUIDE) as GuideMark?
        }
    }

    fun getChildren(): List<SourceMark> {
        return sourceFileMarker.getSourceMarks().filter { it.artifactQualifiedName.isChildOf(artifactQualifiedName) }
    }

    val userData: HashMap<Any, Any>
    fun <T> getUserData(key: SourceKey<T>): T? = userData[key] as T?
    fun <T> putUserData(key: SourceKey<T>, value: T?) {
        if (value != null) {
            userData.put(key, value)
        } else {
            userData.remove(key)
        }

        val event = SourceMarkEvent(this, SourceMarkEventCode.MARK_USER_DATA_UPDATED, key, value)
        triggerEvent(event) {
            val parentEvent = SourceMarkEvent(this, SourceMarkEventCode.CHILD_USER_DATA_UPDATED, key, value)
            propagateEventToParents(parentEvent)
        }
    }

    fun <T> putUserDataIfAbsent(key: SourceKey<T>, value: T?): T? {
        return if (userData.containsKey(key)) {
            userData[key] as T?
        } else {
            putUserData(key, value)
            value
        }
    }

    fun <T> removeUserData(key: SourceKey<T>): T? {
        val cachedValue = getUserData(key)
        putUserData(key, null)
        return cachedValue
    }

    fun hasUserData(): Boolean = userData.isNotEmpty()

    val eventListeners: ArrayList<SourceMarkEventListener>
    fun clearEventListeners() = eventListeners.clear()
    fun getEventListeners(): List<SourceMarkEventListener> = eventListeners.toList()
    fun addEventListener(listener: SourceMarkEventListener) {
        eventListeners += listener
    }

    fun propagateEventToParents(event: SourceMarkEvent) {
        var parent = getParent()
        while (parent != null) {
            parent.triggerEvent(event)
            parent = parent.getParent()
        }
    }

    fun triggerEvent(eventCode: IEventCode, params: List<Any?>, listen: (() -> Unit)? = null) {
        triggerEvent(SourceMarkEvent(this, eventCode, *params.toTypedArray()), listen)
    }

    fun triggerEvent(event: SourceMarkEvent, listen: (() -> Unit)? = null) {
        val eventListeners = getEventListeners()

        //sync listeners
        eventListeners
            .filterIsInstance<SynchronousSourceMarkEventListener>()
            .forEach { it.handleEvent(event) }

        //async listeners
        safeGlobalLaunch {
            eventListeners.forEach {
                if (it !is SynchronousSourceMarkEventListener) {
                    it.handleEvent(event)
                }
            }
            listen?.invoke()
        }
    }

    suspend fun triggerEventSuspend(event: SourceMarkEvent) {
        //sync listeners
        getEventListeners()
            .filterIsInstance<SynchronousSourceMarkEventListener>()
            .forEach { it.handleEvent(event) }

        //async listeners
        safeRunBlocking {
            getEventListeners().forEach {
                if (it !is SynchronousSourceMarkEventListener) {
                    it.handleEvent(event)
                }
            }
        }
    }

    fun closePopup() {
        if (openedMarks.remove(this)) {
            log.trace("Closing popup")
            try {
                if (sourceMarkComponent.configuration.addedMouseMotionListener) {
                    editor?.contentComponent?.removeMouseMotionListener(this)
                    sourceMarkComponent.configuration.addedMouseMotionListener = false
                }
            } catch (ignore: Throwable) {
            }
            try {
                if (sourceMarkComponent.configuration.addedScrollListener) {
                    editor?.scrollingModel?.removeVisibleAreaListener(this)
                    sourceMarkComponent.configuration.addedScrollListener = false
                }
            } catch (ignore: Throwable) {
            }

            editor = null
            if (visiblePopup != null) {
                Disposer.dispose(visiblePopup!!)
            }
            visiblePopup = null
            triggerEvent(SourceMarkEvent(this, SourceMarkEventCode.PORTAL_CLOSED))
        }
    }

    fun displayPopup() {
        displayPopup(FileEditorManager.getInstance(sourceFileMarker.project).selectedTextEditor!!)
    }

    fun displayPopup(editor: Editor = FileEditorManager.getInstance(sourceFileMarker.project).selectedTextEditor!!) {
        if (visiblePopup != null || buildingPopup.getAndSet(true)) {
            log.trace("Ignore display popup")
            return
        } else {
            log.trace("Displaying popup")

            //todo: only close marks which are necessary to close
            closeOpenPopups()
            triggerDisplay(editor)
        }
    }

    fun triggerDisplay(editor: Editor) {
        this.editor = editor

        SwingUtilities.invokeLater {
            val popup: Disposable
            val popupComponent = sourceMarkComponent.getComponent()
            val dynamicSize = sourceMarkComponent.configuration.componentSizeEvaluator
                .getDynamicSize(editor, sourceMarkComponent.configuration)
            if (dynamicSize != null && popupComponent.preferredSize != dynamicSize) {
                popupComponent.preferredSize = dynamicSize
            }
            val popupComponentSize = popupComponent.preferredSize

            val displayPoint = if ((this is ClassSourceMark && sourceMarkComponent.configuration.showAboveClass)
                || (this is MethodSourceMark && sourceMarkComponent.configuration.showAboveMethod)
                || (this is ExpressionSourceMark && sourceMarkComponent.configuration.showAboveExpression)
            ) {
                editor.visualPositionToXY(
                    editor.offsetToVisualPosition(editor.document.getLineStartOffset(lineNumber))
                )
            } else {
                editor.visualPositionToXY(
                    editor.offsetToVisualPosition(
                        editor.document.getLineStartOffset(
                            editor.caretModel.logicalPosition.line
                        )
                    )
                )
            }

            if (sourceMarkComponent.configuration.useHeavyPopup) {
                displayPoint.y -= popupComponentSize.height + 4

                popup = JBPopupFactory.getInstance()
                    .createComponentPopupBuilder(popupComponent, popupComponent)
                    .setShowBorder(false)
                    .setShowShadow(false)
                    .setRequestFocus(true)
                    .setCancelOnWindowDeactivation(false)
                    .createPopup()
                popup.addListener(this)
                popup.show(RelativePoint(editor.contentComponent, displayPoint))
            } else {
                val width = (popupComponentSize.width / 2) + 10
                val height = popupComponentSize.height / 2
                displayPoint.x = (displayPoint.getX() + width).toInt() + 10
                displayPoint.y = (displayPoint.getY() - height).toInt()

                popup = JBPopupFactory.getInstance()
                    .createBalloonBuilder(popupComponent)
                    .setBorderInsets(JBUI.emptyInsets())
                    .setDialogMode(true)
                    .setFillColor(JBColor.background())
                    .setAnimationCycle(0)
                    .createBalloon() as BalloonImpl
                popup.addListener(this)
                popup.setShowPointer(false)
                popup.show(RelativePoint(editor.contentComponent, displayPoint), Balloon.Position.atRight)
            }
            visiblePopup = popup
            openedMarks.add(this)
            triggerEvent(SourceMarkEvent(this, SourceMarkEventCode.PORTAL_OPENED))

            //dispose popup when mouse hovers off popup
            if (sourceMarkComponent.configuration.hideOnMouseMotion) {
                editor.contentComponent.addMouseMotionListener(this)
                sourceMarkComponent.configuration.addedMouseMotionListener = true
            }
            //dispose popup when code has been scrolled
            if (sourceMarkComponent.configuration.hideOnScroll) {
                editor.scrollingModel.addVisibleAreaListener(this)
                sourceMarkComponent.configuration.addedScrollListener = true
            }
        }
    }

    //region Popup Listeners

    @JvmDefault
    override fun beforeShown(event: LightweightWindowEvent) {
        log.trace("Before popup shown")

        //delay prevents component stains when mark is closed and opened quickly
        //todo: open intellij bug
        Timer().schedule(500) {
            buildingPopup.set(false)
        }
    }

    @JvmDefault
    override fun onClosed(event: LightweightWindowEvent) {
        closePopup()
    }

    override fun visibleAreaChanged(e: VisibleAreaEvent) {
        if (buildingPopup.get()) {
            return //todo: piggy backed on above hack; needed for when navigating from different files
        } else if (e.oldRectangle.location == e.newRectangle.location) {
            return //no change in location
        } else if (System.currentTimeMillis() - SourceInlayHintProvider.latestInlayMarkAddedAt <= 200) {
            return //new inlay mark triggered event
        }

        log.debug("Visible area changed")
        closePopup()
    }

    override fun mouseDragged(e2: MouseEvent) {}
    override fun mouseMoved(e2: MouseEvent) {
        //13 pixels on x coordinate puts mouse past gutter
        if (e2.point.getX() > 13) {
            log.debug("Mouse moved outside popup")
            closePopup()
        }
    }

    //endregion
}
