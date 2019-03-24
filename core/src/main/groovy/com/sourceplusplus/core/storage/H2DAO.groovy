package com.sourceplusplus.core.storage

import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.application.SourceApplicationSubscription
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactSubscription
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

/**
 * todo: description
 *
 * @version 0.1.1
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class H2DAO extends AbstractSourceStorage {

    @Override
    void createApplication(SourceApplication application, Handler<AsyncResult<SourceApplication>> handler) {

    }

    @Override
    void updateApplication(SourceApplication application, Handler<AsyncResult<SourceApplication>> handler) {

    }

    @Override
    void getApplication(String appUuid, Handler<AsyncResult<Optional<SourceApplication>>> handler) {

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
    }
}
