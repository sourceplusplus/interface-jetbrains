package com.sourceplusplus.api.model.artifact;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.0
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = SourceArtifactSubscription.class)
@JsonDeserialize(as = SourceArtifactSubscription.class)
public interface AbstractSourceArtifactSubscription extends SourceMessage {

    @Nullable
    SourceArtifactSubscriptionType type();

    int subscribers();
}
