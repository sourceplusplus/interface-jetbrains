package com.sourceplusplus.api.model.artifact;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Map;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.0
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = SourceArtifactSubscriber.class)
@JsonDeserialize(as = SourceArtifactSubscriber.class)
public interface AbstractSourceArtifactSubscriber extends SourceMessage {

    String subscriberUuid();

    Map<SourceArtifactSubscriptionType, Instant> subscriptionLastAccessed();
}
