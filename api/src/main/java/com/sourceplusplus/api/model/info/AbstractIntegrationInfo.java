package com.sourceplusplus.api.model.info;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.1
 * @since 0.1.1
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = IntegrationInfo.class)
@JsonDeserialize(as = IntegrationInfo.class)
public interface AbstractIntegrationInfo extends SourceMessage {

    String name();

    IntegrationType type();

    String version();
}
