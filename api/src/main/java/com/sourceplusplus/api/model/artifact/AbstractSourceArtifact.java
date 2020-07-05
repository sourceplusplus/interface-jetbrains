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
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
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
    @Value.Auxiliary
    Instant createDate();

    @Nullable
    @Value.Auxiliary
    Instant lastUpdated();

    @Value.Default
    @Value.Auxiliary
    default SourceArtifactConfig config() {
        return SourceArtifactConfig.builder().build();
    }

    @Value.Default
    @Value.Auxiliary
    default SourceArtifactStatus status() {
        return SourceArtifactStatus.builder().build();
    }
}
