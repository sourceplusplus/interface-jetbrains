package com.sourceplusplus.plugin.coordinate.artifact.track

import com.sourceplusplus.plugin.marker.SourceFileMarker
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle

import static com.sourceplusplus.plugin.PluginBootstrap.getSourcePlugin

/**
 * todo: description
 *
 * @version 0.2.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class FileClosedTracker extends AbstractVerticle {

    public static final String ADDRESS = "SourceFileClosed"

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(ADDRESS, { messageHandler ->
            def closedFileMarker = (SourceFileMarker) messageHandler.body()

            log.debug("Closed file: " + closedFileMarker.sourceFile)
            sourcePlugin.deactivateSourceFileMarker(closedFileMarker)
        })
        log.info("{} started", getClass().getSimpleName())
    }
}
