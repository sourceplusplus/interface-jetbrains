package com.sourceplusplus.core

import groovy.util.logging.Slf4j
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import org.apache.commons.io.IOUtils

import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Main entry point to launch core server.
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class CoreBootstrap {

    public static final ResourceBundle BUILD = ResourceBundle.getBundle("source-core_build")

    static void main(String[] args) {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")

        def serverConfig
        if (args.length > 0) {
            log.info("Using configuration passed via program arguments")
            serverConfig = new JsonObject(args[0])
        } else {
            def configFile = System.getenv("SOURCE_CONFIG")
            if (!configFile) {
                log.warn("Missing SOURCE_CONFIG environment variable. Using default settings")
                configFile = "local-core.json"
            }

            log.info("Using configuration file: $configFile")
            def configInputStream
            if (new File(configFile).exists()) {
                configInputStream = new File(configFile).newInputStream()
            } else {
                configInputStream = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("config/$configFile")
            }
            def configData = IOUtils.toString(configInputStream, StandardCharsets.UTF_8)
            serverConfig = new JsonObject(configData)
        }

        def buildDate = BUILD.getString("build_date")
        log.info("Build: " + buildDate)

        def version = BUILD.getString("version")
        if (version == "dev") {
            //allow debug pauses
            def vertxOptions = new VertxOptions()
            vertxOptions.setBlockedThreadCheckInterval(Integer.MAX_VALUE)

            Vertx.vertx(vertxOptions).deployVerticle(new SourceCoreServer(serverConfig, version, null))
        } else {
            Vertx.vertx().deployVerticle(new SourceCoreServer(serverConfig, version, Instant.parse(buildDate)))
        }
    }
}
