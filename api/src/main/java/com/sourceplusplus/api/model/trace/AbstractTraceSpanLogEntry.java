package com.sourceplusplus.api.model.trace;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Log data in a trace span result.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = TraceSpanLogEntry.class)
@JsonDeserialize(as = TraceSpanLogEntry.class)
public interface AbstractTraceSpanLogEntry {

    @JsonDeserialize(using = InstantFromMillis.class)
    Instant time();

    @JsonDeserialize(using = AbstractTraceSpan.KeyValuePair.class)
    Map<String, String> data();

    class InstantFromMillis extends JsonDeserializer<Instant> {

        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            NumericNode epoch = p.getCodec().readTree(p);
            if (epoch instanceof LongNode) {
                return Instant.ofEpochMilli(epoch.longValue());
            } else {
                return Instant.ofEpochSecond(epoch.longValue());
            }
        }
    }
}
