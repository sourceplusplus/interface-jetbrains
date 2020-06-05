package com.sourceplusplus.core.storage

import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.application.SourceApplicationSubscription
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactSubscription
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

/**
 * Represents a storage for saving/fetching core data.
 *
 * @version 0.2.6
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
abstract class SourceStorage {

    abstract void getCoreConfig(Handler<AsyncResult<Optional<CoreConfig>>> handler)

    abstract void updateCoreConfig(CoreConfig coreConfig, Handler<AsyncResult<CoreConfig>> handler)

    abstract void createApplication(SourceApplication application, Handler<AsyncResult<SourceApplication>> handler)

    abstract void updateApplication(SourceApplication application, Handler<AsyncResult<SourceApplication>> handler)

    abstract void findApplicationByName(String appName, Handler<AsyncResult<Optional<SourceApplication>>> handler)

    /**
     * Retrieve {@link SourceApplication} by an existing {@link SourceApplication#appUuid}.
     *
     * @param appUuid the {@link SourceApplication#appUuid} to retrieve
     * @param handler executed with {@link SourceApplication} if found, empty Optional otherwise
     */
    abstract void getApplication(String appUuid, Handler<AsyncResult<Optional<SourceApplication>>> handler)

    abstract void getAllApplications(Handler<AsyncResult<List<SourceApplication>>> handler)

    abstract void createArtifact(SourceArtifact artifact, Handler<AsyncResult<SourceArtifact>> handler)

    abstract void updateArtifact(SourceArtifact artifact, Handler<AsyncResult<SourceArtifact>> handler)

    abstract void getArtifact(String appUuid, String artifactQualifiedName,
                              Handler<AsyncResult<Optional<SourceArtifact>>> handler)

    abstract void findArtifactByEndpointName(String appUuid, String endpointName,
                                             Handler<AsyncResult<Optional<SourceArtifact>>> handler)

    abstract void findArtifactByEndpointId(String appUuid, String endpointId,
                                           Handler<AsyncResult<Optional<SourceArtifact>>> handler)

    abstract void findArtifactBySubscribeAutomatically(String appUuid,
                                                       Handler<AsyncResult<List<SourceArtifact>>> handler)

    abstract void findArtifactByEndpoint(String appUuid,
                                         Handler<AsyncResult<List<SourceArtifact>>> handler)

    abstract void findArtifactByFailing(String appUuid,
                                        Handler<AsyncResult<List<SourceArtifact>>> handler)

    abstract void getApplicationArtifacts(String appUuid, Handler<AsyncResult<List<SourceArtifact>>> handler)

    abstract void getArtifactSubscriptions(String appUuid, String artifactQualifiedName,
                                           Handler<AsyncResult<List<SourceArtifactSubscription>>> handler)

    abstract void getSubscriberArtifactSubscriptions(String subscriberUuid, String appUuid,
                                                     Handler<AsyncResult<List<SourceArtifactSubscription>>> handler)

    abstract void getArtifactSubscriptions(Handler<AsyncResult<List<SourceArtifactSubscription>>> handler)

    abstract void updateArtifactSubscription(SourceArtifactSubscription subscription,
                                             Handler<AsyncResult<SourceArtifactSubscription>> handler)

    abstract void deleteArtifactSubscription(SourceArtifactSubscription subscription, Handler<AsyncResult<Void>> handler)

    abstract void setArtifactSubscription(SourceArtifactSubscription subscription,
                                          Handler<AsyncResult<SourceArtifactSubscription>> handler)

    abstract void getArtifactSubscription(String subscriberUuid, String appUuid, String artifactQualifiedName,
                                          Handler<AsyncResult<Optional<SourceArtifactSubscription>>> handler)

    abstract void getApplicationSubscriptions(String appUuid,
                                              Handler<AsyncResult<List<SourceApplicationSubscription>>> handler)

    abstract void refreshDatabase(Handler<AsyncResult<Void>> handler)

    abstract boolean needsManualRefresh()
}
