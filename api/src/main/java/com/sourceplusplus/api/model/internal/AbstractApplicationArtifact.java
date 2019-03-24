package com.sourceplusplus.api.model.internal;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.0
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = ApplicationArtifact.class)
@JsonDeserialize(as = ApplicationArtifact.class)
public interface AbstractApplicationArtifact extends SourceMessage {

    String appUuid();

    String artifactQualifiedName();
}
