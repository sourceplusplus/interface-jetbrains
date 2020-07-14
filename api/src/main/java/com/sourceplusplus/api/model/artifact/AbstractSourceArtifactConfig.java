package com.sourceplusplus.api.model.artifact;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Used to provide additional configuration to source artifacts.
 *
 * @version 0.3.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = SourceArtifactConfig.class)
@JsonDeserialize(as = SourceArtifactConfig.class)
public interface AbstractSourceArtifactConfig extends SourceMessage {

    @Nullable
    Boolean endpoint();

    @Nullable
    Boolean automaticEndpoint();

    @Nullable
    Boolean subscribeAutomatically();

    @Nullable
    String moduleName();

    @Nullable
    String component();

    @Nullable
    String endpointName();

    @Nullable
    Set<String> endpointIds();
}
