package com.sourceplusplus.plugin.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJGutterMark
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJMethodGutterMark
import com.sourceplusplus.plugin.intellij.portal.IntelliJSourcePortal
import com.sourceplusplus.portal.display.PortalTab
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import plus.sourceplus.marker.plugin.SourceMarkerPlugin
import plus.sourceplus.marker.source.mark.gutter.GutterMark

/**
 * todo: description
 *
 * @version 0.2.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class IntelliJArtifactNavigator extends AbstractVerticle {

    public static final String CAN_NAVIGATE_TO_ARTIFACT = "CanNavigateToArtifact"
    public static final String NAVIGATE_TO_ARTIFACT = "NavigateToArtifact"

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(CAN_NAVIGATE_TO_ARTIFACT, { message ->
            def request = message.body() as JsonObject
            def appUuid = request.getString("app_uuid")
            def artifactQualifiedName = request.getString("artifact_qualified_name")
            ApplicationManager.getApplication().invokeLater({
                if (SourceMarkerPlugin.INSTANCE.artifactNavigator.canNavigateToMethod(
                        IntelliJStartupActivity.currentProject, artifactQualifiedName)) {
                    def internalPortal = IntelliJSourcePortal.getInternalPortal(appUuid, artifactQualifiedName)
                    if (!internalPortal.isPresent()) {
                        def sourceMark = SourceMarkerPlugin.INSTANCE.getSourceMark(
                                artifactQualifiedName) as IntelliJGutterMark
                        if (sourceMark) {
                            sourceMark.registerPortal()
                            message.reply(true)
                        } else {
                            message.reply(false)
                        }
                    } else {
                        message.reply(true)
                    }
                } else {
                    message.reply(false)
                }
            })
        })
        vertx.eventBus().consumer(NAVIGATE_TO_ARTIFACT, { message ->
            def request = message.body() as JsonObject
            def portal = IntelliJSourcePortal.getPortal(request.getString("portal_uuid"))
            def artifactQualifiedName = request.getString("artifact_qualified_name")
            ApplicationManager.getApplication().invokeLater({
                GutterMark.closeOpenPortals()

                //todo: don't think the params are necessary
                portal.interface.loadPage(PortalTab.Traces, ["order_type": portal.interface.tracesView.orderType.toString()])
                SourceMarkerPlugin.INSTANCE.artifactNavigator.navigateToMethod(
                        IntelliJStartupActivity.currentProject, artifactQualifiedName)

                def sourceMark = SourceMarkerPlugin.INSTANCE.getSourceMark(
                        artifactQualifiedName) as IntelliJMethodGutterMark
                if (sourceMark != null) {
                    handleMark(sourceMark)
                } else {
                    //todo: smarter
                    vertx.setPeriodic(1000, {
                        sourceMark = SourceMarkerPlugin.INSTANCE.getSourceMark(
                                artifactQualifiedName) as IntelliJMethodGutterMark
                        if (sourceMark != null) {
                            vertx.cancelTimer(it)
                            message.reply(true)
                            handleMark(sourceMark)
                        }
                    })
                }
            })
        })
        log.info("{} started", getClass().getSimpleName())
    }

    private static void handleMark(IntelliJMethodGutterMark mark) {
        mark.markArtifactDataAvailable()

        ApplicationManager.getApplication().invokeLater({
            ApplicationManager.getApplication().runReadAction({
                def editor = FileEditorManager.getInstance(IntelliJStartupActivity.currentProject).getSelectedTextEditor()
                mark.displayPortal(editor)
            })
        })
    }
}
