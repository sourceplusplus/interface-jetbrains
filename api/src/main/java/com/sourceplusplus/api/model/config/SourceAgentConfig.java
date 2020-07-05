package com.sourceplusplus.api.model.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Holds the current configuration used by the agent.
 *
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize(using = SourceAgentConfig.SourceAgentConfigSerializer.class)
@JsonDeserialize(using = SourceAgentConfig.SourceAgentConfigDeserializer.class)
public final class SourceAgentConfig {

    public static final SourceAgentConfig current = new SourceAgentConfig();
    public volatile String apiHost;
    public volatile Integer apiPort;
    public volatile String apiKey;
    public volatile String apiVersion;
    public volatile Boolean apiSslEnabled;
    public volatile Boolean agentEnabled;
    public volatile Boolean testMode;
    public volatile Boolean manualSetupMode;
    public volatile Boolean manualTraceSyncMode;
    public volatile Boolean skywalkingEnabled;
    public volatile String appUuid;
    public volatile String appName;
    public volatile String logLevel;
    public volatile String logLocation;
    public volatile Boolean logMetrics;
    public volatile Boolean outputEnhancedClasses;
    public volatile Integer sampleNPer3Secs;
    public volatile Integer spanLimitPerSegment;
    public volatile List<String> packages;
    public volatile String pluginHost;
    public volatile Integer pluginPort;
    public volatile Boolean pluginBridgeEnabled;

    @SuppressWarnings("unchecked")
    public void applyConfig(JsonObject config) {
        JsonObject agentConfig = Objects.requireNonNull(config);
        if (agentConfig.containsKey("enabled")) agentEnabled = agentConfig.getBoolean("enabled");
        if (agentConfig.containsKey("test_mode")) testMode = agentConfig.getBoolean("test_mode");
        if (agentConfig.containsKey("manual_setup_mode")) manualSetupMode = agentConfig.getBoolean("manual_setup_mode");
        if (agentConfig.containsKey("manual_trace_sync_mode")) manualTraceSyncMode = agentConfig.getBoolean("manual_trace_sync_mode");
        if (agentConfig.containsKey("log_level")) logLevel = agentConfig.getString("log_level").toUpperCase();
        if (agentConfig.containsKey("log_location")) logLocation = agentConfig.getString("log_location");
        if (agentConfig.containsKey("log_metrics")) logMetrics = agentConfig.getBoolean("log_metrics");

        JsonObject applicationConfig = agentConfig.getJsonObject("application");
        if (applicationConfig != null) {
            if (applicationConfig.containsKey("app_uuid")) appUuid = applicationConfig.getString("app_uuid");
            if (applicationConfig.containsKey("app_name")) appName = applicationConfig.getString("app_name");

            JsonObject sourceCodeConfig = applicationConfig.getJsonObject("application_source_code");
            if (sourceCodeConfig != null) {
                packages = (List<String>) sourceCodeConfig.getJsonArray("packages").getList();
            }
        }

        JsonObject apiConfig = agentConfig.getJsonObject("api");
        if (apiConfig != null) {
            if (apiConfig.containsKey("version")) apiVersion = apiConfig.getString("version");
            if (apiConfig.containsKey("host")) {
                apiHost = System.getenv().getOrDefault("SPP_API_HOST", System.getProperty("SPP_API_HOST",
                        apiConfig.getString("host")));
            }
            if (apiConfig.containsKey("port")) apiPort = apiConfig.getInteger("port");
            if (apiConfig.containsKey("ssl")) apiSslEnabled = apiConfig.getBoolean("ssl");
            if (apiConfig.containsKey("key")) apiKey = apiConfig.getString("key");
        }

        JsonObject skywalkingConfig = agentConfig.getJsonObject("skywalking");
        if (skywalkingConfig != null) {
            if (skywalkingConfig.containsKey("enabled"))
                skywalkingEnabled = skywalkingConfig.getBoolean("enabled");
            if (skywalkingConfig.containsKey("output_enhanced_classes"))
                outputEnhancedClasses = skywalkingConfig.getBoolean("output_enhanced_classes");
            if (skywalkingConfig.containsKey("sample_n_per_3_secs"))
                sampleNPer3Secs = skywalkingConfig.getInteger("sample_n_per_3_secs");
            if (skywalkingConfig.containsKey("span_limit_per_segment"))
                spanLimitPerSegment = skywalkingConfig.getInteger("span_limit_per_segment");
            //todo: plugins
        }

        JsonObject pluginBridgeConfig = agentConfig.getJsonObject("plugin-bridge");
        if (pluginBridgeConfig != null) {
            if (pluginBridgeConfig.containsKey("enabled"))
                pluginBridgeEnabled = pluginBridgeConfig.getBoolean("enabled");
            if (pluginBridgeConfig.containsKey("host")) pluginHost = pluginBridgeConfig.getString("host");
            if (pluginBridgeConfig.containsKey("port")) pluginPort = pluginBridgeConfig.getInteger("port");
        }
    }

