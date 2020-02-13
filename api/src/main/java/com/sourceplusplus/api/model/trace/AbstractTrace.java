package com.sourceplusplus.api.model.trace;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.3
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonSerialize(as = Trace.class)
@JsonDeserialize(as = Trace.class)
public interface AbstractTrace {

    String key();

    @JsonAlias({"operationNames"})
    List<String> operationNames();

    int duration();

    long start();

    @JsonAlias({"isError", "error"})
    boolean isError();

    @JsonAlias({"traceIds"})
    List<String> traceIds();

    @Nullable
    String prettyDuration();
}
