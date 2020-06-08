package com.sourceplusplus.core.integration.apm

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.sourceplusplus.core.storage.CoreConfig
import groovy.transform.EqualsAndHashCode
import io.vertx.core.json.Json

import java.time.Instant

/**
 * Persistent configuration for the APM integration.
 *
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class APMIntegrationConfig {

    private Instant latestSearchedService
    private Set<SourceService> sourceServices = new HashSet<>()
    private FailedArtifactTracker failedArtifactTracker = new FailedArtifactTracker()

    @EqualsAndHashCode(includeFields = true, cache = true)
    static class SourceService {
        private String id
        private String appUuid

        @JsonCreator
        SourceService(@JsonProperty("id") String id, @JsonProperty("app_uuid") String appUuid) {
            this.id = id
            this.appUuid = appUuid
        }

        String getId() {
            return id
        }

        String getAppUuid() {
            return appUuid
        }
    }

    static class FailedArtifactTracker {
        @JsonSerialize(using = ServiceLatestSearchedFailingTracesSerializer.class)
        @JsonDeserialize(using = ServiceLatestSearchedFailingTracesDeserializer.class)
        private Map<SourceService, Instant> serviceLatestSearchedFailingTraces = new HashMap<>()

        Map<SourceService, Instant> getServiceLatestSearchedFailingTraces() {
            return serviceLatestSearchedFailingTraces
        }

        void addServiceLatestSearchedFailingTraces(SourceService sourceService, Instant latestSearchedFailingTraces) {
            serviceLatestSearchedFailingTraces.put(sourceService, latestSearchedFailingTraces)
            CoreConfig.INSTANCE?.save()
        }
    }

    Instant getLatestSearchedService() {
        return latestSearchedService
    }

    void setLatestSearchedService(Instant latestSearchedService) {
        this.latestSearchedService = latestSearchedService
        CoreConfig.INSTANCE?.save()
    }

    void addSourceService(SourceService service) {
        if (this.sourceServices.add(service)) {
            CoreConfig.INSTANCE?.save()
        }
    }

    Set<SourceService> getSourceServices() {
        return sourceServices
    }

    FailedArtifactTracker getFailedArtifactTracker() {
        return failedArtifactTracker
    }

    static class ServiceLatestSearchedFailingTracesSerializer extends StdSerializer<Map<SourceService, Instant>> {

        ServiceLatestSearchedFailingTracesSerializer() {
            this(null)
        }

        ServiceLatestSearchedFailingTracesSerializer(Class<Map<SourceService, Instant>> vc) {
            super(vc)
        }

        @Override
        void serialize(Map<SourceService, Instant> value, JsonGenerator gen, SerializerProvider provider)
                throws IOException {
            gen.writeStartArray()
            value.entrySet().each {
                gen.writeStartObject()
                gen.writeNumberField(Json.encode(it.key), it.value.toEpochMilli())
                gen.writeEndObject()
            }
            gen.writeEndArray()
        }
    }

    static class ServiceLatestSearchedFailingTracesDeserializer extends StdDeserializer<Map<SourceService, Instant>> {

        ServiceLatestSearchedFailingTracesDeserializer() {
            this(null)
        }

        ServiceLatestSearchedFailingTracesDeserializer(Class<?> vc) {
            super(vc)
        }

        @Override
        Map<SourceService, Instant> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            Map<SourceService, Instant> serviceInstanceMap = new HashMap<>()
            JsonNode node = jp.getCodec().readTree(jp)
            for (int i = 0; i < node.size(); i++) {
                def data = node.get(i)
                serviceInstanceMap.put(Json.decodeValue(data.fieldNames()[0], SourceService.class),
                        Instant.ofEpochMilli(data[0].asLong()))
            }
            return serviceInstanceMap
        }
    }
}