    public JsonObject toJsonObject() {
        return new JsonObject(toString());
    }

    @Override
    public String toString() {
        return Json.encode(this);
    }

    public static class SourceAgentConfigSerializer extends StdSerializer<SourceAgentConfig> {

        protected SourceAgentConfigSerializer() {
            super(SourceAgentConfig.class);
        }

        @Override
        public void serialize(SourceAgentConfig value, JsonGenerator gen, SerializerProvider provider)
                throws IOException {
            gen.writeStartObject();

            if (value.agentEnabled != null) gen.writeBooleanField("enabled", value.agentEnabled);
            if (value.testMode != null) gen.writeBooleanField("test_mode", value.testMode);
            if (value.manualSetupMode != null) gen.writeBooleanField("manual_setup_mode", value.manualSetupMode);
            if (value.manualTraceSyncMode != null) gen.writeBooleanField("manual_trace_sync_mode", value.manualTraceSyncMode);
            if (value.logLevel != null) gen.writeStringField("log_level", value.logLevel);
            if (value.logLocation != null) gen.writeStringField("log_location", value.logLocation);
            if (value.logMetrics != null) gen.writeBooleanField("log_metrics", value.logMetrics);

            gen.writeFieldName("application");
            gen.writeStartObject();
            if (value.appUuid != null) gen.writeStringField("app_uuid", value.appUuid);
            if (value.appName != null) gen.writeStringField("app_name", value.appName);

            gen.writeFieldName("application_source_code");
            gen.writeStartObject();
            gen.writeArrayFieldStart("packages");
            if (value.packages != null) {
                for (String packagePattern : value.packages) {
                    gen.writeString(packagePattern);
                }
            }
            gen.writeEndArray();
            gen.writeEndObject(); //application_source_code
            gen.writeEndObject(); //application

            gen.writeFieldName("api");
            gen.writeStartObject();
            if (value.apiVersion != null) gen.writeStringField("version", value.apiVersion);
            if (value.apiHost != null) gen.writeStringField("host", value.apiHost);
            if (value.apiPort != null) gen.writeNumberField("port", value.apiPort);
            if (value.apiSslEnabled != null) gen.writeBooleanField("ssl", value.apiSslEnabled);
            if (value.apiKey != null) gen.writeStringField("key", value.apiKey);
            gen.writeEndObject(); //api

            gen.writeFieldName("skywalking");
            gen.writeStartObject();
            if (value.skywalkingEnabled != null) gen.writeBooleanField("enabled", value.skywalkingEnabled);
            if (value.sampleNPer3Secs != null) gen.writeNumberField("sample_n_per_3_secs", value.sampleNPer3Secs);
            if (value.spanLimitPerSegment != null)
                gen.writeNumberField("span_limit_per_segment", value.spanLimitPerSegment);
            if (value.outputEnhancedClasses != null)
                gen.writeBooleanField("output_enhanced_classes", value.outputEnhancedClasses);
            //todo: plugins
            gen.writeEndObject(); //skywalking

            gen.writeFieldName("plugin-bridge");
            gen.writeStartObject();
            if (value.pluginBridgeEnabled != null) gen.writeBooleanField("enabled", value.pluginBridgeEnabled);
            if (value.pluginHost != null) gen.writeStringField("host", value.pluginHost);
            if (value.pluginPort != null) gen.writeNumberField("port", value.pluginPort);
            gen.writeEndObject(); //plugin-bridge

            gen.writeEndObject(); //root
        }
    }

    public static class SourceAgentConfigDeserializer extends StdDeserializer<SourceAgentConfig> {

        protected SourceAgentConfigDeserializer() {
            super(SourceAgentConfig.class);
        }

        @Override
        public SourceAgentConfig deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
            SourceAgentConfig agentConfig = new SourceAgentConfig();
            agentConfig.applyConfig(new JsonObject(node.toString()));
            return agentConfig;
        }
    }
}
