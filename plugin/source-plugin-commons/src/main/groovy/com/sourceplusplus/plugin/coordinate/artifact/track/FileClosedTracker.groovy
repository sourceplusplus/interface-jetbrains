package com.sourceplusplus.plugin.coordinate.artifact.track

import com.sourceplusplus.plugin.marker.SourceFileMarker
import io.vertx.core.AbstractVerticle
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.sourceplusplus.plugin.PluginBootstrap.getSourcePlugin

/**
 * todo: description
 *
 * @version 0.1.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class FileClosedTracker extends AbstractVerticle {

    public static final String ADDRESS = "SourceFileClosed"

    private static final Logger log = LoggerFactory.getLogger(this.name)

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
