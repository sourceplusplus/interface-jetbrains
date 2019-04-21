package com.sourceplusplus.plugin.intellij.marker.mark

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiLiteral
import com.intellij.ui.BalloonImpl
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.plugin.PluginSourceFile
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.render.SourceArtifactGutterMarkRenderer
import com.sourceplusplus.plugin.marker.SourceFileMarker
import com.sourceplusplus.plugin.marker.mark.GutterMark
import com.sourceplusplus.plugin.source.model.SourceMethodAnnotation
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.coordinate.track.PortalViewTracker
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.jetbrains.annotations.NotNull
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.java.JavaUAnnotation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.util.List
import java.util.concurrent.atomic.AtomicBoolean

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJMethodGutterMark extends GutterMark {

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private static final AtomicBoolean buildingPortalUI = new AtomicBoolean(false)
    private static PortalMouseMotionListener mouseMotionListener
    private static Balloon currentShowingBalloon
    private static IntelliJMethodGutterMark currentShowingMark
    private static int scrollPosition
    private final PluginSourceFile sourceFile
    private final SourceArtifact sourceMethod
    private UMethod psiMethod
    private final SourceArtifactGutterMarkRenderer gutterMarkRenderer

    IntelliJMethodGutterMark(SourceFileMarker sourceFileMarker, SourceArtifact sourceMethod, UMethod psiMethod) {
        super(sourceFileMarker)
        this.sourceFile = sourceFileMarker.sourceFile
        this.sourceMethod = sourceMethod
        this.psiMethod = psiMethod
        this.gutterMarkRenderer = new SourceArtifactGutterMarkRenderer(this)
    }

    static void closePortalIfOpen() {
        if (currentShowingBalloon != null) {
            currentShowingBalloon.hide()
            currentShowingBalloon = null
        }
        if (currentShowingMark != null) {
            currentShowingMark.showingPortalWindow.set(false)
            currentShowingMark = null
        }
    }

    void displayPortal(final Vertx vertx, final Editor editor, boolean hideOnMouseMotion) {
        if (showingPortalWindow.get()) {
            return
        } else if (buildingPortalUI.getAndSet(true)) {
            return
        }

        final int portalId = portalId
        vertx.eventBus().send(PortalViewTracker.UPDATE_PORTAL_ARTIFACT,
                new JsonObject().put("portal_id", portalId)
                        .put("artifact_qualified_name", getArtifactQualifiedName())
        )
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            void run() {
                closePortalIfOpen()

                JBPopupFactory popupFactory = JBPopupFactory.getInstance()
                BalloonImpl balloon = popupFactory
                        .createBalloonBuilder(SourcePortal.getPortal(portalId).portalUI.UIComponent)
                        .setBorderInsets(JBUI.emptyInsets())
                        .setDialogMode(true)
                        .setFillColor(JBColor.background())
                        .setAnimationCycle(0)
                        .createBalloon() as BalloonImpl
                Disposer.register(editor.project, balloon)
                balloon.addListener(new PortalPopupListener(vertx, portalId))

                Point portalPoint = editor.visualPositionToXY(editor.offsetToVisualPosition(
                        editor.document.getLineStartOffset(lineNumber)))
                portalPoint.x = portalPoint.x + 380 as int
                portalPoint.y = portalPoint.y - 125 as int

                showingPortalWindow.set(true)
                buildingPortalUI.getAndSet(false)

                balloon.setShowPointer(false)
                balloon.show(new RelativePoint(editor.contentComponent, portalPoint),
                        Balloon.Position.atRight)
                currentShowingBalloon = balloon
                currentShowingMark = IntelliJMethodGutterMark.this
                scrollPosition = editor.scrollingModel.verticalScrollOffset

                //dispose popup when mouse hovers off portal
                if (hideOnMouseMotion) {
                    editor.contentComponent.addMouseMotionListener(
                            mouseMotionListener = new PortalMouseMotionListener(portalId))
                }

                if (hideOnMouseMotion) {
                    //dispose popup when code has been scrolled
                    editor.scrollingModel.addVisibleAreaListener(new PortalVisibleAreaListener(portalId))
                } else {
                    //todo: smarter; it thinks it was scrolled after jumping to method; added delay :/
                    vertx.setTimer(1000, {
                        editor.scrollingModel.addVisibleAreaListener(new PortalVisibleAreaListener(portalId))
                    })
                }
            }
        })
    }

    boolean isViewable() {
        try {
            psiMethod.getContainingFile().getViewProvider().getDocument()
            return true
        } catch (PsiInvalidElementAccessException ex) {
            return false
        }
    }

    /**
     * Line number of the gutter mark.
     * One above the method name identifier.
     * First line for class (maybe? might want to make that for package level stats in the future)
     *
     * @return gutter mark line number
     */
    @Override
    int getLineNumber() {
        def document = psiMethod.nameIdentifier.getContainingFile().getViewProvider().getDocument()
        def num = document.getLineNumber(psiMethod.nameIdentifier.textRange.startOffset)
        if (log.traceEnabled) {
            log.trace("Source mark: $artifactQualifiedName is at line: $num.")
        }
        return num
    }

    UMethod getPsiMethod() {
        return psiMethod
    }

