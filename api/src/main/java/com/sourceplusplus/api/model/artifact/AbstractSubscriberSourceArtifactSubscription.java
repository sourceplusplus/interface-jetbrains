package com.sourceplusplus.api.model.artifact;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Map;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.2
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = SubscriberSourceArtifactSubscription.class)
@JsonDeserialize(as = SubscriberSourceArtifactSubscription.class)
public interface AbstractSubscriberSourceArtifactSubscription {

    String artifactQualifiedName();

    Map<SourceArtifactSubscriptionType, Instant> subscriptionAccess();
}
