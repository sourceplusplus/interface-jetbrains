package com.sourceplusplus.plugin.coordinate.artifact.status

import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJSourceMark
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle

import java.util.concurrent.TimeUnit

/**
 * Periodically fetches failing source artifacts from Source++ Core.
 *
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PluginFailingArtifactStatus extends AbstractVerticle {

    @Override
    void start() throws Exception {
        //sync status periodically
        vertx.setPeriodic(TimeUnit.MINUTES.toMillis(5), {
            refreshActivelyFailingArtifact()
        })
        refreshActivelyFailingArtifact()
    }

    private static void refreshActivelyFailingArtifact() {
        if (SourcePluginConfig.current.activeEnvironment?.appUuid) {
            SourcePluginConfig.current.activeEnvironment.coreClient.getFailingArtifacts(
                    SourcePluginConfig.current.activeEnvironment.appUuid, {
                if (it.succeeded()) {
                    it.result().each {
                        def sourceMark = SourceMarkerPlugin.INSTANCE.getSourceMark(
                                it.artifactQualifiedName()) as IntelliJSourceMark
                        sourceMark?.updateSourceArtifact(it)
                    }
                    log.debug("Refreshed actively failing artifacts")
                } else {
                    log.error("Failed to refresh actively failing artifacts", it.cause())
                }
            })
        }
    }
}
