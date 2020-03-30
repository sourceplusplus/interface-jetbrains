package com.sourceplusplus.plugin.coordinate

import com.codahale.metrics.MetricRegistry
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.coordinate.artifact.config.SkywalkingTraceConfigIntegrator
import com.sourceplusplus.plugin.coordinate.artifact.config.SpringMVCArtifactConfigIntegrator
import com.sourceplusplus.plugin.coordinate.artifact.track.ArtifactConfigTracker
import com.sourceplusplus.plugin.coordinate.artifact.track.ArtifactSignatureChangeTracker
import com.sourceplusplus.plugin.coordinate.artifact.track.PluginArtifactSubscriptionTracker
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Promise
import io.vertx.core.net.NetServer
import io.vertx.ext.bridge.BridgeOptions
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.eventbus.bridge.tcp.TcpEventBusBridge

/**
 * Used to coordinate all the different events that Source++ Plugin subscribes to.
 *
 * Currently subscribes to the following events:
 *  - user opens/closes file
 *  - user changes source code artifact's signature
 *  - user viewing source code artifact
 *  - source code artifact configuration changes
 *  - source code artifact subscription changes
 *
 * @version 0.2.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PluginCoordinator extends AbstractVerticle {

    public final static MetricRegistry pluginMetrics = new MetricRegistry()

    @Override
    void start(Promise<Void> startFuture) throws Exception {
        //bridge agent to plugin
        log.info("Booting agent event bus bridge")
        def eventBusBridge = TcpEventBusBridge.create(vertx, new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddressRegex(".+"))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex(".+")))
        eventBusBridge.listen(0, {
            if (it.failed()) {
                startFuture.fail(it.cause())
            } else {
                log.info("PluginCoordinator started")

                def serverStr = "server"
                def netServer = it.result()."$serverStr" as NetServer
                SourcePluginConfig.current.remoteAgentPort = netServer.actualPort()
                log.debug("Using remote agent port: " + SourcePluginConfig.current.remoteAgentPort)
                startFuture.complete()
            }
        })

        //config
        vertx.deployVerticle(new SpringMVCArtifactConfigIntegrator(), new DeploymentOptions().setWorker(true))
        vertx.deployVerticle(new SkywalkingTraceConfigIntegrator(), new DeploymentOptions().setWorker(true))

        //track
        vertx.deployVerticle(new ArtifactConfigTracker(), new DeploymentOptions().setWorker(true))
        vertx.deployVerticle(new ArtifactSignatureChangeTracker(), new DeploymentOptions().setWorker(true))
        vertx.deployVerticle(new PluginArtifactSubscriptionTracker(), new DeploymentOptions().setWorker(true))
    }
}
