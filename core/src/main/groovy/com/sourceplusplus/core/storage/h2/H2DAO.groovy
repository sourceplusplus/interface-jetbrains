package com.sourceplusplus.core.storage.h2

import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.application.SourceApplicationSubscription
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.artifact.SourceArtifactSubscription
import com.sourceplusplus.api.model.artifact.SourceArtifactSubscriptionType
import com.sourceplusplus.api.model.config.SourceAgentConfig
import com.sourceplusplus.core.storage.SourceStorage
import groovy.util.logging.Slf4j
import io.vertx.core.*
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException

import java.time.Instant

/**
 * todo: description
 *
 * @version 0.2.5
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class H2DAO extends SourceStorage {

    private static final String SOURCE_CORE_SCHEMA = Resources.toString(Resources.getResource(
            "storage/h2/schema/source_core.sql"), Charsets.UTF_8)
    private static final String CREATE_APPLICATION = Resources.toString(Resources.getResource(
            "storage/h2/queries/create_application.sql"), Charsets.UTF_8)
    private static final String UPDATE_APPLICATION = Resources.toString(Resources.getResource(
            "storage/h2/queries/update_application.sql"), Charsets.UTF_8)
    private static final String FIND_APPLICATION_BY_NAME = Resources.toString(Resources.getResource(
            "storage/h2/queries/find_application_by_name.sql"), Charsets.UTF_8)
    private static final String GET_APPLICATION = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_application.sql"), Charsets.UTF_8)
    private static final String GET_ALL_APPLICATIONS = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_all_applications.sql"), Charsets.UTF_8)
    private static final String CREATE_ARTIFACT = Resources.toString(Resources.getResource(
            "storage/h2/queries/create_artifact.sql"), Charsets.UTF_8)
    private static final String CREATE_ARTIFACT_WITH_CONFIG = Resources.toString(Resources.getResource(
            "storage/h2/queries/create_artifact_with_config.sql"), Charsets.UTF_8)
    private static final String UPDATE_ARTIFACT_CONFIG = Resources.toString(Resources.getResource(
            "storage/h2/queries/update_artifact_config.sql"), Charsets.UTF_8)
    private static final String GET_ARTIFACT = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_artifact.sql"), Charsets.UTF_8)
    private static final String GET_ARTIFACT_BY_ENDPOINT_NAME = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_artifact_by_endpoint_name.sql"), Charsets.UTF_8)
    private static final String GET_ARTIFACT_BY_ENDPOINT_ID = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_artifact_by_endpoint_id.sql"), Charsets.UTF_8)
    private static final String GET_ARTIFACT_BY_SUBSCRIBE_AUTOMATICALLY = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_artifact_by_subscribe_automatically.sql"), Charsets.UTF_8)
    private static final String GET_APPLICATION_ARTIFACTS = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_application_artifacts.sql"), Charsets.UTF_8)
    private static final String GET_ARTIFACT_SUBSCRIPTIONS = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_artifact_subscriptions.sql"), Charsets.UTF_8)
    private static final String GET_SUBSCRIBER_ARTIFACT_SUBSCRIPTIONS = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_subscriber_artifact_subscriptions.sql"), Charsets.UTF_8)
    private static final String GET_ALL_ARTIFACT_SUBSCRIPTIONS = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_all_artifact_subscriptions.sql"), Charsets.UTF_8)
    private static final String UPDATE_ARTIFACT_SUBSCRIPTION = Resources.toString(Resources.getResource(
            "storage/h2/queries/update_artifact_subscription.sql"), Charsets.UTF_8)
    private static final String GET_SUBSCRIBER_ARTIFACT_SUBSCRIPTION = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_subscriber_artifact_subscription.sql"), Charsets.UTF_8)
    private static final String GET_APPLICATION_SUBSCRIPTIONS = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_application_subscriptions.sql"), Charsets.UTF_8)
    private static final String DELETE_ARTIFACT_SUBSCRIPTION = Resources.toString(Resources.getResource(
            "storage/h2/queries/delete_artifact_subscription.sql"), Charsets.UTF_8)
    final JDBCClient client

    H2DAO(Vertx vertx, JsonObject config) {
        client = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", "jdbc:h2:mem:spp;DB_CLOSE_DELAY=-1")
                .put("driver_class", "org.h2.Driver"))

        client.update(SOURCE_CORE_SCHEMA, {
            if (it.failed()) {
                log.error("Failed to create Source++ Core schema in H2", it.cause())
                System.exit(-1)
            }
        })
    }

    @Override
    void createApplication(SourceApplication application, Handler<AsyncResult<SourceApplication>> handler) {
        def params = new JsonArray()
        params.add(application.appUuid())
        params.add(application.appName())
        params.add(application.createDate())
        if (application.agentConfig()) {
            params.add(Json.encode(application.agentConfig()))
        } else {
            params.addNull()
        }
        client.updateWithParams(CREATE_APPLICATION, params, {
            if (it.succeeded()) {
                handler.handle(Future.succeededFuture(application))
            } else if (it.cause() instanceof JdbcSQLIntegrityConstraintViolationException) {
                handler.handle(Future.failedFuture(new IllegalArgumentException("Application name is already in use")))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void updateApplication(SourceApplication application, Handler<AsyncResult<SourceApplication>> handler) {
        def params = new JsonArray()
        params.add(application.appUuid())
        if (application.appName()) {
            params.add(application.appName())
        } else {
            params.addNull()
        }
        if (application.agentConfig()) {
            params.add(Json.encode(application.agentConfig()))
        } else {
            params.addNull()
        }
        client.updateWithParams(UPDATE_APPLICATION, params, {
            if (it.succeeded()) {
                getApplication(application.appUuid(), {
                    if (it.succeeded()) {
                        handler.handle(Future.succeededFuture(it.result().get()))
                    } else {
                        handler.handle(Future.failedFuture(it.cause()))
                    }
                })
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void findApplicationByName(String appName, Handler<AsyncResult<Optional<SourceApplication>>> handler) {
        def params = new JsonArray()
        params.add(appName)
        client.queryWithParams(FIND_APPLICATION_BY_NAME, params, {
            if (it.succeeded()) {
                def results = it.result().results
                if (!results.isEmpty()) {
                    def existingApp = results.get(0)
                    def sourceApplication = SourceApplication.builder()
                            .appUuid(existingApp.getString(0))
                            .appName(existingApp.getString(1))
                            .createDate(Instant.parse(existingApp.getString(2)))
                    if (existingApp.getString(3)) {
                        sourceApplication.agentConfig(Json.decodeValue(
                                existingApp.getString(3), SourceAgentConfig.class))
                    }
                    handler.handle(Future.succeededFuture(Optional.of(sourceApplication.build())))
                } else {
                    handler.handle(Future.succeededFuture(Optional.empty()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getApplication(String appUuid, Handler<AsyncResult<Optional<SourceApplication>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        client.queryWithParams(GET_APPLICATION, params, {
            if (it.succeeded()) {
                def results = it.result().results
                if (!results.isEmpty()) {
                    def existingApp = results.get(0)
                    def sourceApplication = SourceApplication.builder()
                            .appUuid(existingApp.getString(0))
                            .appName(existingApp.getString(1))
                            .createDate(Instant.parse(existingApp.getString(2)))
                    if (existingApp.getString(3)) {
                        sourceApplication.agentConfig(Json.decodeValue(
                                existingApp.getString(3), SourceAgentConfig.class))
                    }
                    handler.handle(Future.succeededFuture(Optional.of(sourceApplication.build())))
                } else {
                    handler.handle(Future.succeededFuture(Optional.empty()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void getAllApplications(Handler<AsyncResult<List<SourceApplication>>> handler) {
        client.query(GET_ALL_APPLICATIONS, {
            if (it.succeeded()) {
                def applications = new ArrayList<SourceApplication>()
                def results = it.result().results
                if (!results.isEmpty()) {
                    results.each {
                        def sourceApplication = SourceApplication.builder()
                                .appUuid(it.getString(0))
                                .appName(it.getString(1))
                                .createDate(Instant.parse(it.getString(2)))
                        if (it.getString(3)) {
                            sourceApplication.agentConfig(Json.decodeValue(
                                    it.getString(3), SourceAgentConfig.class))
                        }
                        applications.add(sourceApplication.build())
                    }
                }
                handler.handle(Future.succeededFuture(applications))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void createArtifact(SourceArtifact artifact, Handler<AsyncResult<SourceArtifact>> handler) {
        def queryTemplate = CREATE_ARTIFACT
        def params = new JsonArray()
        params.add(artifact.appUuid())
        params.add(artifact.artifactQualifiedName())
        params.add(artifact.createDate())
        if (artifact.config() != null) {
            queryTemplate = CREATE_ARTIFACT_WITH_CONFIG
            if (artifact.config().endpoint() != null) {
                params.add(artifact.config().endpoint())
            } else {
                params.addNull()
            }
            if (artifact.config().subscribeAutomatically() != null) {
                params.add(artifact.config().subscribeAutomatically())
            } else {
                params.addNull()
            }
            if (artifact.config().forceSubscribe() != null) {
                params.add(artifact.config().forceSubscribe())
            } else {
                params.addNull()
            }
            if (artifact.config().moduleName() != null) {
                params.add(artifact.config().moduleName())
            } else {
                params.addNull()
            }
            if (artifact.config().component() != null) {
                params.add(artifact.config().component())
            } else {
                params.addNull()
            }
            if (artifact.config().endpointName() != null) {
                params.add(artifact.config().endpointName())
            } else {
                params.addNull()
            }
            if (artifact.config().endpointIds() != null) {
                params.add(artifact.config().endpointIds().collect().get(0))
            } else {
                params.addNull()
            }
        }
        client.updateWithParams(queryTemplate, params, {
            if (it.succeeded()) {
                handler.handle(Future.succeededFuture(artifact))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void updateArtifact(SourceArtifact artifact, Handler<AsyncResult<SourceArtifact>> handler) {
        def params = new JsonArray()
        params.add(artifact.appUuid())
        params.add(artifact.artifactQualifiedName())
        if (artifact.config()?.endpoint() != null) {
            params.add(artifact.config().endpoint())
        } else {
            params.addNull()
        }
        if (artifact.config()?.subscribeAutomatically() != null) {
            params.add(artifact.config().subscribeAutomatically())
        } else {
            params.addNull()
        }
        if (artifact.config()?.forceSubscribe() != null) {
            params.add(artifact.config().forceSubscribe())
        } else {
            params.addNull()
        }
        if (artifact.config()?.moduleName() != null) {
            params.add(artifact.config().moduleName())
        } else {
            params.addNull()
        }
        if (artifact.config()?.component() != null) {
            params.add(artifact.config().component())
        } else {
            params.addNull()
        }
        if (artifact.config()?.endpointName() != null) {
            params.add(artifact.config().endpointName())
        } else {
            params.addNull()
        }
        if (artifact.config()?.endpointIds() != null) {
            params.add(artifact.config().endpointIds().collect().get(0))
        } else {
            params.addNull()
        }
        client.updateWithParams(UPDATE_ARTIFACT_CONFIG, params, {
            if (it.succeeded()) {
                getArtifact(artifact.appUuid(), artifact.artifactQualifiedName(), {
                    if (it.succeeded()) {
                        handler.handle(Future.succeededFuture(it.result().get()))
                    } else {
                        handler.handle(Future.failedFuture(it.cause()))
                    }
                })
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void getArtifact(String appUuid, String artifactQualifiedName,
                     Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        params.add(artifactQualifiedName)
        client.queryWithParams(GET_ARTIFACT, params, {
            if (it.succeeded()) {
                def results = it.result().results
                if (!results.isEmpty()) {
                    def artifactDetails = results.get(0)
                    def artifact = SourceArtifact.builder()
                            .appUuid(artifactDetails.getString(0))
                            .artifactQualifiedName(artifactDetails.getString(1))
                            .createDate(Instant.parse(artifactDetails.getString(2)))
                            .lastUpdated(Instant.parse(artifactDetails.getString(3))).build()
                    def config = SourceArtifactConfig.builder()
                            .endpoint(artifactDetails.getBoolean(4))
                            .subscribeAutomatically(artifactDetails.getBoolean(5))
                            .forceSubscribe(artifactDetails.getBoolean(6))
                            .moduleName(artifactDetails.getString(7))
                            .component(artifactDetails.getString(8))
                            .endpointName(artifactDetails.getString(9))
                    if (artifactDetails.getString(10)) {
                        config.addEndpointIds(artifactDetails.getString(10))
                    }
                    handler.handle(Future.succeededFuture(Optional.of(artifact.withConfig(config.build()))))
                } else {
                    handler.handle(Future.succeededFuture(Optional.empty()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void findArtifactByEndpointName(String appUuid, String endpointName,
                                    Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        params.add(endpointName)
        client.queryWithParams(GET_ARTIFACT_BY_ENDPOINT_NAME, params, {
            if (it.succeeded()) {
                def results = it.result().results
                if (!results.isEmpty()) {
                    def artifactDetails = results.get(0)
                    def artifact = SourceArtifact.builder()
                            .appUuid(artifactDetails.getString(0))
                            .artifactQualifiedName(artifactDetails.getString(1))
                            .createDate(Instant.parse(artifactDetails.getString(2)))
                            .lastUpdated(Instant.parse(artifactDetails.getString(3))).build()
                    def config = SourceArtifactConfig.builder()
                            .endpoint(artifactDetails.getBoolean(4))
                            .subscribeAutomatically(artifactDetails.getBoolean(5))
                            .forceSubscribe(artifactDetails.getBoolean(6))
                            .moduleName(artifactDetails.getString(7))
                            .component(artifactDetails.getString(8))
                            .endpointName(artifactDetails.getString(9))
                    if (artifactDetails.getString(10)) {
                        config.addEndpointIds(artifactDetails.getString(10))
                    }
                    handler.handle(Future.succeededFuture(Optional.of(artifact.withConfig(config.build()))))
                } else {
                    handler.handle(Future.succeededFuture(Optional.empty()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void findArtifactByEndpointId(String appUuid, String endpointId,
                                  Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        params.add(endpointId)
        client.queryWithParams(GET_ARTIFACT_BY_ENDPOINT_ID, params, {
            if (it.succeeded()) {
                def results = it.result().results
                if (!results.isEmpty()) {
                    def artifactDetails = results.get(0)
                    def artifact = SourceArtifact.builder()
                            .appUuid(artifactDetails.getString(0))
                            .artifactQualifiedName(artifactDetails.getString(1))
                            .createDate(Instant.parse(artifactDetails.getString(2)))
                            .lastUpdated(Instant.parse(artifactDetails.getString(3))).build()
                    def config = SourceArtifactConfig.builder()
                            .endpoint(artifactDetails.getBoolean(4))
                            .subscribeAutomatically(artifactDetails.getBoolean(5))
                            .forceSubscribe(artifactDetails.getBoolean(6))
                            .moduleName(artifactDetails.getString(7))
                            .component(artifactDetails.getString(8))
                            .endpointName(artifactDetails.getString(9))
                    if (artifactDetails.getString(10)) {
                        config.addEndpointIds(artifactDetails.getString(10))
                    }
                    handler.handle(Future.succeededFuture(Optional.of(artifact.withConfig(config.build()))))
                } else {
                    handler.handle(Future.succeededFuture(Optional.empty()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void findArtifactBySubscribeAutomatically(String appUuid, Handler<AsyncResult<List<SourceArtifact>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        client.queryWithParams(GET_ARTIFACT_BY_SUBSCRIBE_AUTOMATICALLY, params, {
            if (it.succeeded()) {
                def artifacts = new ArrayList<SourceArtifact>()
                def results = it.result().results
                results.each {
                    def artifact = SourceArtifact.builder()
                            .appUuid(it.getString(0))
                            .artifactQualifiedName(it.getString(1))
                            .createDate(Instant.parse(it.getString(2)))
                            .lastUpdated(Instant.parse(it.getString(3))).build()
                    def config = SourceArtifactConfig.builder()
                            .endpoint(it.getBoolean(4))
                            .subscribeAutomatically(it.getBoolean(5))
                            .forceSubscribe(it.getBoolean(6))
                            .moduleName(it.getString(7))
                            .component(it.getString(8))
                            .endpointName(it.getString(9))
                    if (it.getString(10)) {
                        config.addEndpointIds(it.getString(10))
                    }
                    artifacts.add(artifact.withConfig(config.build()))
                }
                handler.handle(Future.succeededFuture(artifacts))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void getApplicationArtifacts(String appUuid, Handler<AsyncResult<List<SourceArtifact>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        client.queryWithParams(GET_APPLICATION_ARTIFACTS, params, {
            if (it.succeeded()) {
                def artifacts = new ArrayList<SourceArtifact>()
                def results = it.result().results
                results.each {
                    def artifact = SourceArtifact.builder()
                            .appUuid(it.getString(0))
                            .artifactQualifiedName(it.getString(1))
                            .createDate(Instant.parse(it.getString(2)))
                            .lastUpdated(Instant.parse(it.getString(3))).build()
                    def config = SourceArtifactConfig.builder()
                            .endpoint(it.getBoolean(4))
                            .subscribeAutomatically(it.getBoolean(5))
                            .forceSubscribe(it.getBoolean(6))
                            .moduleName(it.getString(7))
                            .component(it.getString(8))
                            .endpointName(it.getString(9))
                    if (it.getString(10)) {
                        config.addEndpointIds(it.getString(10))
                    }
                    artifacts.add(artifact.withConfig(config.build()))
                }
                handler.handle(Future.succeededFuture(artifacts))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void getArtifactSubscriptions(String appUuid, String artifactQualifiedName,
                                  Handler<AsyncResult<List<SourceArtifactSubscription>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        params.add(artifactQualifiedName)
        client.queryWithParams(GET_ARTIFACT_SUBSCRIPTIONS, params, {
            if (it.succeeded()) {
                def subscriptions = new HashMap<String, SourceArtifactSubscription.Builder>()
                def results = it.result().results
                results.each {
                    def key = it.getString(0) + "-" + it.getString(1) + "-" + it.getString(2)
                    def subscription = SourceArtifactSubscription.builder()
                            .subscriberUuid(it.getString(0))
                            .appUuid(it.getString(1))
                            .artifactQualifiedName(it.getString(2))
                    subscriptions.putIfAbsent(key, subscription)
                    subscriptions.get(key).putSubscriptionLastAccessed(SourceArtifactSubscriptionType.valueOf(
                            it.getString(3)), it.getInstant(4))
                }
                handler.handle(Future.succeededFuture(subscriptions.values().collect { it.build() }))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void getSubscriberArtifactSubscriptions(String subscriberUuid, String appUuid,
                                            Handler<AsyncResult<List<SourceArtifactSubscription>>> handler) {
        def params = new JsonArray()
        params.add(subscriberUuid)
        params.add(appUuid)
        client.queryWithParams(GET_SUBSCRIBER_ARTIFACT_SUBSCRIPTIONS, params, {
            if (it.succeeded()) {
                def subscriptions = new HashMap<String, SourceArtifactSubscription.Builder>()
                def results = it.result().results
                results.each {
                    def key = it.getString(0) + "-" + it.getString(1) + "-" + it.getString(2)
                    def subscription = SourceArtifactSubscription.builder()
                            .subscriberUuid(it.getString(0))
                            .appUuid(it.getString(1))
                            .artifactQualifiedName(it.getString(2))
                    subscriptions.putIfAbsent(key, subscription)
                    subscriptions.get(key).putSubscriptionLastAccessed(SourceArtifactSubscriptionType.valueOf(
                            it.getString(3)), it.getInstant(4))
                }
                handler.handle(Future.succeededFuture(subscriptions.values().collect { it.build() }))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void getArtifactSubscriptions(Handler<AsyncResult<List<SourceArtifactSubscription>>> handler) {
        client.query(GET_ALL_ARTIFACT_SUBSCRIPTIONS, {
            if (it.succeeded()) {
                def subscriptions = new HashMap<String, SourceArtifactSubscription.Builder>()
                def results = it.result().results
                results.each {
                    def key = it.getString(0) + "-" + it.getString(1) + "-" + it.getString(2)
                    def subscription = SourceArtifactSubscription.builder()
                            .subscriberUuid(it.getString(0))
                            .appUuid(it.getString(1))
                            .artifactQualifiedName(it.getString(2))
                    subscriptions.putIfAbsent(key, subscription)
                    subscriptions.get(key).putSubscriptionLastAccessed(SourceArtifactSubscriptionType.valueOf(
                            it.getString(3)), it.getInstant(4))
                }
                handler.handle(Future.succeededFuture(subscriptions.values().collect { it.build() }))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void updateArtifactSubscription(SourceArtifactSubscription subscription,
                                    Handler<AsyncResult<SourceArtifactSubscription>> handler) {
        def futures = []
        subscription.subscriptionLastAccessed().each {
            def future = Promise.promise()
            futures.add(future)

            def params = new JsonArray()
            params.add(subscription.subscriberUuid())
            params.add(subscription.appUuid())
            params.add(subscription.artifactQualifiedName())
            params.add(it.key.toString())
            params.add(it.value)
            client.updateWithParams(UPDATE_ARTIFACT_SUBSCRIPTION, params, future)
        }
        CompositeFuture.all(futures).onComplete({
            if (it.succeeded()) {
                getArtifactSubscription(subscription.subscriberUuid(), subscription.appUuid(),
                        subscription.artifactQualifiedName(), handler)
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void deleteArtifactSubscription(SourceArtifactSubscription subscription, Handler<AsyncResult<Void>> handler) {
        def params = new JsonArray()
        params.add(subscription.subscriberUuid())
        params.add(subscription.appUuid())
        params.add(subscription.artifactQualifiedName())
        client.updateWithParams(DELETE_ARTIFACT_SUBSCRIPTION, params, {
            if (it.succeeded()) {
                handler.handle(Future.succeededFuture())
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void setArtifactSubscription(SourceArtifactSubscription subscription,
                                 Handler<AsyncResult<SourceArtifactSubscription>> handler) {
        deleteArtifactSubscription(subscription, {
            if (it.succeeded()) {
                updateArtifactSubscription(subscription, handler)
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void getArtifactSubscription(String subscriberUuid, String appUuid, String artifactQualifiedName,
                                 Handler<AsyncResult<Optional<SourceArtifactSubscription>>> handler) {
        def params = new JsonArray()
        params.add(subscriberUuid)
        params.add(appUuid)
        params.add(artifactQualifiedName)
        client.queryWithParams(GET_SUBSCRIBER_ARTIFACT_SUBSCRIPTION, params, {
            if (it.succeeded()) {
                def subscriptions = new HashMap<String, SourceArtifactSubscription.Builder>()
                def results = it.result().results
                results.each {
                    def key = it.getString(0) + "-" + it.getString(1) + "-" + it.getString(2)
                    def subscription = SourceArtifactSubscription.builder()
                            .subscriberUuid(it.getString(0))
                            .appUuid(it.getString(1))
                            .artifactQualifiedName(it.getString(2))
                    subscriptions.putIfAbsent(key, subscription)
                    subscriptions.get(key).putSubscriptionLastAccessed(SourceArtifactSubscriptionType.valueOf(
                            it.getString(3)), it.getInstant(4))
                }

                def value = subscriptions.values().collect { it.build() }
                if (value) {
                    handler.handle(Future.succeededFuture(Optional.of(value.get(0))))
                } else {
                    handler.handle(Future.succeededFuture(Optional.empty()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void getApplicationSubscriptions(String appUuid, Handler<AsyncResult<List<SourceApplicationSubscription>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        client.queryWithParams(GET_APPLICATION_SUBSCRIPTIONS, params, {
            if (it.succeeded()) {
                def results = it.result().results
                def subscriptionCounts = new HashMap<String, Set<String>>()
                def applicationSubscriptions = new HashMap<String, SourceApplicationSubscription.Builder>()
                results.each {
                    def subscription = SourceArtifactSubscription.builder()
                            .subscriberUuid(it.getString(0))
                            .appUuid(it.getString(1))
                            .artifactQualifiedName(it.getString(2))
                            .putSubscriptionLastAccessed(SourceArtifactSubscriptionType.valueOf(it.getString(3)), it.getInstant(4))
                            .build()
                    subscriptionCounts.putIfAbsent(subscription.artifactQualifiedName(), new HashSet<>())
                    applicationSubscriptions.putIfAbsent(subscription.artifactQualifiedName(),
                            SourceApplicationSubscription.builder().artifactQualifiedName(subscription.artifactQualifiedName()))
                    def appSubscription = applicationSubscriptions.get(subscription.artifactQualifiedName())
                    def subscribers = subscriptionCounts.get(subscription.artifactQualifiedName())
                    subscribers.add(subscription.subscriberUuid())
                    appSubscription.subscribers(subscribers.size())
                    appSubscription.addAllTypes(subscription.subscriptionLastAccessed().keySet())
                }
                handler.handle(Future.succeededFuture(applicationSubscriptions.values().collect { it.build() }))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void refreshDatabase(Handler<AsyncResult<Void>> handler) {
        handler.handle(Future.succeededFuture())
    }

    @Override
    boolean needsManualRefresh() {
        return false
    }
}
