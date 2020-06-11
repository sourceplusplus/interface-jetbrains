package com.sourceplusplus.plugin

import com.intellij.psi.PsiFile
import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.marker.SourceFileMarker
import com.sourceplusplus.marker.SourceFileMarkerProvider
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.source.mark.gutter.component.jcef.GutterMarkJcefComponentProvider
import com.sourceplusplus.plugin.coordinate.PluginCoordinator
import com.sourceplusplus.plugin.intellij.marker.IntelliJSourceFileMarker
import com.sourceplusplus.portal.PortalBootstrap
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import org.jetbrains.annotations.NotNull

import java.awt.Dimension

/**
 * Used to bootstrap the Source++ Plugin.
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PluginBootstrap extends AbstractVerticle {

    private static SourcePlugin sourcePlugin

    PluginBootstrap(SourcePlugin sourcePlugin) {
        PluginBootstrap.sourcePlugin = sourcePlugin

        //setup SourceMarker
        SourceMarkerPlugin.INSTANCE.enabled = true
        SourceMarkerPlugin.configuration.defaultGutterMarkConfiguration.activateOnMouseHover = false
        SourceMarkerPlugin.configuration.defaultGutterMarkConfiguration.activateOnMouseClick = true
        SourceMarkerPlugin.configuration.defaultGutterMarkConfiguration.activateOnKeyboardShortcut = true
        SourceMarkerPlugin.configuration.defaultGutterMarkConfiguration.componentProvider = new GutterMarkJcefComponentProvider()
        SourceMarkerPlugin.configuration.defaultGutterMarkConfiguration.componentProvider.with {
            defaultConfiguration.preloadJcefBrowser = false
            defaultConfiguration.autoDisposeBrowser = false //todo: should be able to dispose, see IntelliJPortalUI.close()
            defaultConfiguration.setComponentSize(new Dimension(775, 250))
            //todo: measure size of editor and make short if it will extend past IDE
            //defaultConfiguration.setComponentSize(new Dimension(620, 250))
        }
        SourceMarkerPlugin.configuration.sourceFileMarkerProvider = new SourceFileMarkerProvider() {
            @Override
            SourceFileMarker createSourceFileMarker(@NotNull PsiFile psiFile) {
                log.debug("Creating source file marker for file: " + psiFile)
                return new IntelliJSourceFileMarker(psiFile)
            }
        }
    }

    @Override
    void start() throws Exception {
        log.info("Source++ Plugin activated. App UUID: " + SourcePluginConfig.current.activeEnvironment.appUuid)
        SourceMessage.registerCodecs(vertx)
        vertx.deployVerticle(new PluginCoordinator())
        vertx.deployVerticle(new PortalBootstrap(true))
    }

    @Override
    void stop() throws Exception {
        log.info("{} stopped", getClass().getSimpleName())
    }

    static SourcePlugin getSourcePlugin() {
        return sourcePlugin
    }
}
