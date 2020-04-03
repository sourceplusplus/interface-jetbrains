package com.sourceplusplus.api.model.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.5
 * @since 0.2.0
 */
@SourceStyle
@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize(as = IntegrationConnection.class)
@JsonDeserialize(as = IntegrationConnection.class)
public interface AbstractIntegrationConnection {

    String getHost();

    int getPort();

    @Nullable
    Integer getProxyPort();
}
