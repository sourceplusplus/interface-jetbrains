package com.sourceplusplus.api.model.integration.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Configuration for the Apache SkyWalking APM integration.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.2.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = ApacheSkyWalkingIntegrationConfig.class)
@JsonDeserialize(as = ApacheSkyWalkingIntegrationConfig.class)
public interface AbstractApacheSkyWalkingIntegrationConfig extends IntegrationConfig {

    @Nullable
    String timezone();

    @Nullable
    Integer endpointDetectionIntervalSeconds();

    @Nullable
    Integer failingArtifactDetectionIntervalSeconds();
}
