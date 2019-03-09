package com.sourceplusplus.api.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.json.Json;

import java.util.UUID;

/**
 * A common client interface for the various Source++ components.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.2
 * @since 0.1.0
 */
public interface SourceClient {

    String CLIENT_ID = UUID.randomUUID().toString();

    static void initMappers() {
        Json.mapper.findAndRegisterModules();
        Json.mapper.registerModule(new GuavaModule());
        Json.mapper.registerModule(new Jdk8Module());
        Json.mapper.registerModule(new JavaTimeModule());
        Json.mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        Json.mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        Json.mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }
}
