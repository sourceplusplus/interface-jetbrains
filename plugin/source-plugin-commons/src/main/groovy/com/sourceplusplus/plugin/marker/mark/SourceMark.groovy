package com.sourceplusplus.plugin.marker.mark

import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.plugin.PluginSourceFile
import com.sourceplusplus.plugin.marker.SourceFileMarker
import com.sourceplusplus.plugin.source.model.SourceMethodAnnotation
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import java.time.Instant

/**
 * todo: description
 *
 * @version 0.1.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
abstract class SourceMark {

    private final SourceFileMarker sourceFileMarker
    private boolean artifactSubscribed
    private boolean artifactDataAvailable
    private Instant subscribeTime
    private Instant unsubscribeTime

    SourceMark(@NotNull SourceFileMarker sourceFileMarker) {
        this.sourceFileMarker = sourceFileMarker
    }

    SourceFileMarker getSourceFileMarker() {
        return sourceFileMarker
    }

    boolean isArtifactDataAvailable() {
        return artifactDataAvailable
    }

    void setArtifactDataAvailable(boolean artifactDataAvailable) {
        this.artifactDataAvailable = artifactDataAvailable
    }

    void setArtifactSubscribed(boolean artifactSubscribed) {
        this.artifactSubscribed = artifactSubscribed
    }

    boolean isArtifactSubscribed() {
        return artifactSubscribed
    }

    Instant getSubscribeTime() {
        return subscribeTime
    }

    void setSubscribeTime(Instant subscribeTime) {
        this.subscribeTime = subscribeTime
    }

    Instant getUnsubscribeTime() {
        return unsubscribeTime
    }

    void setUnsubscribeTime(Instant unsubscribeTime) {
        this.unsubscribeTime = unsubscribeTime
    }

    @NotNull
    abstract PluginSourceFile getSourceFile()

    @Nullable
    abstract SourceArtifact getSourceMethod()

    abstract boolean isClassMark()

    abstract boolean isMethodMark()

    abstract void getMethodAnnotations(Handler<AsyncResult<List<SourceMethodAnnotation>>> handler)

    abstract String getModuleName()

    @NotNull
    String getArtifactQualifiedName() {
        if (isClassMark()) {
            return sourceFile.qualifiedClassName
        } else {
            return sourceMethod.artifactQualifiedName()
        }
    }

    @Override
    boolean equals(Object o) {
        if (o instanceof SourceMark) {
            if (isMethodMark() && o.isMethodMark()) {
                return sourceMethod == o.sourceMethod
            } else if (isClassMark() && isClassMark()) {
                return sourceFile == o.sourceFile
            }
        }
        return false
    }

    @Override
    int hashCode() {
        if (isMethodMark()) {
            return sourceMethod.hashCode()
        } else {
            return sourceFile.hashCode()
        }
    }
}
