package com.sourceplusplus.api.model.internal;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.QueryTimeFrame;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.0
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = FormattedQuickStats.class)
@JsonDeserialize(as = FormattedQuickStats.class)
public interface AbstractFormattedQuickStats extends SourceMessage {

    QueryTimeFrame timeFrame();

    @Nullable
    String max();

    @Nullable
    String min();

    @Nullable
    String p50();

    @Nullable
    String p75();

    @Nullable
    String p90();

    @Nullable
    String p95();

    @Nullable
    String p99();
}
