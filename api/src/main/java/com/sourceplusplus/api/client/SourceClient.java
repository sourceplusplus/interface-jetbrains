package com.sourceplusplus.api.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.json.jackson.DatabindCodec;

import java.util.UUID;

/**
 * A common client interface for the various Source++ components.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.3
 * @since 0.1.0
 */
public interface SourceClient {

    String CLIENT_ID = UUID.randomUUID().toString();

    static void initMappers() {
        DatabindCodec.mapper().registerModule(new GuavaModule());
        DatabindCodec.mapper().registerModule(new Jdk8Module());
        DatabindCodec.mapper().registerModule(new JavaTimeModule());
        DatabindCodec.mapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        DatabindCodec.mapper().enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        DatabindCodec.mapper().enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }
}
