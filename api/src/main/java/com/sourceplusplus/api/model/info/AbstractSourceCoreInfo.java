package com.sourceplusplus.api.model.info;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import com.sourceplusplus.api.model.config.SourceCoreConfig;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.4
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = SourceCoreInfo.class)
@JsonDeserialize(as = SourceCoreInfo.class)
public interface AbstractSourceCoreInfo extends SourceMessage {

    String version();

    @Nullable
    Instant buildDate();

    SourceCoreConfig config();

    List<IntegrationInfo> integrations();

    @Value.Check
    default void validate() {
        if (!"dev".equals(version())) {
            Preconditions.checkNotNull(buildDate());
        }
    }
}
