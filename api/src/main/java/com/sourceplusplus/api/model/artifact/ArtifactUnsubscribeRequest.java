package com.sourceplusplus.api.model.artifact;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.sourceplusplus.api.client.SourceClient;
import com.sourceplusplus.api.model.SourceMessage;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Used to unsubscribe to artifact metrics/traces.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.0
 */
@JsonAutoDetect
public interface ArtifactUnsubscribeRequest extends SourceMessage {

    @Nullable
    String appUuid();

    @Nullable
    String artifactQualifiedName();

    @Value.Default
    default String getSubscriberClientId() {
        return SourceClient.CLIENT_ID;
    }

    @Value.Default
    default String subscriberUuid() {
        return SourceClient.CLIENT_ID;
    }

    SourceArtifactSubscriptionType getType();
}
