package com.sourceplusplus.docserver

import groovy.util.logging.Slf4j
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

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Used to handle the Source++ website documentation.
 *
 * @version 0.2.5
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class DocServerBootstrap extends AbstractVerticle {

    public static final ResourceBundle BUILD = ResourceBundle.getBundle("source-doc-server_build")

    private static final Map<String, JsonObject> DOC_CACHE = ExpiringMap.builder()
            .expiration(1, TimeUnit.HOURS).build()

    static void main(String[] args) {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
        def configJSON
        def configFile = System.getenv("SOURCE_CONFIG")
        if (!configFile) {
            log.warn("Missing SOURCE_CONFIG environment variable. Using default settings")
            configFile = "local.json"
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
    void start(Promise<Void> fut) throws Exception {
        def serveLocalFiles = false
        def basePath = config().getString("base.path")
        if (!basePath.contains("http")) {
            serveLocalFiles = true
        }

        def router = createRouter()
        def client = WebClient.create(vertx)
        router.get().handler({ ctx ->
            if (serveLocalFiles) {
                def url = config().getString("base.path") + ctx.request().uri()
                log.info("Serving: " + ctx.request().uri())

                def cachedValue = DOC_CACHE.get(url)
                if (cachedValue != null) {
                    ctx.response().setStatusCode(200)
                            .end(Json.encodePrettily(cachedValue))
                    log.info("Served cached: " + ctx.request().uri())
                } else {
                    def markdownText = new File(url).text
                    def response = new JsonObject()
                    response.put("content", updateMarkdownLinks(url, markdownText).bytes.encodeBase64() as String)
                    DOC_CACHE.put(url, response)

                    ctx.response().setStatusCode(200)
                            .end(Json.encodePrettily(response))
                    log.info("Served: " + ctx.request().uri())
                }
            } else {
                if (ctx.request().uri() == "/favicon.ico") {
                    ctx.response().putHeader("location", config().getString("base.url") + "/favicon.ico")
                            .setStatusCode(302).end()
                    return
                }

                def url = config().getString("base.path") + ctx.request().uri() + "?1=1"
                if (config().getString("version")) {
                    url += "&ref=" + config().getString("version")
                }
                log.info("Serving: " + ctx.request().uri())

                def cachedValue = DOC_CACHE.get(url)
                if (cachedValue != null) {
                    ctx.response().setStatusCode(200)
                            .end(Json.encodePrettily(cachedValue))
                    log.info("Served cached: " + ctx.request().uri())
                } else {
                    def httpRequest = client.getAbs(url)
                    if (config().getString("access_username") && config().getString("access_token")) {
                        httpRequest = httpRequest.basicAuthentication(
                                config().getString("access_username"), config().getString("access_token"))
                    }

                    httpRequest.send({
                        if (it.succeeded()) {
                            def response = it.result().bodyAsJsonObject()
                            def content = new String(response.getString("content").decodeBase64())
                            response.put("content", updateMarkdownLinks(url, content).bytes.encodeBase64() as String)
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

    private String updateMarkdownLinks(String currentUrl, String markdownText) {
        currentUrl = currentUrl.replace(config().getString("base.path"), "")
        if (currentUrl.contains("?")) {
            currentUrl = currentUrl.substring(0, currentUrl.indexOf("?"))
        }
        def currentDirectory = ""
        currentUrl.split("/").each {
            if (it && !it.endsWith(".md")) {
                if (currentDirectory) {
                    currentDirectory += "/"
                }
                currentDirectory += it.substring(it.indexOf("-") + 1)
            }
        }

        def matcher = markdownText =~ "(\\[.+\\])\\(((?!http).+\\.md)\\/(.+\\))"
        matcher.each {
            def match = it as String[]
            String absoluteLink = makeLinkAbsolute(currentDirectory, match[1] + "(" + match[2] + ")")
            absoluteLink = absoluteLink.substring(0, absoluteLink.length() - 1) + match[3]
            markdownText = markdownText.replaceAll(Pattern.quote(match[0]), Matcher.quoteReplacement(absoluteLink))
        }
        markdownText.findAll("(\\[.+\\])(\\((?!http).+\\.md\\))").each {
            markdownText = markdownText.replaceAll(Pattern.quote(it),
                    Matcher.quoteReplacement(makeLinkAbsolute(currentDirectory, it)))
        }
        return markdownText
    }

    private String makeLinkAbsolute(String currentDirectory, String linkText) {
        String altText = linkText.substring(1, linkText.indexOf("]"))
        String link = linkText.substring(linkText.lastIndexOf("(") + 1, linkText.length() - 1)
        if (linkText.contains("../")) {
            link = link.replaceAll("\\.\\./", "")
            String parentDir = link.split("/")[0]
            parentDir = parentDir.substring(parentDir.indexOf("-") + 1)
            link = link.split("/")[1]
            link = link.substring(link.indexOf("-") + 1, link.length() - 3)
            return "[$altText](" + config().getString("base.url") + "/knowledge/$parentDir/$link)"
        } else {
            link = link.replaceAll("\\./", "")
            link = link.substring(link.indexOf("-") + 1, link.length() - 3)
            return "[$altText](" + config().getString("base.url") + "/knowledge/$currentDirectory/$link)"
        }
    }

    private static Handler<RoutingContext> getFailureHandler() {
        return {
            RoutingContext ctx -> log.error("Request failed", ctx.failure())
        }
    }
}
