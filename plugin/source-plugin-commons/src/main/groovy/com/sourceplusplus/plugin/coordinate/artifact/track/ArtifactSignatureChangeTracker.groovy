package com.sourceplusplus.plugin.coordinate.artifact.track

import com.sourceplusplus.plugin.marker.mark.SourceMark
import io.vertx.core.AbstractVerticle
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
class ArtifactSignatureChangeTracker extends AbstractVerticle {

    public static final String ARTIFACT_ADDED = "ArtifactAdded"
    public static final String ARTIFACT_REMOVED = "ArtifactRemoved"
    public static final String ARTIFACT_SIGNATURE_UPDATED = "ArtifactSignatureUpdated"

    private static final Logger log = LoggerFactory.getLogger(this.name)

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(ARTIFACT_ADDED, {
            def sourceMark = it.body() as SourceMark

            //publish updated event
            vertx.eventBus().publish(ARTIFACT_SIGNATURE_UPDATED,
                    new JsonObject().put("change", "added")
                            .put("artifact", sourceMark.sourceMethod.artifactQualifiedName()))
        })

        vertx.eventBus().consumer(ARTIFACT_REMOVED, {
            def sourceMark = it.body() as SourceMark
            sourceMark.artifactSubscribed = false
            sourceMark.sourceFileMarker.removeSourceMark(sourceMark)

            //publish updated event
            vertx.eventBus().publish(ARTIFACT_SIGNATURE_UPDATED,
                    new JsonObject().put("change", "removed")
                            .put("artifact", sourceMark.sourceMethod.artifactQualifiedName()))
        })
        log.info("{} started", getClass().getSimpleName())
    }
}
