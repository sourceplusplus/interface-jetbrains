package com.sourceplusplus.core.storage

import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.application.SourceApplicationSubscription
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactSubscription
import com.sourceplusplus.api.model.config.SourceAgentConfig
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Instant

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class H2DAO extends AbstractSourceStorage {

    private static final Logger log = LoggerFactory.getLogger(this.name)

    private static final String SOURCE_CORE_SCHEMA = Resources.toString(Resources.getResource(
            "storage/h2/schema/source_core.sql"), Charsets.UTF_8)
    private static final String CREATE_APPLICATION = Resources.toString(Resources.getResource(
            "storage/h2/queries/create_application.sql"), Charsets.UTF_8)
    private static final String UPDATE_APPLICATION = Resources.toString(Resources.getResource(
            "storage/h2/queries/update_application.sql"), Charsets.UTF_8)
    private static final String GET_APPLICATION = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_application.sql"), Charsets.UTF_8)
    final JDBCClient client

    H2DAO(Vertx vertx, JsonObject config) {
        client = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
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
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    @Override
    void updateApplication(SourceApplication application, Handler<AsyncResult<SourceApplication>> handler) {
        def params = new JsonArray()
        if (application.appName()) {
            params.add(application.appName())
        } else {
            params.addNull()
        }
        params.add(application.appUuid())
        if (application.agentConfig()) {
            params.add(Json.encode(application.agentConfig()))
        } else {
            params.addNull()
        }
        params.add(application.appUuid())
        params.add(application.appUuid())
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
    void getApplication(String appUuid, Handler<AsyncResult<Optional<SourceApplication>>> handler) {
        client.query(GET_APPLICATION, {
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
    void createArtifact(SourceArtifact artifact, Handler<AsyncResult<SourceArtifact>> handler) {

    }

    @Override
    void updateArtifact(SourceArtifact artifact, Handler<AsyncResult<SourceArtifact>> handler) {

    }

    @Override
    void getArtifact(String appUuid, String artifactQualifiedName,
                     Handler<AsyncResult<Optional<SourceArtifact>>> handler) {

    }

    @Override
    void findArtifactByEndpointName(String appUuid, String endpointName,
                                    Handler<AsyncResult<Optional<SourceArtifact>>> handler) {

    }

    @Override
    void findArtifactByEndpointId(String appUuid, String endpointId,
                                  Handler<AsyncResult<Optional<SourceArtifact>>> handler) {

    }

    @Override
    void findArtifactBySubscribeAutomatically(String appUuid, Handler<AsyncResult<List<SourceArtifact>>> handler) {

    }

    @Override
    void getAllApplications(Handler<AsyncResult<List<SourceApplication>>> handler) {

    }

    @Override
    void getApplicationArtifacts(String appUuid, Handler<AsyncResult<List<SourceArtifact>>> handler) {

    }

    @Override
    void getArtifactSubscriptions(String appUuid, String artifactQualifiedName,
                                  Handler<AsyncResult<List<SourceArtifactSubscription>>> handler) {

    }

    @Override
    void getSubscriberArtifactSubscriptions(String subscriberUuid, String appUuid,
                                            Handler<AsyncResult<List<SourceArtifactSubscription>>> handler) {

    }

    @Override
    void getArtifactSubscriptions(Handler<AsyncResult<List<SourceArtifactSubscription>>> handler) {

    }

    @Override
    void updateArtifactSubscription(SourceArtifactSubscription subscription,
                                    Handler<AsyncResult<SourceArtifactSubscription>> handler) {

    }

    @Override
    void deleteArtifactSubscription(SourceArtifactSubscription subscription, Handler<AsyncResult<Void>> handler) {

    }

    @Override
    void setArtifactSubscription(SourceArtifactSubscription subscription,
                                 Handler<AsyncResult<SourceArtifactSubscription>> handler) {

    }

    @Override
    void getArtifactSubscription(String subscriberUuid, String appUuid, String artifactQualifiedName,
                                 Handler<AsyncResult<Optional<SourceArtifactSubscription>>> handler) {

    }

    @Override
    void getApplicationSubscriptions(String appUuid, Handler<AsyncResult<List<SourceApplicationSubscription>>> handler) {

    }

    @Override
    void refreshDatabase(Handler<AsyncResult<Void>> handler) {
        handler.handle(Future.succeededFuture())
    }
}
