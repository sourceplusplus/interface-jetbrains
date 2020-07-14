package com.sourceplusplus.api.model.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;

/**
 * Traces result for a given artifact.
 *
 * @version 0.3.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@SourceStyle
@Value.Immutable
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonSerialize(as = ArtifactTraceResult.class)
@JsonDeserialize(as = ArtifactTraceResult.class)
public interface AbstractArtifactTraceResult extends SourceMessage {

    @Nullable
    String appUuid();

    @Nullable
    String artifactQualifiedName();

    @Nullable
    String artifactSimpleName();

    TraceOrderType orderType();

    Instant start();

    Instant stop();

    String step();

    List<Trace> traces();

    int total();

    default ArtifactTraceResult mergeWith(ArtifactTraceResult result) {
        if (!Objects.equals(appUuid(), result.appUuid())) {
            throw new IllegalArgumentException("Mismatching application uuid");
        } else if (!Objects.equals(artifactQualifiedName(), result.artifactQualifiedName())) {
            throw new IllegalArgumentException("Mismatching artifact qualified name");
        } else if (!Objects.equals(orderType(), result.orderType())) {
            throw new IllegalArgumentException("Mismatching order type");
        } else if (!Objects.equals(step(), result.step())) {
            throw new IllegalArgumentException("Mismatching step");
        }

        if (start().isBefore(result.start())) {
            result = result.withStart(start());
        }
        if (stop().isAfter(result.stop())) {
            result = result.withStop(stop());
        }
        if (result.artifactSimpleName() == null && artifactSimpleName() != null) {
            result = result.withArtifactSimpleName(artifactSimpleName());
        }

        Set<Trace> combinedTraces = new HashSet<>(traces());
        combinedTraces.addAll(result.traces());

        List<Trace> finalTraces = new ArrayList<>(combinedTraces);
        finalTraces.sort((t2, t1) -> {
            if (orderType() == TraceOrderType.SLOWEST_TRACES) {
                return Integer.compare(t1.duration(), t2.duration());
            } else {
                return Long.compare(t1.start(), t2.start());
            }
        });
        result = result.withTraces(finalTraces);
        result = result.withTotal(finalTraces.size());
        return result;
    }

    default ArtifactTraceResult truncate(int amount) {
        if (traces().size() > amount) {
            return ((ArtifactTraceResult) this).withTraces(traces().subList(0, amount))
                    .withTotal(traces().size());
        }
        return (ArtifactTraceResult) this;
    }
}