//    UMethod setPsiMethod(UMethod psiMethod) {
//        this.psiMethod = psiMethod
//    }

    @NotNull
    SourceArtifactGutterMarkRenderer getGutterMarkRenderer() {
        return gutterMarkRenderer
    }

    @Override
    PluginSourceFile getSourceFile() {
        return sourceFile
    }

    @Override
    SourceArtifact getSourceMethod() {
        return sourceMethod
    }

    @Override
    boolean isClassMark() {
        return false
    }

    @Override
    boolean isMethodMark() {
        return true
    }

    @Override
    void getMethodAnnotations(Handler<AsyncResult<List<SourceMethodAnnotation>>> handler) {
        ReadAction.run({
            def annotations = new ArrayList<SourceMethodAnnotation>()
            psiMethod.getAnnotations().each {
                def qualifiedName = it.qualifiedName
                if (qualifiedName) {
                    if (it instanceof JavaUAnnotation) {
                        def attributeMap = new HashMap<String, Object>()
                        it.attributeValues.each {
                            if (it.name) {
                                attributeMap.put(it.name, (it.expression as ULiteralExpression).value)
                            } else {
                                log.warn("Unknown annotation expression: " + it)
                            }
                        }
                        annotations.add(new SourceMethodAnnotation(qualifiedName, attributeMap))
                    } else {
                        def attributeMap = new HashMap<String, Object>()
                        it.attributes.each {
                            if (it.attributeValue.sourceElement instanceof PsiLiteral) {
                                attributeMap.put(it.attributeName, (it.attributeValue.sourceElement as PsiLiteral).value)
                            } else {
                                attributeMap.put(it.attributeName, it.attributeValue.sourceElement.toString())
                            }
                        }
                        annotations.add(new SourceMethodAnnotation(qualifiedName, attributeMap))
                    }
                }
            }
            handler.handle(Future.succeededFuture(annotations))
        })
    }

    @Override
    String getModuleName() {
        return ProjectRootManager.getInstance(psiMethod.getProject()).getFileIndex()
                .getModuleForFile(psiMethod.getContainingFile().getVirtualFile()).name
    }

    private class PortalPopupListener implements JBPopupListener {

        private final Vertx vertx
        private final int portalId

        PortalPopupListener(Vertx vertx, int portalId) {
            this.vertx = Objects.requireNonNull(vertx)
            this.portalId = portalId
        }

        @Override
        void beforeShown(LightweightWindowEvent event1) {
            if (IntelliJMethodGutterMark.this.portalId == portalId) {
                vertx.eventBus().publish(PortalViewTracker.OPENED_PORTAL,
                        new JsonObject().put("portal_id", portalId).put("artifact_qualified_name",
                                IntelliJMethodGutterMark.this.sourceMethod.artifactQualifiedName())
                )
            }
        }

        @Override
        void onClosed(LightweightWindowEvent event1) {
            if (IntelliJMethodGutterMark.this.portalId == portalId) {
                vertx.eventBus().publish(PortalViewTracker.CLOSED_PORTAL,
                        new JsonObject().put("portal_id", portalId).put("artifact_qualified_name",
                                IntelliJMethodGutterMark.this.sourceMethod.artifactQualifiedName())
                )
                IntelliJMethodGutterMark.this.showingPortalWindow.set(false)
                currentShowingBalloon = null
            }
        }
    }

    private class PortalVisibleAreaListener implements VisibleAreaListener {

        private final int portalId

        PortalVisibleAreaListener(int portalId) {
            this.portalId = portalId
        }

        @Override
        void visibleAreaChanged(VisibleAreaEvent e) {
            if (currentShowingBalloon != null && IntelliJMethodGutterMark.this.portalId == portalId) {
                currentShowingBalloon.hide()
                currentShowingBalloon = null
            }
        }
    }

    private class PortalMouseMotionListener implements MouseMotionListener {

        private final int portalId

        PortalMouseMotionListener(int portalId) {
            this.portalId = portalId
        }

        @Override
        void mouseDragged(MouseEvent e2) {
        }

        @Override
        void mouseMoved(MouseEvent e2) {
            //13 pixels on x coord puts mouse past gutter
            if (currentShowingBalloon != null && e2.point.x > 13 && IntelliJMethodGutterMark.this.portalId == portalId) {
                currentShowingBalloon.hide()
                currentShowingBalloon = null
            }
        }
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        if (!super.equals(o)) return false
        IntelliJMethodGutterMark that = (IntelliJMethodGutterMark) o
        if (psiMethod != that.psiMethod) return false
        return true
    }

    int hashCode() {
        int result = super.hashCode()
        result = 31 * result + (psiMethod != null ? psiMethod.hashCode() : 0)
        return result
    }
}
