package spp.jetbrains.marker.source.mark.api

import com.intellij.codeInsight.hints.InlayHintsPassFactory
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
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
import com.intellij.psi.PsiElement
import com.intellij.ui.BalloonImpl
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.plugin.SourceInlayComponentProvider
import spp.jetbrains.marker.plugin.SourceInlayHintProvider
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponent
import spp.jetbrains.marker.source.mark.api.config.SourceMarkConfiguration
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.api.event.SynchronousSourceMarkEventListener
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.marker.source.mark.gutter.event.GutterMarkEventCode
import spp.jetbrains.marker.source.mark.inlay.ExpressionInlayMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.marker.source.mark.inlay.event.InlayMarkEventCode.INLAY_MARK_HIDDEN
import spp.jetbrains.marker.source.mark.inlay.event.InlayMarkEventCode.INLAY_MARK_VISIBLE
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

    val id: String
    val type: Type
    val isClassMark: Boolean
    val isMethodMark: Boolean
    val isExpressionMark: Boolean
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
    val language: Language
        get() = getPsiElement().language

    fun getPsiElement(): PsiElement

    fun isVisible(): Boolean
    fun setVisible(visible: Boolean)

    fun canApply(): Boolean = configuration.applySourceMarkFilter.test(this)
    fun apply(sourceMarkComponent: SourceMarkComponent, addToMarker: Boolean = true)
    fun apply(addToMarker: Boolean = true) {
        SourceMarker.getGlobalSourceMarkEventListeners().forEach(::addEventListener)

        if (addToMarker) {
            check(sourceFileMarker.applySourceMark(this, autoRefresh = true, overrideFilter = true))
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
                    val editor = FileEditorManager.getInstance(project).selectedTextEditor
                    if (editor == null) {
                        TODO()
                    } else {
                        val provider = SourceInlayComponentProvider.from(editor)
                        val viewport = (editor as? EditorImpl)?.scrollPane?.viewport!!
                        var displayLineIndex = lineNumber - 1
                        if (this is ExpressionInlayMark) {
                            if (showAboveExpression) {
                                displayLineIndex--
                            }
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
                    }
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
        if (this is InlayMark) {
            configuration.inlayRef?.get()?.dispose()
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
        triggerEvent(SourceMarkEvent(this, SourceMarkEventCode.MARK_REMOVED)) {
            clearEventListeners()
        }
    }

    fun <T> getUserData(key: SourceKey<T>): T?
    fun <T> putUserData(key: SourceKey<T>, value: T?)
    fun hasUserData(): Boolean

    fun clearEventListeners()
    fun getEventListeners(): List<SourceMarkEventListener>
    fun addEventListener(listener: SourceMarkEventListener)
    fun triggerEvent(event: SourceMarkEvent, listen: (() -> Unit)? = null) {
        //sync listeners
        getEventListeners()
            .filterIsInstance<SynchronousSourceMarkEventListener>()
            .forEach { it.handleEvent(event) }

        //async listeners
        GlobalScope.launch {
            getEventListeners().forEach {
                if (it !is SynchronousSourceMarkEventListener) {
                    it.handleEvent(event)
                }
            }
            listen?.invoke()
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
