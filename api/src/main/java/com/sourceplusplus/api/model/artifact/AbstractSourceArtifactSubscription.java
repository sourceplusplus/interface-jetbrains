package com.sourceplusplus.api.model.artifact;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Map;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.4
 * @since 0.1.2
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = SourceArtifactSubscription.class)
@JsonDeserialize(as = SourceArtifactSubscription.class)
public interface AbstractSourceArtifactSubscription extends SourceMessage {

    @Nullable
    String appUuid();

    String artifactQualifiedName();

    String subscriberUuid();

    Map<SourceArtifactSubscriptionType, Instant> subscriptionLastAccessed();
}
