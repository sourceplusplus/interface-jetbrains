package com.sourceplusplus.api.model.info;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.base.Preconditions;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import com.sourceplusplus.api.model.config.SourceCoreConfig;
import com.sourceplusplus.api.model.integration.IntegrationInfo;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to convey information about the current setup of core.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.3.0
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

    @JsonSerialize(using = ActiveIntegrationsSerializer.class)
    @JsonDeserialize(using = ActiveIntegrationsDeserializer.class)
    List<IntegrationInfo> activeIntegrations();

    @Value.Check
    default void validate() {
        if (!"dev".equals(version()) && !"embedded".equals(version())) {
            Preconditions.checkNotNull(buildDate());
        }
    }

    class ActiveIntegrationsSerializer extends StdSerializer<List<IntegrationInfo>> {

        public ActiveIntegrationsSerializer() {
            this(null);
        }

        public ActiveIntegrationsSerializer(Class<List<IntegrationInfo>> vc) {
            super(vc);
        }

        @Override
        public void serialize(List<IntegrationInfo> value, JsonGenerator gen, SerializerProvider provider)
                throws IOException {
            gen.writeStartArray();
            for (IntegrationInfo info : value) {
                gen.writeRawValue(new JsonObject(Json.encode(info)).put("id", info.id()).toString());
            }
            gen.writeEndArray();
        }
    }

    class ActiveIntegrationsDeserializer extends StdDeserializer<List<IntegrationInfo>> {

        public ActiveIntegrationsDeserializer() {
            this(null);
        }

        public ActiveIntegrationsDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public List<IntegrationInfo> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            List<IntegrationInfo> infos = new ArrayList<>();
            JsonNode node = jp.getCodec().readTree(jp);
            for (int i = 0; i < node.size(); i++) {
                infos.add(Json.decodeValue(new JsonObject(node.get(i).toString()).putNull("config").toString(),
                        IntegrationInfo.class));
            }
            return infos;
        }
    }
}
