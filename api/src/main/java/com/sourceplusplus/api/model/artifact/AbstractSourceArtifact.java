package com.sourceplusplus.api.model.artifact;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * Represents a subscribable source artifact (method/class/etc).
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonSerialize(as = SourceArtifact.class)
@JsonDeserialize(as = SourceArtifact.class)
public interface AbstractSourceArtifact extends SourceMessage {

    @Nullable
    String appUuid();

    @Nullable
    String artifactQualifiedName();

    @Nullable
    Instant createDate();

    @Nullable
    Instant lastUpdated();

    @Nullable
    SourceArtifactConfig config();

    @Nullable
    SourceArtifactStatus status();
}
