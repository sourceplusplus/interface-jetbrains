package com.sourceplusplus.plugin.coordinate.artifact.track

import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJSourceMark
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject

/**
 * todo: description
 *
 * @version 0.2.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class ArtifactSignatureChangeTracker extends AbstractVerticle {

    public static final String ARTIFACT_ADDED = "ArtifactAdded"
    public static final String ARTIFACT_REMOVED = "ArtifactRemoved"
    public static final String ARTIFACT_SIGNATURE_UPDATED = "ArtifactSignatureUpdated"

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(ARTIFACT_ADDED, {
            def sourceMark = it.body() as IntelliJSourceMark

            //publish updated event
            vertx.eventBus().publish(ARTIFACT_SIGNATURE_UPDATED,
                    new JsonObject().put("change", "added")
                            .put("artifact", sourceMark.artifactQualifiedName))
        })

        vertx.eventBus().consumer(ARTIFACT_REMOVED, {
            def sourceMark = it.body() as IntelliJSourceMark
            sourceMark.markArtifactUnsubscribed()
            sourceMark.sourceFileMarker.removeSourceMark(sourceMark)

            //publish updated event
            vertx.eventBus().publish(ARTIFACT_SIGNATURE_UPDATED,
                    new JsonObject().put("change", "removed")
                            .put("artifact", sourceMark.artifactQualifiedName))
        })
        log.info("{} started", getClass().getSimpleName())
    }
}
