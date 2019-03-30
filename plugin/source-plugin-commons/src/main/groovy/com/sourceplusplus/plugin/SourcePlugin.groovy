package com.sourceplusplus.plugin

import com.google.common.collect.Sets
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.plugin.marker.SourceFileMarker
import com.sourceplusplus.plugin.marker.mark.SourceMark
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * todo: description
 *
 * @version 0.1.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class SourcePlugin {

    public static final String SOURCE_FILE_MARKER_ACTIVATED = "SourceFileMarkerActivated"

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private final Set<SourceFileMarker> availableSourceFileMarkers = Sets.newConcurrentHashSet()
    private final SourceCoreClient coreClient
    private final Vertx vertx

    SourcePlugin(SourceCoreClient coreClient) {
        this.coreClient = Objects.requireNonNull(coreClient)
        vertx = Vertx.vertx()
        System.addShutdownHook {
            vertx.close()
        }
        vertx.deployVerticle(new PluginBootstrap(this))
    }

    void startTooltipUIBridge(Handler<AsyncResult<HttpServer>> listenHandler) {
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx)
        BridgeOptions tooltipBridgeOptions = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddressRegex(".+"))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex(".+"))
        sockJSHandler.bridge(tooltipBridgeOptions)

        Router router = Router.router(vertx)
        router.route("/eventbus/*").handler(sockJSHandler)
        vertx.createHttpServer().requestHandler(router.&accept).listen(0, listenHandler)
    }

    void refreshActiveSourceFileMarkers() {
        availableSourceFileMarkers.each {
            it.refresh()
        }
    }

    void activateSourceFileMarker(SourceFileMarker sourceFileMarker) {
        if (availableSourceFileMarkers.add(Objects.requireNonNull(sourceFileMarker))) {
            def sourceMarks = sourceFileMarker.createSourceMarks()
            sourceFileMarker.setSourceMarks(sourceMarks)
            sourceFileMarker.refresh()
            log.info("Activated source file marker: {} - Mark count: {}", sourceFileMarker, sourceMarks.size())
            vertx.eventBus().publish(SOURCE_FILE_MARKER_ACTIVATED, sourceFileMarker.sourceFile.qualifiedClassName)
        }
    }

    void deactivateSourceFileMarker(SourceFileMarker sourceFileMarker) {
        if (availableSourceFileMarkers.remove(Objects.requireNonNull(sourceFileMarker))) {
            def sourceMarks = sourceFileMarker.getSourceMarks()

            log.info("Deactivated source file marker: {} - Mark count: {}", sourceFileMarker, sourceMarks.size())
            sourceMarks.each {
                sourceFileMarker.removeSourceMark(it)
                log.trace("Removed source mark: {}", it)
            }
            sourceFileMarker.refresh()
        }
    }

    SourceCoreClient getCoreClient() {
        return coreClient
    }

    @Nullable
    SourceFileMarker getSourceFileMarker(String qualifiedClassName) {
        return availableSourceFileMarkers.find {
            it.sourceFile.qualifiedClassName == qualifiedClassName
        }
    }

    @Nullable
    SourceFileMarker getSourceFileMarker(PluginSourceFile sourceFile) {
        return availableSourceFileMarkers.find {
            it.sourceFile == sourceFile
        }
    }

    @NotNull
    Set<SourceFileMarker> getAvailableSourceFileMarkers() {
        return Sets.newHashSet(availableSourceFileMarkers)
    }

    @NotNull
    Vertx getVertx() {
        return vertx
    }

    @Nullable
    SourceMark getSourceMark(String artifactQualifiedName) {
        def sourceMark = null
        availableSourceFileMarkers.each {
            if (sourceMark == null) {
                sourceMark = it.getSourceMark(artifactQualifiedName)
            }
        }
        return sourceMark
    }
}
