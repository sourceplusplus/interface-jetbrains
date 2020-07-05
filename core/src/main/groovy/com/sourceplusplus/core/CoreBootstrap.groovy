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
 * @version 0.3.1
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

        def buildDate = BUILD.getString("build_date") ? Instant.parse(BUILD.getString("build_date")) : null
        log.info("Build: {}", buildDate)

        def vertxOptions = new VertxOptions()
        def version = BUILD.getString("version")
        if (version == "dev") {
            vertxOptions.setBlockedThreadCheckInterval(Integer.MAX_VALUE) //allow debug pauses
        }
        log.info("Version: {}", version)

        def vertx = Vertx.vertx(vertxOptions)
        vertx.deployVerticle(new SourceCoreServer(serverConfig, version, buildDate), {
            if (it.failed()) {
                it.cause().printStackTrace()
                vertx.close({
                    System.exit(-1)
                })
            }
        })
    }
}
