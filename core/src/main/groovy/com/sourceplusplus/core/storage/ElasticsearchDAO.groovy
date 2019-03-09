package com.sourceplusplus.core.storage

import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.artifact.SourceArtifact
import io.searchbox.client.JestClient
import io.searchbox.client.JestClientFactory
import io.searchbox.client.JestResult
import io.searchbox.client.JestResultHandler
import io.searchbox.client.config.HttpClientConfig
import io.searchbox.core.*
import io.searchbox.indices.CreateIndex
import io.searchbox.indices.mapping.PutMapping
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * todo: description
 *
 * @version 0.1.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class ElasticsearchDAO {

    private static final String SOURCE_ARTIFACT_INDEX_MAPPINGS = Resources.toString(Resources.getResource(
            "config/elasticsearch/artifact_index_mappings.json"), Charsets.UTF_8)
    private static final Logger log = LoggerFactory.getLogger(this.name)
    private final static String SPP_INDEX = "source_plus_plus"
    private final String elasticSearchHost
    private final int elasticSearchPort
    private final JestClient client

    ElasticsearchDAO(JsonObject config) {
        this.elasticSearchHost = config.getString("host")
        this.elasticSearchPort = config.getInteger("port")

        JestClientFactory factory = new JestClientFactory()
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder("http://$elasticSearchHost:$elasticSearchPort")
                .multiThreaded(true)
                .build())
        client = factory.getObject()
        client.executeAsync(new Ping.Builder().build(), new JestResultHandler() {
            @Override
            void completed(Object result) {
                installIndexes({
                    if (it.failed()) {
                        log.error("Failed to create index mappings in Elasticsearch", it.cause())
                        System.exit(-1)
                    } else {
                        log.info("Elasticsearch connected")
                    }
                })
            }

            @Override
            void failed(Exception ex) {
                log.error("Failed to connect to Elasticsearch", ex)
                System.exit(-1)
            }
        })
    }

    private void installIndexes(Handler<AsyncResult<Void>> handler) {
        //todo: application
        def createIndex = new CreateIndex.Builder(SPP_INDEX + "_artifact")
                .settings(["number_of_shards": 2, "number_of_replicas": 0])
                .build()
        client.executeAsync(createIndex, new JestResultHandler<JestResult>() {

            @Override
            void completed(JestResult indexResult) {
                if (indexResult.succeeded) {
                    log.info("Created artifact index")
                } else {
                    if (indexResult.jsonObject.getAsJsonObject("error").get("type").asString
                            == "resource_already_exists_exception") {
                        log.debug("Artifact index already exists")
                    } else {
                        handler.handle(Future.failedFuture(indexResult.jsonString))
                        return
                    }
                }

                def artifactMapping = new PutMapping.Builder(SPP_INDEX + "_artifact",
                        "artifact", SOURCE_ARTIFACT_INDEX_MAPPINGS).build()
                client.executeAsync(artifactMapping, new JestResultHandler<JestResult>() {

                    @Override
                    void completed(JestResult mappingResult) {
                        if (mappingResult.succeeded) {
                            log.debug("Created artifact index mapping")
                            handler.handle(Future.succeededFuture())
                        } else {
                            handler.handle(Future.failedFuture(indexResult.jsonString))
                        }
                    }

                    @Override
                    void failed(Exception ex) {
                        log.error("Failed to create artifact index mapping", ex)
                        handler.handle(Future.failedFuture(ex))
                    }
                })
            }

            @Override
            void failed(Exception ex) {
                log.error("Failed to create artifact index", ex)
                handler.handle(Future.failedFuture(ex))
            }
        })
    }

    void createApplication(SourceApplication application, Handler<AsyncResult<SourceApplication>> handler) {
        def createApplication = new JsonObject(Json.encode(application))
        createApplication.remove("create_request") //todo: smarter
        createApplication.remove("update_request") //todo: smarter

        def index = new Index.Builder(Json.encode(createApplication)).index(SPP_INDEX + "_application")
                .type("application")
                .id(application.appUuid()).build()
        client.executeAsync(index, new JestResultHandler() {

            @Override
            void completed(Object result) {
                log.info(String.format("Created application. App UUID: %s - App name: %s",
                        application.appUuid(), application.appName()))
                handler.handle(Future.succeededFuture(application))
            }

            @Override
            void failed(Exception ex) {
                log.error("Failed to create application", ex)
                handler.handle(Future.failedFuture(ex))
            }
        })
    }

    void updateApplication(SourceApplication application, Handler<AsyncResult<SourceApplication>> handler) {
        def updatedApplication = new JsonObject(Json.encode(application))
        updatedApplication.remove("create_request") //todo: smarter
        updatedApplication.remove("update_request") //todo: smarter

        def update = new Update.Builder(new JsonObject().put("doc", updatedApplication).toString())
                .index(SPP_INDEX + "_application")
                .type("application")
                .id(Objects.requireNonNull(application.appUuid())).build()
        client.executeAsync(update, new JestResultHandler<DocumentResult>() {

            @Override
            void completed(DocumentResult result) {
                log.info(String.format("Updated application. App uuid: %s - App name: %s",
                        application.appUuid(), application.appName()))
                if (result.succeeded) {
                    getApplication(application.appUuid(), {
                        if (it.succeeded()) {
                            handler.handle(Future.succeededFuture(it.result().get()))
                        } else {
                            log.error("Failed to get updated application", it.cause())
                            handler.handle(Future.failedFuture(it.cause()))
                        }
                    })
                } else {
                    log.error(result.errorMessage)
                    handler.handle(Future.failedFuture(result.errorMessage))
                }
            }

            @Override
            void failed(Exception ex) {
                log.error("Failed to update application", ex)
                handler.handle(Future.failedFuture(ex))
            }
        })
    }

    void getApplication(String appUuid, Handler<AsyncResult<Optional<SourceApplication>>> handler) {
        def get = new Get.Builder(SPP_INDEX + "_application", Objects.requireNonNull(appUuid))
                .type("application").build()
        client.executeAsync(get, new JestResultHandler<DocumentResult>() {

            @Override
            void completed(DocumentResult result) {
                if (result.responseCode == 404) {
                    log.warn(String.format("Could not find application. App UUID: %s", appUuid))
                    handler.handle(Future.succeededFuture(Optional.empty()))
                } else if (result.succeeded) {
                    def application = Json.decodeValue(result.sourceAsString, SourceApplication.class)
                    log.debug(String.format("Found application. App UUID: %s - App name: %s",
                            application.appUuid(), application.appName()))
                    handler.handle(Future.succeededFuture(Optional.of(application)))
                } else {
                    log.error(result.errorMessage)
                    handler.handle(Future.failedFuture(result.errorMessage))
                }
            }

            @Override
            void failed(Exception ex) {
                log.error("Failed to get application", ex)
                handler.handle(Future.failedFuture(ex))
            }
        })
    }

    void createArtifact(SourceArtifact artifact, Handler<AsyncResult<SourceArtifact>> handler) {
        def index = new Index.Builder(Json.encode(artifact)).index(SPP_INDEX + "_artifact")
                .type("artifact")
                .id(URLEncoder.encode(artifact.appUuid() + "-" + artifact.artifactQualifiedName())).build()
        client.executeAsync(index, new JestResultHandler() {

            @Override
            void completed(Object result) {
                log.info(String.format("Created artifact. App uuid: %s - Artifact qualified name: %s",
                        artifact.appUuid(), artifact.artifactQualifiedName()))
                handler.handle(Future.succeededFuture(artifact))
            }

            @Override
            void failed(Exception ex) {
                log.error("Failed to create artifact", ex)
                handler.handle(Future.failedFuture(ex))
            }
        })
    }

    void updateArtifact(SourceArtifact artifact, Handler<AsyncResult<SourceArtifact>> handler) {
        def update = new Update.Builder(new JsonObject().put("doc", new JsonObject(Json.encode(artifact))).toString())
                .index(SPP_INDEX + "_artifact")
                .type("artifact")
                .id(URLEncoder.encode(artifact.appUuid() + "-" + artifact.artifactQualifiedName())).build()
        client.executeAsync(update, new JestResultHandler<DocumentResult>() {

            @Override
            void completed(DocumentResult result) {
                log.info(String.format("Updated artifact. App uuid: %s - Artifact qualified name: %s",
                        artifact.appUuid(), artifact.artifactQualifiedName()))
                if (result.succeeded) {
                    getArtifact(artifact.appUuid(), artifact.artifactQualifiedName(), {
                        if (it.succeeded()) {
                            handler.handle(Future.succeededFuture(it.result().get()))
                        } else {
                            log.error("Failed to get updated artifact", it.cause())
                            handler.handle(Future.failedFuture(it.cause()))
                        }
                    })
                } else {
                    log.error(result.errorMessage)
                    handler.handle(Future.failedFuture(result.errorMessage))
                }
            }

            @Override
            void failed(Exception ex) {
                log.error("Failed to update artifact", ex)
                handler.handle(Future.failedFuture(ex))
            }
        })
    }

    void getArtifact(String appUuid, String artifactQualifiedName,
                     Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        def get = new Get.Builder(SPP_INDEX + "_artifact", URLEncoder.encode(appUuid + "-" + artifactQualifiedName))
                .type("artifact").build()
        client.executeAsync(get, new JestResultHandler<DocumentResult>() {

            @Override
            void completed(DocumentResult result) {
                if (result.responseCode == 404) {
                    log.debug(String.format("Could not find artifact. Artifact qualified name: %s",
                            artifactQualifiedName))
                    handler.handle(Future.succeededFuture(Optional.empty()))
                } else if (result.succeeded) {
                    def artifact = Json.decodeValue(result.sourceAsString, SourceArtifact.class)
                    artifact = artifact.withAppUuid(appUuid).withArtifactQualifiedName(artifactQualifiedName)

                    log.debug(String.format("Found artifact. Artifact qualified name: %s",
                            artifact.artifactQualifiedName()))
                    handler.handle(Future.succeededFuture(Optional.of(artifact)))
                } else {
                    log.error(result.errorMessage)
                    handler.handle(Future.failedFuture(result.errorMessage))
                }
            }

            @Override
            void failed(Exception ex) {
                log.error("Failed to get artifact", ex)
                handler.handle(Future.failedFuture(ex))
            }
        })
    }

    void findArtifactByEndpointName(String appUuid, String endpointName,
                                    Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        String query = '{\n' +
                '  "query": {\n' +
                '    "bool": {\n' +
                '      "must": [\n' +
                '        { "term": { "app_uuid": "' + appUuid + '" }},\n' +
                '        { "term": { "config.endpoint_name": "' + endpointName + '" }}\n' +
                '      ]\n' +
                '    }\n' +
                '  }\n' +
                '}'
        def search = new Search.Builder(query)
                .addIndex(SPP_INDEX + "_artifact")
                .build()

        client.executeAsync(search, new JestResultHandler<SearchResult>() {

            @Override
            void completed(SearchResult result) {
                if (result.succeeded || result.responseCode == 404) {
                    def resultString = result.getSourceAsString()
                    if (resultString == null || resultString.isEmpty()) {
                        log.debug(String.format("Could not find artifact. Endpoint name: %s", endpointName))
                        handler.handle(Future.succeededFuture(Optional.empty()))
                    } else {
                        def artifact = Json.decodeValue(result.getSourceAsString(), SourceArtifact.class)
                        handler.handle(Future.succeededFuture(Optional.of(artifact)))
                    }
                } else {
                    log.error(result.errorMessage)
                    handler.handle(Future.failedFuture(result.errorMessage))
                }
            }

            @Override
            void failed(Exception ex) {
                log.error("Failed to search for artifact by endpoint name", ex)
                handler.handle(Future.failedFuture(ex))
            }
        })
    }

    void findArtifactByEndpointId(String appUuid, String endpointId,
                                  Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        String query = '{\n' +
                '  "query": {\n' +
                '    "bool": {\n' +
                '      "must": [\n' +
                '        { "term": { "app_uuid": "' + appUuid + '" }},\n' +
                '        { "term": { "config.endpoint_ids": "' + endpointId + '" }}\n' +
                '      ]\n' +
                '    }\n' +
                '  }\n' +
                '}'
        def search = new Search.Builder(query)
                .addIndex(SPP_INDEX + "_artifact")
                .build()

        client.executeAsync(search, new JestResultHandler<SearchResult>() {

            @Override
            void completed(SearchResult result) {
                if (result.succeeded || result.responseCode == 404) {
                    def resultString = result.getSourceAsString()
                    if (resultString == null || resultString.isEmpty()) {
                        log.debug(String.format("Could not find artifact. Endpoint id: %s", endpointId))
                        handler.handle(Future.succeededFuture(Optional.empty()))
                    } else {
                        def artifact = Json.decodeValue(result.getSourceAsString(), SourceArtifact.class)
                        handler.handle(Future.succeededFuture(Optional.of(artifact)))
                    }
                } else {
                    log.error(result.errorMessage)
                    handler.handle(Future.failedFuture(result.errorMessage))
                }
            }

            @Override
            void failed(Exception ex) {
                log.error("Failed to search for artifact by endpoint id", ex)
                handler.handle(Future.failedFuture(ex))
            }
        })
    }

    void findArtifactBySubscribeAutomatically(String appUuid, Handler<AsyncResult<List<SourceArtifact>>> handler) {
        String query = '{\n' +
                '  "query": {\n' +
                '    "bool": {\n' +
                '      "must": [\n' +
                '        { "term": { "app_uuid": "' + appUuid + '" }},\n' +
                '        { "match": { "config.subscribe_automatically": true }}\n' +
                '      ]\n' +
                '    }\n' +
                '  }\n' +
                '}'
        def search = new Search.Builder(query)
                .addIndex(SPP_INDEX + "_artifact")
                .build()

        client.executeAsync(search, new JestResultHandler<SearchResult>() {

            @Override
            void completed(SearchResult result) {
                if (result.succeeded || result.responseCode == 404) {
                    def results = result.getSourceAsStringList()
                    if (results == null || results.isEmpty()) {
                        log.debug(String.format("Could not find any artifacts which subscribed automatically"))
                        handler.handle(Future.succeededFuture(new ArrayList<>()))
                    } else {
                        def rtnList = new ArrayList<SourceArtifact>()
                        results.each {
                            rtnList.add(Json.decodeValue(it, SourceArtifact.class))
                        }
                        handler.handle(Future.succeededFuture(rtnList))
                    }
                } else {
                    log.error(result.errorMessage)
                    handler.handle(Future.failedFuture(result.errorMessage))
                }
            }

            @Override
            void failed(Exception ex) {
                log.error("Failed to search for artifact by endpoint name", ex)
                handler.handle(Future.failedFuture(ex))
            }
        })
    }

    void getAllApplications(Handler<AsyncResult<List<SourceApplication>>> handler) {
        String query = '{\n' +
                '    "query": {\n' +
                '        "match_all": {}\n' +
                '    }\n' +
                '}'
        def search = new Search.Builder(query)
                .addIndex(SPP_INDEX + "_application")
                .build()

        client.executeAsync(search, new JestResultHandler<SearchResult>() {

            @Override
            void completed(SearchResult result) {
                if (result.succeeded || result.responseCode == 404) {
                    def results = result.getSourceAsStringList()
                    if (results == null || results.isEmpty()) {
                        log.debug(String.format("Could not find any applications"))
                        handler.handle(Future.succeededFuture(new ArrayList<>()))
                    } else {
                        def rtnList = new ArrayList<SourceApplication>()
                        results.each {
                            rtnList.add(Json.decodeValue(it, SourceApplication.class))
                        }
                        handler.handle(Future.succeededFuture(rtnList))
                    }
                } else {
                    log.error(result.errorMessage)
                    handler.handle(Future.failedFuture(result.errorMessage))
                }
            }

            @Override
            void failed(Exception ex) {
                log.error("Failed to get all applications", ex)
                handler.handle(Future.failedFuture(ex))
            }
        })
    }

    void getApplicationArtifacts(String appUuid, Handler<AsyncResult<List<SourceArtifact>>> handler) {
        String query = '{\n' +
                '  "query": {\n' +
                '    "bool": {\n' +
                '      "must": [\n' +
                '        { "term": { "app_uuid": "' + appUuid + '" }}\n' +
                '      ]\n' +
                '    }\n' +
                '  }\n' +
                '}'
        def search = new Search.Builder(query)
                .addIndex(SPP_INDEX + "_artifact")
                .build()

        client.executeAsync(search, new JestResultHandler<SearchResult>() {

            @Override
            void completed(SearchResult result) {
                if (result.succeeded || result.responseCode == 404) {
                    def results = result.getSourceAsStringList()
                    if (results == null || results.isEmpty()) {
                        log.debug(String.format("Could not find any artifacts for application: " + appUuid))
                        handler.handle(Future.succeededFuture(new ArrayList<>()))
                    } else {
                        def rtnList = new ArrayList<SourceArtifact>()
                        results.each {
                            rtnList.add(Json.decodeValue(it, SourceArtifact.class))
                        }
                        handler.handle(Future.succeededFuture(rtnList))
                    }
                } else {
                    log.error(result.errorMessage)
                    handler.handle(Future.failedFuture(result.errorMessage))
                }
            }

            @Override
            void failed(Exception ex) {
                log.error("Failed to get all artifacts for application: " + appUuid, ex)
                handler.handle(Future.failedFuture(ex))
            }
        })
    }
}
