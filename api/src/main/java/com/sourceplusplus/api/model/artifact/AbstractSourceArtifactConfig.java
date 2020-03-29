package com.sourceplusplus.api.model.artifact;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.4
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = SourceArtifactConfig.class)
@JsonDeserialize(as = SourceArtifactConfig.class)
public interface AbstractSourceArtifactConfig extends SourceMessage {

    @Nullable
    Boolean endpoint();

    @Nullable
    Boolean subscribeAutomatically();

    @Nullable
    Boolean forceSubscribe();

    @Nullable
    String moduleName();

    @Nullable
    String component();

    @Nullable
    String endpointName();

    @Nullable
    Set<String> endpointIds();
}
