package com.sourceplusplus.api.model.info;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import io.vertx.core.json.JsonObject;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.0
 * @since 0.1.1
 */
@SourceStyle
@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize(as = IntegrationInfo.class)
@JsonDeserialize(as = IntegrationInfo.class)
public interface AbstractIntegrationInfo extends SourceMessage {

    String id();

    @Nullable
    String name();

    @Nullable
    IntegrationCategory category();

    @Nullable
    Boolean enabled();

    @Nullable
    String version();

    IntegrationConnection connection();

    @Nullable
    JsonObject config();
}
