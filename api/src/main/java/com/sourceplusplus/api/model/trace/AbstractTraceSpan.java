package com.sourceplusplus.api.model.trace;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sourceplusplus.api.model.SourceStyle;
import io.vertx.core.json.Json;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.5
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = TraceSpan.class)
@JsonDeserialize(as = TraceSpan.class)
public interface AbstractTraceSpan {

    @JsonAlias({"traceId", "trace_id"})
    String traceId();

    @JsonAlias({"segmentId", "segment_id"})
    String segmentId();

    @JsonAlias({"spanId", "span_id"})
    long spanId();

    @JsonAlias({"parentSpanId", "parent_span_id"})
    long parentSpanId();

    List<TraceSpanRef> refs();

    @JsonAlias({"serviceCode", "service_code"})
    String serviceCode();

    @JsonAlias({"startTime", "start_time"})
    long startTime();

    @JsonAlias({"endTime", "end_time"})
    long endTime();

    @JsonAlias({"endpointName", "endpoint_name"})
    String endpointName();

    @Nullable
    String artifactQualifiedName();

    String type();

    String peer();

    String component();

    @JsonAlias({"isError", "error"})
    boolean isError();

    String layer();

    @JsonDeserialize(using = KeyValuePair.class)
    Map<String, String> tags();

    List<TraceSpanLogEntry> logs();

    class KeyValuePair extends JsonDeserializer<Map<String, String>> {

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            Map<String, String> rtnMap = new HashMap<>();
            TreeNode treeNode = p.getCodec().readTree(p);
            if (treeNode.isObject()) {
                rtnMap = Json.decodeValue(treeNode.toString(), Map.class);
            } else {
                ArrayNode array = (ArrayNode) treeNode;
                for (int i = 0; i < array.size(); i++) {
                    JsonNode node = array.get(i);
                    rtnMap.put(node.get("key").asText(), node.get("value").asText());
                }
            }
            return rtnMap;
        }
    }
}
