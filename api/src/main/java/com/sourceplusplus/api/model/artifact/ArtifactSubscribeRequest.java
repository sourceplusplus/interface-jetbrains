package com.sourceplusplus.api.model.artifact;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sourceplusplus.api.client.SourceClient;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest;
import com.sourceplusplus.api.model.trace.ArtifactTraceSubscribeRequest;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Used to subscribe to artifact metrics/traces.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.0
 */
@JsonAutoDetect
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes(value = {
        @JsonSubTypes.Type(value = ArtifactMetricSubscribeRequest.class, name = "METRICS"),
        @JsonSubTypes.Type(value = ArtifactTraceSubscribeRequest.class, name = "TRACES")
})
public interface ArtifactSubscribeRequest extends SourceMessage {

    @Nullable
    String appUuid();

    @Nullable
    String artifactQualifiedName();

    @Value.Default
    default String subscriberUuid() {
        return SourceClient.CLIENT_ID;
    }

    SourceArtifactSubscriptionType getType();
}
