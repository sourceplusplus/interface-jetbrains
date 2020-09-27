package com.sourceplusplus.marker.source.mark.api

import com.intellij.codeInsight.hints.InlayHintsPassFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.ui.BalloonImpl
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import com.sourceplusplus.marker.plugin.SourceInlayProvider
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.component.api.SourceMarkComponent
import com.sourceplusplus.marker.source.mark.api.config.SourceMarkConfiguration
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.marker.source.mark.gutter.GutterMark
import com.sourceplusplus.marker.source.mark.inlay.InlayMark
import org.slf4j.LoggerFactory
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceMark : JBPopupListener, MouseMotionListener, VisibleAreaListener {

    /**
     * todo: description.
     */
    enum class Type {
        GUTTER,
        INLAY
    }

    companion object {
        private val log = LoggerFactory.getLogger(SourceMark::class.java)
        private val buildingPopup = AtomicBoolean()
        private var openedMarks: MutableList<SourceMark> = ArrayList()

        @JvmStatic
        fun closeOpenPopups() {
            openedMarks.toList().forEach {
                it.closePopup()
            }
        }
    }

    val type: Type
    val isClassMark: Boolean
    val isMethodMark: Boolean
    val moduleName: String
    val artifactQualifiedName: String
    val sourceFileMarker: SourceFileMarker
    val valid: Boolean
    val lineNumber: Int
    var editor: Editor?
    val viewProviderBound: Boolean
    var visiblePopup: Disposable?
    val configuration: SourceMarkConfiguration
    var sourceMarkComponent: SourceMarkComponent
    val project: Project; get() = sourceFileMarker.project
    fun getPsiElement(): PsiElement

    fun canApply(): Boolean = configuration.applySourceMarkFilter.test(this)
    fun apply(sourceMarkComponent: SourceMarkComponent, addToMarker: Boolean = true)
    fun apply(addToMarker: Boolean = true) {
        SourceMarkerPlugin.getGlobalSourceMarkEventListeners().forEach(::addEventListener)

        if (addToMarker) {
            check(sourceFileMarker.applySourceMark(this, autoRefresh = true, overrideFilter = true))
            triggerEvent(SourceMarkEvent(this, SourceMarkEventCode.MARK_ADDED))

            if (this is GutterMark) {
                if (configuration.icon != null) {
                    setVisible(true)
                } else {
                    setVisible(false)
                }
            } else if (this is InlayMark) {
                ApplicationManager.getApplication().invokeLater {
                    InlayHintsPassFactory.forceHintsUpdateOnNextPass()
                }
            }
        }
    }

    fun dispose() {
        dispose(true)
    }

    fun dispose(removeFromMarker: Boolean = true) {
        closePopup()

        if (removeFromMarker) {
            check(sourceFileMarker.removeSourceMark(this, autoRefresh = true, autoDispose = false))
        }
        triggerEvent(SourceMarkEvent(this, SourceMarkEventCode.MARK_REMOVED))
        clearEventListeners()
    }

    fun <T> getUserData(key: SourceKey<T>): T?
    fun <T> putUserData(key: SourceKey<T>, value: T?)

    fun clearEventListeners()
    fun getEventListeners(): List<SourceMarkEventListener>
    fun addEventListener(listener: SourceMarkEventListener)
    fun triggerEvent(event: SourceMarkEvent) {
        getEventListeners().forEach { it.handleEvent(event) }
    }

    fun closePopup() {
        if (openedMarks.remove(this)) {
            log.debug("Closing popup")
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
            log.debug("Displaying popup")

            //todo: only close marks which are necessary to close
            closeOpenPopups()
            triggerDisplay(editor)
        }
    }

    fun triggerDisplay(editor: Editor) {
        this.editor = editor

        SwingUtilities.invokeLater {
            val displayPoint = editor.visualPositionToXY(
                editor.offsetToVisualPosition(editor.document.getLineStartOffset(lineNumber))
            )

            val popup: Disposable
            val popupComponent = sourceMarkComponent.getComponent()
            val dynamicSize = sourceMarkComponent.configuration.componentSizeEvaluator
                .getDynamicSize(editor, sourceMarkComponent.configuration)
            if (dynamicSize != null && popupComponent.preferredSize != dynamicSize) {
                popupComponent.preferredSize = dynamicSize
            }
            val popupComponentSize = popupComponent.preferredSize

            if (sourceMarkComponent.configuration.useHeavyPopup) {
                if ((this is ClassSourceMark && sourceMarkComponent.configuration.showAboveClass)
                    || (this is MethodSourceMark && sourceMarkComponent.configuration.showAboveMethod)
                    || (this is ExpressionSourceMark && sourceMarkComponent.configuration.showAboveExpression)
                ) {
                    displayPoint.y -= popupComponentSize.height + 4
                }

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
                displayPoint.x = (displayPoint.getX() + width).toInt()
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
        log.debug("Before popup shown")

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
        } else if (System.currentTimeMillis() - SourceInlayProvider.latestInlayMarkAddedAt <= 200) {
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
