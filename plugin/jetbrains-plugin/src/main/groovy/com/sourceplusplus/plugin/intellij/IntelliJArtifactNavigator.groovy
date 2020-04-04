package com.sourceplusplus.plugin.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.gutter.GutterMark
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJSourceMark
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJGutterMark
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJMethodGutterMark
import com.sourceplusplus.plugin.intellij.portal.IntelliJSourcePortal
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.display.PortalTab
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject

import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.CAN_NAVIGATE_TO_ARTIFACT
import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.NAVIGATE_TO_ARTIFACT

/**
 * todo: description
 *
 * @version 0.2.5
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class IntelliJArtifactNavigator extends AbstractVerticle {

    private String artifactNavigationPendingPopup

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(CAN_NAVIGATE_TO_ARTIFACT.address, { message ->
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
                        } else {
                            IntelliJSourcePortal.register(appUuid, artifactQualifiedName, false)
                        }
                    }
                    message.reply(true)
                } else {
                    message.reply(false)
                }
            })
        })
        vertx.eventBus().consumer(NAVIGATE_TO_ARTIFACT.address, { message ->
            def request = message.body() as JsonObject
            def portal = IntelliJSourcePortal.getPortal(request.getString("portal_uuid"))
            def parentStackNavigation = request.getBoolean("parent_stack_navigation", false)
            if (portal.portalUI.tracesView.innerTrace && parentStackNavigation) {
                def mark = SourceMarkerPlugin.INSTANCE.getSourceMark(
                        portal.portalUI.viewingPortalArtifact) as IntelliJGutterMark
                if (mark.configuration.icon == IntelliJGutterMark.arrowToLeft) {
                    ApplicationManager.getApplication().invokeLater({
                        mark.dispose()
                    })
                }
            }

            def artifactQualifiedName = request.getString("artifact_qualified_name")
            ApplicationManager.getApplication().invokeLater({
                GutterMark.closeOpenPopups()

                artifactNavigationPendingPopup = artifactQualifiedName
                SourceMarkerPlugin.INSTANCE.artifactNavigator.navigateToMethod(
                        IntelliJStartupActivity.currentProject, artifactQualifiedName)

                def sourceMark = SourceMarkerPlugin.INSTANCE.getSourceMark(
                        artifactQualifiedName) as IntelliJMethodGutterMark
                if (sourceMark != null && sourceMark.portalRegistered) {
                    handleMark(sourceMark)
                }
            })
        })
        vertx.eventBus().consumer(IntelliJSourceMark.SOURCE_MARK_APPLIED, {
            def sourceMark = it.body() as SourceMark
            if (sourceMark.artifactQualifiedName == artifactNavigationPendingPopup) {
                def gutterMark = sourceMark as IntelliJMethodGutterMark
                gutterMark.registerPortal(PortalTab.Traces)
                handleMark(gutterMark)
            }
        })
        log.info("{} started", getClass().getSimpleName())
    }

    private void handleMark(IntelliJMethodGutterMark mark) {
        artifactNavigationPendingPopup = null
        def portal = SourcePortal.getPortal(mark.portalUuid)
        portal.portalUI.loadPage(PortalTab.Traces, ["order_type": portal.portalUI.tracesView.orderType.toString()])

        if (portal.portalUI.tracesView.innerTrace) {
            mark.configuration.icon = IntelliJGutterMark.arrowToLeft
            mark.sourceFileMarker.refresh()
        }
        ApplicationManager.getApplication().invokeLater({
            ApplicationManager.getApplication().runReadAction({
                def editor = FileEditorManager.getInstance(IntelliJStartupActivity.currentProject).getSelectedTextEditor()
                mark.displayPopup(editor)
            })
        })
    }
}
