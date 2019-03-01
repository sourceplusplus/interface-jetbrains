package com.sourceplusplus.docserver

import io.vertx.core.*
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.net.JksOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import net.jodah.expiringmap.ExpiringMap
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * todo: description
 *
 * @version 0.1.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class DocServerBootstrap extends AbstractVerticle {

    public static final ResourceBundle BUILD = ResourceBundle.getBundle("source-doc-server_build")

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private static final Map<String, JsonObject> DOC_CACHE = ExpiringMap.builder()
            .expiration(1, TimeUnit.HOURS).build()

    static void main(String[] args) {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
        def configJSON
        def configFile = System.getenv("SOURCE_CONFIG")
        if (!configFile) {
            throw new RuntimeException("Missing SOURCE_CONFIG system environment!")
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
        configJSON = new JsonObject(configData)

        def vertxOptions = new VertxOptions()
        if (BUILD.getString("version") == "dev") {
            //allow debug pauses
            vertxOptions.setBlockedThreadCheckInterval(Integer.MAX_VALUE)
        }
        log.info("Build: " + BUILD.getString("build_date"))
        Vertx.vertx(vertxOptions).deployVerticle(new DocServerBootstrap(),
                new DeploymentOptions().setConfig(configJSON))
    }

    @Override
    void start(Future<Void> fut) throws Exception {
        def router = createRouter()
        def client = WebClient.create(vertx)
        router.get().handler({ ctx ->
            def url = config().getString("base.path") + ctx.request().uri() + "?1=1"
            if (config().getString("version")) {
                url += "&ref=" + config().getString("version")
            }
            if (config().getString("access_token")) {
                url += "&access_token=" + config().getString("access_token")
            }
            log.info("Serving: " + ctx.request().uri())

            def cachedValue = DOC_CACHE.get(url)
            if (cachedValue != null) {
                ctx.response().setStatusCode(200)
                        .end(Json.encodePrettily(cachedValue))
                log.info("Served cached: " + ctx.request().uri())
            } else {
                client.getAbs(url).send({
                    if (it.succeeded()) {
                        def response = it.result().bodyAsJsonObject()
                        def content = new String(response.getString("content").decodeBase64())
                        response.put("content", updateMarkdownLinks(content).bytes.encodeBase64() as String)
                        DOC_CACHE.put(url, response)

                        ctx.response().setStatusCode(200)
                                .end(Json.encodePrettily(response))
                        log.info("Served: " + ctx.request().uri())
                    } else {
                        log.error("Failed to get markdown", it.cause())
                        ctx.response().setStatusCode(500)
                                .end(it.cause().message)
                    }
                })
            }
        })

        def deploymentOptions = new DeploymentOptions()
        deploymentOptions.setConfig(config())

        if (config().getBoolean("ssl")) {
            vertx.createHttpServer(new HttpServerOptions().setSsl(true)
                    .setKeyStoreOptions(new JksOptions()
                    .setPath(config().getString("jks_path"))
                    .setPassword(config().getString("jks_password"))))
                    .requestHandler(router)
                    .listen(config().getInteger("port"), config().getString("host"), {
                if (it.succeeded()) {
                    fut.complete()
                    log.info("DocServer started (ver. " + BUILD.getString("version") + ")")
                } else {
                    fut.fail(it.cause())
                }
            })
        } else {
            vertx.createHttpServer()
                    .requestHandler(router)
                    .listen(config().getInteger("port"), config().getString("host"), {
                if (it.succeeded()) {
                    fut.complete()
                    log.info("DocServer started (ver. " + BUILD.getString("version") + ")")
                } else {
                    fut.fail(it.cause())
                }
            })
        }
    }

    private Router createRouter() {
        def router = Router.router(vertx)
        router.route().handler(CorsHandler.create("*"))
        router.route().failureHandler(getFailureHandler())

        //consume/produce JSON only
        router.route().consumes("application/json")
        router.route().produces("application/json")

        //enable the body parser so we can handle JSON input
        router.route().handler(BodyHandler.create())

        router.route().handler({ context ->
            //ensure no matter what responses are returned as JSON
            context.response().headers().add(HttpHeaders.CONTENT_TYPE, "application/json")
            //cache for awhile too
            context.response().headers().add(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")

            context.next()
        })
        return router.mountSubRouter("/v1", router)
    }

    private static String updateMarkdownLinks(String markdownText) {
        replaces.each { markdownText = markdownText.replace(it.key, it.value) }
        return markdownText
    }

    private static Handler<RoutingContext> getFailureHandler() {
        return {
            RoutingContext ctx -> log.error("Request failed", ctx.failure())
        }
    }

    //todo: smarter (too static and only work for introduction section)
    private static final Map<String, String> replaces = new HashMap<>()
    static {
        replaces.put("./01-getting-started.md", "../getting-started")
        replaces.put("./02a-self-hosted-checklist.md", "../self-hosted-setup-checklist")
        replaces.put("./02b-source-cloud-checklist.md", "../source-cloud-setup-checklist")
        replaces.put("./03-setup-source-core.md", "../setup-core")
        replaces.put("./04-configure-source-core.md", "../configure-core")
        replaces.put("./05-install-source-plugin.md", "../install-plugin")
        replaces.put("./06-configure-source-plugin.md", "../configure-plugin")
        replaces.put("./07-attach-source-agent.md", "../attach-agent")
        replaces.put("./08-configure-source-agent.md", "../configure-agent")
        replaces.put("./09-subscribe-to-artifact.md", "../subscribe-to-artifact")

        replaces.put("(#create-application)", "(../application-api/#create-application)")
        replaces.put("(#update-application)", "(../application-api/#update-application)")
        replaces.put("(#refresh-subscriber-subscriptions)", "(../application-api/#refresh-subscriber-subscriptions)")
        replaces.put("(#create-source-artifact)", "(../artifact-api/#create-source-artifact)")
        replaces.put("(#update-source-artifact-configuration)", "(../artifact-api/#update-source-artifact-configuration)")
        replaces.put("(#unsubscribe-source-artifact-subscriptions)", "(../artifact-api/#unsubscribe-source-artifact-subscriptions)")
        replaces.put("(#subscribe-artifact-metrics)", "(../metric-api/#subscribe-artifact-metrics)")
        replaces.put("(#unsubscribe-artifact-metrics)", "(../metric-api/#unsubscribe-artifact-metrics)")
        replaces.put("(#subscribe-artifact-traces)", "(../trace-api/#subscribe-artifact-traces)")
        replaces.put("(#unsubscribe-artifact-traces)", "(../trace-api/#unsubscribe-artifact-traces)")
        replaces.put("(#get-application)", "(../application-api/#get-application)")
        replaces.put("(#get-application-subscriptions)", "(../application-api/#get-application-subscriptions)")
        replaces.put("(#get-application-artifacts)", "(../artifact-api/#get-application-artifacts)")
        replaces.put("(#get-source-artifact)", "(../artifact-api/#get-source-artifact)")
        replaces.put("(#get-artifact-traces)", "(../trace-api/#get-artifact-traces)")
        replaces.put("(#get-artifact-trace-span)", "(../trace-api/#get-artifact-trace-span)")
        replaces.put("(#get-source-artifact-configuration)", "(../artifact-api/#get-source-artifact-configuration)")
        replaces.put("(#get-source-artifact-subscriptions)", "(../artifact-api/#get-source-artifact-subscriptions)")
    }
}
