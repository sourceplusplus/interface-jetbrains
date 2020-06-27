package com.sourceplusplus.api.model.application;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import com.sourceplusplus.api.model.config.SourceAgentConfig;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * Used to create/update source applications.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.3.0
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonSerialize(as = SourceApplication.class)
@JsonDeserialize(as = SourceApplication.class)
public interface AbstractSourceApplication extends SourceMessage {

    @Nullable
    String appUuid();

    @Nullable
    String appName();

    @Nullable
    Instant createDate();

    @Nullable
    SourceAgentConfig agentConfig();

    @Nullable
    @JsonAlias({"isCreateRequest", "create_request"})
    Boolean isCreateRequest();

    @Nullable
    @JsonAlias({"isUpdateRequest", "update_request"})
    Boolean isUpdateRequest();

    @Value.Check
    default void validate() {
        boolean isCreateRequest = isCreateRequest() == null ? false : isCreateRequest();
        boolean isUpdateRequest = isUpdateRequest() == null ? false : isUpdateRequest();
        Preconditions.checkState(!(isCreateRequest && isUpdateRequest),
                "Cannot be both create and update request at the same time");
        if (!isCreateRequest && !isUpdateRequest) {
            Preconditions.checkNotNull(appUuid());
            Preconditions.checkNotNull(createDate());
        }
    }
}
